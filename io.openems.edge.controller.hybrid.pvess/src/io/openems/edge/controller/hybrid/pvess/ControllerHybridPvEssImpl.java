package io.openems.edge.controller.hybrid.pvess;

import static io.openems.common.utils.IntUtils.fitWithin;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Hybrid.PvEss", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerHybridPvEssImpl extends AbstractOpenemsComponent
		implements ControllerHybridPvEss, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerHybridPvEssImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private List<TouSlot> touSlots = List.of();

	public ControllerHybridPvEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerHybridPvEss.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.touSlots = parseTouSlots(config.touScheduleJson());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var ess = this.componentManager.<ManagedSymmetricEss>getComponent(this.config.ess_id());
		var meter = this.componentManager.<ElectricityMeter>getComponent(this.config.meter_id());

		if (!ess.isOnGridOrUndefined(m -> this.logWarn(this.log, m))) {
			return;
		}

		var meterPower = meter.getActivePower().getOrError();
		var essPower = ess.getActivePower().getOrError();
		var desiredEssPower = switch (this.config.mode()) {
		case ZERO_EXPORT -> calculateZeroExportEssPower(meterPower, essPower, this.config.targetGridSetpoint());
		case PEAK_SHAVING -> calculatePeakShavingEssPower(meterPower, essPower, this.config.peakShavingPower(),
				this.config.rechargePower());
		case TOU -> this.calculateTouEssPower(meterPower, essPower, this.pvPowerForTou(), this.getActiveTouSlot());
		};

		var limitedEssPower = this.applyEssLimits(ess, desiredEssPower);
		ess.setActivePowerEqualsWithFilter(limitedEssPower);
		ess.setReactivePowerEqualsWithoutFilter(0);
		this._setCalculatedEssPower(limitedEssPower);

		if (this.config.enablePvCurtailment()) {
			var pvInverter = this.componentManager.<ManagedSymmetricPvInverter>getComponent(this.config.pvInverter_id());
			var pvPower = pvInverter.getActivePower().getOrError();
			var pvReleaseLimit = this.getPvReleaseLimit(pvInverter, pvPower);
			var pvLimit = this.calculatePvLimit(meterPower, essPower, pvPower, pvReleaseLimit, desiredEssPower,
					limitedEssPower);
			pvInverter.setActivePowerLimit(pvLimit);
			this._setCalculatedPvLimit(pvLimit);
		}
	}

	protected static int calculateZeroExportEssPower(int meterPower, int essPower, int targetGridSetpoint) {
		return meterPower + essPower - targetGridSetpoint;
	}

	protected static int calculatePeakShavingEssPower(int meterPower, int essPower, int peakShavingPower,
			int rechargePower) {
		var uncompensatedGridPower = meterPower + essPower;
		if (uncompensatedGridPower >= peakShavingPower) {
			return uncompensatedGridPower - peakShavingPower;
		}
		if (uncompensatedGridPower <= rechargePower) {
			return uncompensatedGridPower - rechargePower;
		}
		return 0;
	}

	private int calculateTouEssPower(int meterPower, int essPower, int pvPower, TouSlot slot) {
		if (slot == null) {
			return calculateZeroExportEssPower(meterPower, essPower, this.config.targetGridSetpoint());
		}

		var uncompensatedGridPower = meterPower + essPower;
		return switch (slot.type()) {
		case CHARGE -> switch (slot.chargeSource()) {
		case ANY -> slot.power() * -1;
		case PV_SURPLUS -> Math.min(Math.max(0, uncompensatedGridPower * -1), slot.power()) * -1;
		case PV_ALL -> Math.min(Math.max(0, pvPower), slot.power()) * -1;
		};
		case DISCHARGE -> Math.min(Math.max(0, uncompensatedGridPower), slot.power());
		};
	}

	private int applyEssLimits(ManagedSymmetricEss ess, int desiredEssPower) throws OpenemsNamedException {
		return this.applyEssLimits(ess, desiredEssPower, this.getActiveTouSlot());
	}

	private int applyEssLimits(ManagedSymmetricEss ess, int desiredEssPower, TouSlot slot) throws OpenemsNamedException {
		var minPower = ess.getPower().getMinPower(ess, ALL, ACTIVE);
		var maxPower = ess.getPower().getMaxPower(ess, ALL, ACTIVE);

		if (this.config.maxChargePower() > 0) {
			minPower = Math.max(minPower, this.config.maxChargePower() * -1);
		}
		if (this.config.maxDischargePower() > 0) {
			maxPower = Math.min(maxPower, this.config.maxDischargePower());
		}

		var soc = ess.getSoc().asOptional();
		if (soc.isPresent()) {
			var value = soc.get();
			var minSoc = slot != null && slot.minSoc() != null ? slot.minSoc() : this.config.minSoc();
			var maxSoc = slot != null && slot.maxSoc() != null ? slot.maxSoc() : this.config.maxSoc();
			if (minSoc > 0 && value <= minSoc) {
				maxPower = Math.min(maxPower, 0);
			}
			if (maxSoc < 100 && value >= maxSoc) {
				minPower = Math.max(minPower, 0);
			}
		}

		return fitWithin(minPower, maxPower, desiredEssPower);
	}

	private int calculatePvLimit(int meterPower, int essPower, int pvPower, int pvReleaseLimit, int desiredEssPower,
			int limitedEssPower) {
		var remainingExportAfterEssLimit = meterPower + essPower - limitedEssPower;
		var allowedExport = this.config.mode() == HybridMode.ZERO_EXPORT //
				? this.config.maximumSellToGridPower() //
				: 0;

		if (remainingExportAfterEssLimit * -1 <= allowedExport) {
			return pvReleaseLimit;
		}

		if (pvPower <= 0) {
			return 0;
		}

		var unhandledExport = (remainingExportAfterEssLimit * -1) - allowedExport;
		if (desiredEssPower < limitedEssPower) {
			unhandledExport = Math.max(unhandledExport, limitedEssPower - desiredEssPower);
		}
		return fitWithin(0, pvReleaseLimit, pvPower - unhandledExport);
	}

	private int getPvReleaseLimit(ManagedSymmetricPvInverter pvInverter, int pvPower) {
		var maxActivePower = pvInverter.getMaxActivePower().asOptional();
		if (maxActivePower.isPresent() && maxActivePower.get() > 0) {
			return maxActivePower.get();
		}
		if (this.config.fallbackPvReleasePower() > 0) {
			return this.config.fallbackPvReleasePower();
		}
		return Math.max(0, pvPower);
	}

	private int pvPowerForTou() throws OpenemsNamedException {
		var pvInverter = this.componentManager.<ManagedSymmetricPvInverter>getComponent(this.config.pvInverter_id());
		return pvInverter.getActivePower().getOrError();
	}

	private TouSlot getActiveTouSlot() {
		if (this.config.mode() != HybridMode.TOU) {
			return null;
		}
		var now = LocalTime.now(this.componentManager.getClock());
		for (TouSlot slot : this.touSlots) {
			if (slot.isActiveAt(now)) {
				return slot;
			}
		}
		return null;
	}

	private static List<TouSlot> parseTouSlots(String scheduleJson) throws OpenemsNamedException {
		if (scheduleJson == null || scheduleJson.trim().isEmpty()) {
			return List.of();
		}

		var result = new ArrayList<TouSlot>();
		var chargeCount = 0;
		var dischargeCount = 0;
		for (JsonElement element : JsonUtils.getAsJsonArray(JsonUtils.parse(scheduleJson))) {
			var enabled = JsonUtils.getAsOptionalBoolean(element, "enabled").orElse(true);
			var type = parseEnum(TouSlotType.class, element, "type");
			var slot = new TouSlot(//
					JsonUtils.getAsOptionalString(element, "name").orElse(type.name()), //
					enabled, //
					type, //
					parseTime(element, "start"), //
					parseTime(element, "end"), //
					Math.max(0, JsonUtils.getAsInt(element, "power")), //
					type == TouSlotType.CHARGE //
							? parseEnum(ChargeSource.class, element, "chargeSource") //
							: ChargeSource.PV_SURPLUS, //
					JsonUtils.getAsOptionalInt(element, "minSoc").orElse(null), //
					JsonUtils.getAsOptionalInt(element, "maxSoc").orElse(null));
			if (slot.start().equals(slot.end())) {
				throw new OpenemsException("TOU slot [" + slot.name() + "] start and end must be different");
			}
			if (slot.enabled()) {
				switch (slot.type()) {
				case CHARGE -> chargeCount++;
				case DISCHARGE -> dischargeCount++;
				}
			}
			result.add(slot);
		}

		if (result.size() > 20) {
			throw new OpenemsException("TOU schedule supports max 20 slots");
		}
		if (chargeCount > 10 || dischargeCount > 10) {
			throw new OpenemsException("TOU schedule supports max 10 charge slots and 10 discharge slots");
		}

		validateNoOverlap(result);
		return result.stream() //
				.sorted(Comparator.comparing(TouSlot::start)) //
				.toList();
	}

	private static LocalTime parseTime(JsonElement element, String memberName) throws OpenemsNamedException {
		return DateUtils.parseLocalTimeOrError(JsonUtils.getAsString(element, memberName));
	}

	private static <E extends Enum<E>> E parseEnum(Class<E> enumType, JsonElement element, String memberName)
			throws OpenemsNamedException {
		var value = JsonUtils.getAsString(element, memberName);
		try {
			return Enum.valueOf(enumType, value);
		} catch (IllegalArgumentException e) {
			throw new OpenemsException("Invalid TOU " + memberName + " [" + value + "]");
		}
	}

	private static void validateNoOverlap(List<TouSlot> slots) throws OpenemsNamedException {
		for (var i = 0; i < slots.size(); i++) {
			var left = slots.get(i);
			if (!left.enabled()) {
				continue;
			}
			for (var j = i + 1; j < slots.size(); j++) {
				var right = slots.get(j);
				if (right.enabled() && left.overlaps(right)) {
					throw new OpenemsException(
							"TOU slot [" + left.name() + "] overlaps with slot [" + right.name() + "]");
				}
			}
		}
	}

	private void _setCalculatedEssPower(Integer value) {
		this.<IntegerReadChannel>channel(ControllerHybridPvEss.ChannelId.CALCULATED_ESS_POWER).setNextValue(value);
	}

	private void _setCalculatedPvLimit(Integer value) {
		this.<IntegerReadChannel>channel(ControllerHybridPvEss.ChannelId.CALCULATED_PV_LIMIT).setNextValue(value);
	}

	private record TouSlot(String name, boolean enabled, TouSlotType type, LocalTime start, LocalTime end, int power,
			ChargeSource chargeSource, Integer minSoc, Integer maxSoc) {

		private boolean isActiveAt(LocalTime time) {
			if (!this.enabled) {
				return false;
			}
			if (this.start.isBefore(this.end)) {
				return !time.isBefore(this.start) && time.isBefore(this.end);
			}
			return !time.isBefore(this.start) || time.isBefore(this.end);
		}

		private boolean overlaps(TouSlot other) {
			for (var left : this.toMinuteRanges()) {
				for (var right : other.toMinuteRanges()) {
					if (left.start() < right.end() && right.start() < left.end()) {
						return true;
					}
				}
			}
			return false;
		}

		private List<MinuteRange> toMinuteRanges() {
			var startMinute = this.start.toSecondOfDay() / 60;
			var endMinute = this.end.toSecondOfDay() / 60;
			if (startMinute < endMinute) {
				return List.of(new MinuteRange(startMinute, endMinute));
			}
			return List.of(new MinuteRange(startMinute, 24 * 60), new MinuteRange(0, endMinute));
		}
	}

	private record MinuteRange(int start, int end) {
	}
}
