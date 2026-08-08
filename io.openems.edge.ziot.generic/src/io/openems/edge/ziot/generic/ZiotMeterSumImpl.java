package io.openems.edge.ziot.generic;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = ConfigMeterSum.class, factory = true)
@Component(//
		name = "Ziot.Meter.Sum", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class ZiotMeterSumImpl extends AbstractOpenemsComponent
		implements ZiotMeterSum, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(ZiotMeterSumImpl.class);

	@Reference
	private ComponentManager componentManager;

	private ConfigMeterSum config;

	public ZiotMeterSumImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigMeterSum config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		this.calculateChannelValues();
	}

	private void calculateChannelValues() {
		final var frequency = new CalculateAverage();
		final var activePower = new CalculateIntegerSum();
		final var activePowerL1 = new CalculateIntegerSum();
		final var activePowerL2 = new CalculateIntegerSum();
		final var activePowerL3 = new CalculateIntegerSum();
		final var reactivePower = new CalculateIntegerSum();
		final var reactivePowerL1 = new CalculateIntegerSum();
		final var reactivePowerL2 = new CalculateIntegerSum();
		final var reactivePowerL3 = new CalculateIntegerSum();
		final var activeProductionEnergy = new CalculateLongSum();
		final var activeProductionEnergyL1 = new CalculateLongSum();
		final var activeProductionEnergyL2 = new CalculateLongSum();
		final var activeProductionEnergyL3 = new CalculateLongSum();
		final var activeConsumptionEnergy = new CalculateLongSum();
		final var activeConsumptionEnergyL1 = new CalculateLongSum();
		final var activeConsumptionEnergyL2 = new CalculateLongSum();
		final var activeConsumptionEnergyL3 = new CalculateLongSum();
		final var voltage = new CalculateAverage();
		final var voltageL1 = new CalculateAverage();
		final var voltageL2 = new CalculateAverage();
		final var voltageL3 = new CalculateAverage();
		final var current = new CalculateIntegerSum();
		final var currentL1 = new CalculateIntegerSum();
		final var currentL2 = new CalculateIntegerSum();
		final var currentL3 = new CalculateIntegerSum();

		for (var meter : this.getMeters()) {
			frequency.addValue(meter.getFrequencyChannel());
			activePower.addValue(meter.getActivePowerChannel());
			activePowerL1.addValue(meter.getActivePowerL1Channel());
			activePowerL2.addValue(meter.getActivePowerL2Channel());
			activePowerL3.addValue(meter.getActivePowerL3Channel());
			reactivePower.addValue(meter.getReactivePowerChannel());
			reactivePowerL1.addValue(meter.getReactivePowerL1Channel());
			reactivePowerL2.addValue(meter.getReactivePowerL2Channel());
			reactivePowerL3.addValue(meter.getReactivePowerL3Channel());
			activeProductionEnergy.addValue(meter.getActiveProductionEnergyChannel());
			activeProductionEnergyL1.addValue(meter.getActiveProductionEnergyL1Channel());
			activeProductionEnergyL2.addValue(meter.getActiveProductionEnergyL2Channel());
			activeProductionEnergyL3.addValue(meter.getActiveProductionEnergyL3Channel());
			activeConsumptionEnergy.addValue(meter.getActiveConsumptionEnergyChannel());
			activeConsumptionEnergyL1.addValue(meter.getActiveConsumptionEnergyL1Channel());
			activeConsumptionEnergyL2.addValue(meter.getActiveConsumptionEnergyL2Channel());
			activeConsumptionEnergyL3.addValue(meter.getActiveConsumptionEnergyL3Channel());
			voltage.addValue(meter.getVoltageChannel());
			voltageL1.addValue(meter.getVoltageL1Channel());
			voltageL2.addValue(meter.getVoltageL2Channel());
			voltageL3.addValue(meter.getVoltageL3Channel());
			current.addValue(meter.getCurrentChannel());
			currentL1.addValue(meter.getCurrentL1Channel());
			currentL2.addValue(meter.getCurrentL2Channel());
			currentL3.addValue(meter.getCurrentL3Channel());
		}

		this._setFrequency(frequency.calculateRounded());
		this._setActivePower(activePower.calculate());
		this._setActivePowerL1(activePowerL1.calculate());
		this._setActivePowerL2(activePowerL2.calculate());
		this._setActivePowerL3(activePowerL3.calculate());
		this._setReactivePower(reactivePower.calculate());
		this._setReactivePowerL1(reactivePowerL1.calculate());
		this._setReactivePowerL2(reactivePowerL2.calculate());
		this._setReactivePowerL3(reactivePowerL3.calculate());
		this._setActiveProductionEnergy(activeProductionEnergy.calculate());
		this._setActiveProductionEnergyL1(activeProductionEnergyL1.calculate());
		this._setActiveProductionEnergyL2(activeProductionEnergyL2.calculate());
		this._setActiveProductionEnergyL3(activeProductionEnergyL3.calculate());
		this._setActiveConsumptionEnergy(activeConsumptionEnergy.calculate());
		this._setActiveConsumptionEnergyL1(activeConsumptionEnergyL1.calculate());
		this._setActiveConsumptionEnergyL2(activeConsumptionEnergyL2.calculate());
		this._setActiveConsumptionEnergyL3(activeConsumptionEnergyL3.calculate());
		this._setVoltage(voltage.calculateRounded());
		this._setVoltageL1(voltageL1.calculateRounded());
		this._setVoltageL2(voltageL2.calculateRounded());
		this._setVoltageL3(voltageL3.calculateRounded());
		this._setCurrent(current.calculate());
		this._setCurrentL1(currentL1.calculate());
		this._setCurrentL2(currentL2.calculate());
		this._setCurrentL3(currentL3.calculate());
	}

	private List<ElectricityMeter> getMeters() {
		List<ElectricityMeter> result = new ArrayList<>();
		for (String meterId : this.config.meter_ids()) {
			try {
				ElectricityMeter meter = this.componentManager.getComponent(meterId);
				result.add(meter);
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to use meter [" + meterId + "] in meter sum: " + e.getMessage());
			}
		}
		return result;
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public boolean addToSum() {
		return this.config.addToSum();
	}

	@Override
	public String[] getMeterIds() {
		return this.config.meter_ids();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
