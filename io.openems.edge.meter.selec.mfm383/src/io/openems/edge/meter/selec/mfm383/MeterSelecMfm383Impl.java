package io.openems.edge.meter.selec.mfm383;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.chain;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.selec.mfm383.Config.ReadRegisterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Selec.MFM383", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSelecMfm383Impl extends AbstractOpenemsModbusComponent
		implements MeterSelecMfm383, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	private MeterType meterType = MeterType.GRID;
	private boolean invert = false;
	private ReadRegisterType readRegisterType = ReadRegisterType.INPUT_REGISTERS;

	public MeterSelecMfm383Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterSelecMfm383.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();
		this.readRegisterType = config.readRegisterType();

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
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				this.read(0, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(0), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(2), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(4), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.VOLTAGE_AVG_LN, new FloatDoublewordElement(6), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.VOLTAGE_L12, new FloatDoublewordElement(8), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.VOLTAGE_L23, new FloatDoublewordElement(10), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.VOLTAGE_L31, new FloatDoublewordElement(12), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.VOLTAGE_AVG_LL, new FloatDoublewordElement(14), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(16), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(18), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(20), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.CURRENT_AVG, new FloatDoublewordElement(22), SCALE_FACTOR_3)), //
				this.read(24, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(24),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(26),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(28),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(MeterSelecMfm383.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(30), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(32), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(34), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(36),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(38),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(40),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(42),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(MeterSelecMfm383.ChannelId.APPARENT_POWER, new FloatDoublewordElement(44), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(46),
								chain(SCALE_FACTOR_3, INVERT_IF_TRUE(this.invert))), //
						m(MeterSelecMfm383.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(48), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(50), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(52), SCALE_FACTOR_3), //
						m(MeterSelecMfm383.ChannelId.POWER_FACTOR, new FloatDoublewordElement(54), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(56), SCALE_FACTOR_3), //
						m(this.invert ? ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
								: ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(58),
								SCALE_FACTOR_3)) //
		);
	}

	private Task read(int startAddress, Priority priority, ModbusElement... elements) {
		return switch (this.readRegisterType) {
		case HOLDING_REGISTERS -> new FC3ReadRegistersTask(startAddress, priority, elements);
		case INPUT_REGISTERS -> new FC4ReadInputRegistersTask(startAddress, priority, elements);
		};
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
