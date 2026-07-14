package io.openems.edge.controller.pvinverter.selltogridlimit;

import java.util.Locale;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.PvInverter.SellToGridLimit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerPvInverterSellToGridLimitImpl extends AbstractOpenemsComponent
		implements ControllerPvInverterSellToGridLimit, Controller, OpenemsComponent {

	public static final double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2;

	private final Logger log = LoggerFactory.getLogger(ControllerPvInverterSellToGridLimitImpl.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;
	private int lastSetLimit = 0;

	public ControllerPvInverterSellToGridLimitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerPvInverterSellToGridLimit.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricPvInverter pvInverter = this.componentManager.getComponent(this.config.pvInverter_id());
		ElectricityMeter meter = this.componentManager.getComponent(this.config.meter_id());

		// Calculates required charge/discharge power
		var gridPower = this.getGridPower(meter, this.config.asymmetricMode());
		var pvPower = pvInverter.getActivePower().getOrError();
		var maximumSellToGridPower = this.getMaximumSellToGridPower(this.config.asymmetricMode());
		var calculatedPower = gridPower + pvPower + maximumSellToGridPower;

		if (Math.abs(this.lastSetLimit) > 100 && Math.abs(calculatedPower) > 100 && Math
				.abs(this.lastSetLimit - calculatedPower) > Math.abs(this.lastSetLimit) * DEFAULT_MAX_ADJUSTMENT_RATE) {
			if (this.lastSetLimit > calculatedPower) {
				calculatedPower = this.lastSetLimit - (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
			} else {
				calculatedPower = this.lastSetLimit + (int) Math.abs(this.lastSetLimit * DEFAULT_MAX_ADJUSTMENT_RATE);
			}
		}
		// store lastSetLimit
		this.lastSetLimit = calculatedPower;

		// set result
		pvInverter.setActivePowerLimit(calculatedPower);
		this.logControlWrite(pvInverter, gridPower, pvPower, maximumSellToGridPower, calculatedPower);
	}

	private int getGridPower(ElectricityMeter meter, boolean asymmetricMode) throws InvalidValueException {
		if (!asymmetricMode) {
			return meter.getActivePower().getOrError();
		}
		var gridPowerL1 = meter.getActivePowerL1().getOrError();
		var gridPowerL2 = meter.getActivePowerL2().getOrError();
		var gridPowerL3 = meter.getActivePowerL3().getOrError();
		return Math.min(Math.min(gridPowerL1, gridPowerL2), gridPowerL3) * 3;
	}

	private int getMaximumSellToGridPower(boolean asymmetricMode) {
		var maximumSellToGridPower = this.config.maximumSellToGridPower();
		return asymmetricMode ? maximumSellToGridPower * 3 : maximumSellToGridPower;
	}

	private void logControlWrite(ManagedSymmetricPvInverter pvInverter, int gridPower, int pvPower,
			int maximumSellToGridPower, int calculatedPower) {
		var writePercent = this.toPercent(pvInverter, calculatedPower);
		this.log.info("CTRL_WRITE_OK ⚡ Grid={} kW | 🏭 Load={} kW | ☀ PV={} kW | 🎯 Limit={} kW | ✍ Write={}{} -> {}",
				formatKw(gridPower), formatKw(gridPower + pvPower), formatKw(pvPower), formatKw(maximumSellToGridPower),
				writePercent == null ? formatKw(calculatedPower) : writePercent, writePercent == null ? " kW" : "%",
				this.config.pvInverter_id());
	}

	private String toPercent(ManagedSymmetricPvInverter pvInverter, int power) {
		var maxPower = pvInverter.getMaxApparentPower().orElse(0);
		if (maxPower <= 0) {
			return null;
		}
		var percent = Math.max(-100, Math.min(100, Math.round(power * 100F / maxPower)));
		return String.format(Locale.ROOT, "%.2f", (double) percent);
	}

	private static String formatKw(int watts) {
		return String.format(Locale.ROOT, "%.2f", watts / 1000.0);
	}
}
