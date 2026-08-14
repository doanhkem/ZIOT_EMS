package io.openems.edge.ziot.generic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = ConfigPvInverter.class, factory = true)
@Component(//
		name = "Ziot.Generic.PvInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=PRODUCTION" } //
)
public class ZiotGenericPvInverterImpl extends AbstractOpenemsModbusComponent
		implements ZiotGenericPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave {

	private static final Logger LOG = LoggerFactory.getLogger(ZiotGenericPvInverterImpl.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Object ENERGY_GUARD_LOCK = new Object();
	private static final long ENERGY_GUARD_MAX_STEP_WH = 20_000L;
	private static final Path ENERGY_GUARD_FILE = Path.of(System.getProperty("ziot.energy.guard.file",
			"/opt/openems-edge/data/ziot-energy-guard.json"));

	@Reference
	private ConfigurationAdmin cm;

	private ConfigPvInverter config;
	private GenericMapping mapping = new GenericMapping();
	private GenericWriteCapabilities writeCapabilities = GenericWriteCapabilities.of(this.mapping,
			GenericChannelMap.pvInverter());
	private Long lastValidActiveProductionEnergy = null;
	private Long lastValidActiveConsumptionEnergy = null;

	public ZiotGenericPvInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ZiotGenericPvInverter.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, ConfigPvInverter config) throws OpenemsException {
		this.config = config;
		this.mapping = GenericMappingLoader.load(config.mappingFile(), config.model().key());
		this.writeCapabilities = GenericWriteCapabilities.of(this.mapping, GenericChannelMap.pvInverter());
		this.loadEnergyGuardCache();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.updateConfiguredLimits();
		this.activateActivePowerGuard();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return GenericProtocolFactory.create(this, this.mapping, GenericChannelMap.pvInverter(), this::map);
	}

	private io.openems.edge.bridge.modbus.api.element.ModbusElement map(
			io.openems.edge.common.channel.ChannelId channelId,
			io.openems.edge.bridge.modbus.api.element.ModbusElement element,
			io.openems.edge.bridge.modbus.api.ElementToChannelConverter converter) {
		if (channelId == ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY) {
			converter = ElementToChannelConverter.chain(converter,
					new ElementToChannelConverter(value -> this.guardActiveProductionEnergy(value), value -> value));
		}
		if (channelId == ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY) {
			converter = ElementToChannelConverter.chain(converter,
					new ElementToChannelConverter(value -> this.guardActiveConsumptionEnergy(value), value -> value));
		}
		return this.m(channelId, element, converter);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		// Generic PV profile is read-only unless a write register is configured.
	}

	@Override
	public void setActivePowerLimit(Integer value) throws OpenemsNamedException {
		if (value == null) {
			return;
		}
		if (this.writeCapabilities.has(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT)) {
			this.getActivePowerLimitChannel().setNextWriteValue(this.clampPower(value));
			return;
		}
		if (this.writeCapabilities.has(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT)) {
			this.<IntegerWriteChannel>channel(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT)
					.setNextWriteValue(this.clampPower(value));
			return;
		}
		if (this.writeCapabilities.has(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT)) {
			this.<FloatWriteChannel>channel(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT)
					.setNextWriteValue(this.powerToPercent(value, this.hasPowerLimitEnable()));
			this.enablePowerLimitIfConfigured();
			return;
		}
		throw new OpenemsException("No PV active-power write register is configured.");
	}

	@Override
	public void setActivePowerLimit(int value) throws OpenemsNamedException {
		this.setActivePowerLimit(Integer.valueOf(value));
	}

	private void activateActivePowerGuard() {
		this.getActivePowerChannel().onSetNextValue(value -> {
			var activePower = value.get();
			if (activePower == null) {
				return;
			}
			var maxPower = this.getMaxApparentPower().orElse(0);
			if (maxPower <= 0) {
				return;
			}
			if (Math.abs((long) activePower) > Math.round(maxPower * 1.1)) {
				this._setActivePower((Integer) null);
			}
		});
	}

	private Object guardActiveProductionEnergy(Object value) {
		var guardedValue = this.guardEnergy(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, value,
				this.lastValidActiveProductionEnergy);
		if (guardedValue instanceof Number number) {
			this.lastValidActiveProductionEnergy = number.longValue();
			this.saveEnergyGuardValue(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
					this.lastValidActiveProductionEnergy);
		}
		return guardedValue;
	}

	private Object guardActiveConsumptionEnergy(Object value) {
		var guardedValue = this.guardEnergy(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, value,
				this.lastValidActiveConsumptionEnergy);
		if (guardedValue instanceof Number number) {
			this.lastValidActiveConsumptionEnergy = number.longValue();
			this.saveEnergyGuardValue(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
					this.lastValidActiveConsumptionEnergy);
		}
		return guardedValue;
	}

	private Object guardEnergy(ElectricityMeter.ChannelId channelId, Object value, Long lastValidEnergy) {
		if (!(value instanceof Number number)) {
			return value;
		}
		var energy = number.longValue();
		if (lastValidEnergy == null || lastValidEnergy <= 0) {
			return value;
		}
		if (energy < lastValidEnergy) {
			LOG.warn("KWH_GUARD_BLOCKED component={} channel={} old={} new={} reason=DECREASE", this.id(),
					channelId.id(), lastValidEnergy, energy);
			return null;
		}
		var relativeThreshold = Math.round(lastValidEnergy * 1.2);
		var absoluteThreshold = lastValidEnergy + ENERGY_GUARD_MAX_STEP_WH;
		if (energy > relativeThreshold || energy > absoluteThreshold) {
			LOG.warn(
					"KWH_GUARD_BLOCKED component={} channel={} old={} new={} relativeThreshold={} absoluteThreshold={} reason=SPIKE",
					this.id(), channelId.id(), lastValidEnergy, energy, relativeThreshold, absoluteThreshold);
			return null;
		}
		return value;
	}

	private void loadEnergyGuardCache() {
		synchronized (ENERGY_GUARD_LOCK) {
			var cache = readEnergyGuardFile();
			this.lastValidActiveProductionEnergy = readEnergyGuardValue(cache,
					this.energyGuardKey(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY));
			this.lastValidActiveConsumptionEnergy = readEnergyGuardValue(cache,
					this.energyGuardKey(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY));
		}
	}

	private void saveEnergyGuardValue(ElectricityMeter.ChannelId channelId, long value) {
		synchronized (ENERGY_GUARD_LOCK) {
			var cache = readEnergyGuardFile();
			cache.addProperty(this.energyGuardKey(channelId), value);
			writeEnergyGuardFile(cache);
		}
	}

	private String energyGuardKey(ElectricityMeter.ChannelId channelId) {
		return this.config.id() + "/" + channelId.id();
	}

	private static Long readEnergyGuardValue(JsonObject cache, String key) {
		var element = cache.get(key);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			return null;
		}
		return element.getAsLong();
	}

	private static JsonObject readEnergyGuardFile() {
		if (!Files.exists(ENERGY_GUARD_FILE)) {
			return new JsonObject();
		}
		try {
			var json = JsonParser.parseString(Files.readString(ENERGY_GUARD_FILE, StandardCharsets.UTF_8));
			if (json != null && json.isJsonObject()) {
				return json.getAsJsonObject();
			}
		} catch (IOException | RuntimeException e) {
			LOG.warn("Failed to read ZIOT energy guard file [{}]: {}", ENERGY_GUARD_FILE, e.getMessage());
		}
		return new JsonObject();
	}

	private static void writeEnergyGuardFile(JsonObject cache) {
		try {
			Files.createDirectories(ENERGY_GUARD_FILE.getParent());
			Files.writeString(ENERGY_GUARD_FILE, GSON.toJson(cache), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			LOG.warn("Failed to write ZIOT energy guard file [{}]: {}", ENERGY_GUARD_FILE, e.getMessage());
		}
	}

	private int clampPower(int power) throws OpenemsException {
		var maxPower = this.getMaxApparentPower().orElse(0);
		if (maxPower <= 0) {
			throw new OpenemsException("MaxApparentPower must be configured/read before writing an active-power limit.");
		}
		return Math.max(0, Math.min(maxPower, power));
	}

	private float powerToPercent(int power, boolean avoidZeroPercent) throws OpenemsException {
		var maxPower = this.getMaxApparentPower().orElse(0);
		if (maxPower <= 0) {
			throw new OpenemsException(
					"MaxApparentPower must be configured/read before writing an active-power percentage.");
		}
		return clampPercent((float) (power * 100.0 / maxPower), avoidZeroPercent);
	}

	private boolean hasPowerLimitEnable() {
		return this.writeCapabilities.has(ZiotGenericPvInverter.ChannelId.ACTIVE_POWER_LIMIT_ENABLE);
	}

	private void enablePowerLimitIfConfigured() throws OpenemsNamedException {
		if (!this.hasPowerLimitEnable()) {
			return;
		}
		this.<IntegerWriteChannel>channel(ZiotGenericPvInverter.ChannelId.ACTIVE_POWER_LIMIT_ENABLE)
				.setNextWriteValue(1);
	}

	private static float clampPercent(float value, boolean avoidZeroPercent) {
		return Math.max(0, Math.min(110, value));
	}

	private void updateConfiguredLimits() {
		if (this.config == null || this.config.maxApparentPower() <= 0) {
			return;
		}
		this._setMaxApparentPower(this.config.maxApparentPower());
		this._setMaxActivePower(this.config.maxApparentPower());
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
