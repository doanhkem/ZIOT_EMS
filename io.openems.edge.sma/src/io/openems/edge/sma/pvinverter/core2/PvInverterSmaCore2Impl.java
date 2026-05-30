package io.openems.edge.sma.pvinverter.core2;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.SMA.CORE2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
public class PvInverterSmaCore2Impl extends AbstractOpenemsModbusComponent implements PvInverterSmaCore2,
		ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private static final ElementToChannelConverter PF_SCALE_FACTOR = new ElementToChannelConverter(//
			value -> value == null ? null : ((Number) value).floatValue() * 0.001F, //
			value -> value == null ? null : Math.round(((Number) value).floatValue() * 1000F));

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public PvInverterSmaCore2Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterSmaCore2.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(40004, Priority.LOW, //
						m(PvInverterSmaCore2.ChannelId.MANUFACTURER, new StringWordElement(40004, 16)), //
						m(PvInverterSmaCore2.ChannelId.MODEL, new StringWordElement(40020, 24)), //
						m(PvInverterSmaCore2.ChannelId.VERSION, new StringWordElement(40044, 8)), //
						m(PvInverterSmaCore2.ChannelId.SERIAL_NUMBER, new StringWordElement(40052, 11)), //
						new DummyRegisterElement(40063, 40067), //
						m(PvInverterSmaCore2.ChannelId.DEVICE_ADDRESS, new UnsignedWordElement(40068))), //

				new FC3ReadRegistersTask(40073, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(40073), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(40074), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(40075), SCALE_FACTOR_2)), //

				new FC3ReadRegistersTask(40080, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(40080), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(40081), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(40082), SCALE_FACTOR_2), //
						new DummyRegisterElement(40083), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(40084), SCALE_FACTOR_1), //
						new DummyRegisterElement(40085), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(40086), SCALE_FACTOR_1), //
						new DummyRegisterElement(40087), //
						m(PvInverterSmaCore2.ChannelId.APPARENT_POWER, new SignedWordElement(40088), SCALE_FACTOR_1), //
						new DummyRegisterElement(40089), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(40090), SCALE_FACTOR_1), //
						new DummyRegisterElement(40091), //
						m(PvInverterSmaCore2.ChannelId.POWER_FACTOR, new SignedWordElement(40092), PF_SCALE_FACTOR), //
						new DummyRegisterElement(40093), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(40094),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(40096), //
						m(PvInverterSmaCore2.ChannelId.DC_AMPS, new UnsignedWordElement(40097), SCALE_FACTOR_1), //
						new DummyRegisterElement(40098), //
						m(PvInverterSmaCore2.ChannelId.DC_VOLT, new UnsignedWordElement(40099), SCALE_FACTOR_2), //
						new DummyRegisterElement(40100), //
						m(PvInverterSmaCore2.ChannelId.DC_WATTS, new SignedWordElement(40101), SCALE_FACTOR_1), //
						new DummyRegisterElement(40102), //
						m(PvInverterSmaCore2.ChannelId.TMP_CAB, new SignedWordElement(40103), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(40104, 40107), //
						m(PvInverterSmaCore2.ChannelId.OPERATING_STATUS, new UnsignedWordElement(40108))));
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
		// Read-only driver for the explicitly mapped SMA CORE2 telemetry registers.
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}
}
