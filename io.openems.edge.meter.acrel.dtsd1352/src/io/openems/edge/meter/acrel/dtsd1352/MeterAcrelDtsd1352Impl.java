package io.openems.edge.meter.acrel.dtsd1352;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;
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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Acrel.DTSD1352", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterAcrelDtsd1352Impl extends AbstractOpenemsModbusComponent
		implements MeterAcrelDtsd1352, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	private MeterType meterType = MeterType.GRID;
	private boolean invert = false;
	private double voltageRatio = 1.0;
	private double currentRatio = 1.0;

	public MeterAcrelDtsd1352Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterAcrelDtsd1352.ChannelId.values() //
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
		this.voltageRatio = config.voltageRatio();
		this.currentRatio = config.currentRatio();

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
		var voltage = MULTIPLY(this.voltageRatio * 100.0);
		var current = MULTIPLY(this.currentRatio * 10.0);
		var power = MULTIPLY(this.voltageRatio * this.currentRatio);
		var signedPower = chain(power, INVERT_IF_TRUE(this.invert));
		var energy = MULTIPLY(this.voltageRatio * this.currentRatio * 10.0);

		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x000A, Priority.LOW, //
						m(this.forwardEnergyChannel(), new UnsignedDoublewordElement(0x000A), energy)), //
				new FC3ReadRegistersTask(0x0014, Priority.LOW, //
						m(this.reverseEnergyChannel(), new UnsignedDoublewordElement(0x0014), energy)), //
				new FC3ReadRegistersTask(0x0061, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x0061), voltage), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x0062), voltage), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x0063), voltage), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(0x0064), current), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(0x0065), current), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(0x0066), current)), //
				new FC3ReadRegistersTask(0x0077, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(0x0077), MULTIPLY(10)), //
						m(MeterAcrelDtsd1352.ChannelId.VOLTAGE_L1_L2, new UnsignedWordElement(0x0078), voltage), //
						m(MeterAcrelDtsd1352.ChannelId.VOLTAGE_L2_L3, new UnsignedWordElement(0x0079), voltage), //
						m(MeterAcrelDtsd1352.ChannelId.VOLTAGE_L3_L1, new UnsignedWordElement(0x007A), voltage)), //
				new FC3ReadRegistersTask(0x0091, Priority.LOW, //
						m(MeterAcrelDtsd1352.ChannelId.STATUS_REGISTER, new UnsignedWordElement(0x0091))), //
				new FC3ReadRegistersTask(0x0164, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x0164),
								signedPower), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x0166),
								signedPower), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x0168),
								signedPower), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x016A), signedPower), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x016C),
								signedPower), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x016E),
								signedPower), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x0170),
								signedPower), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x0172),
								signedPower), //
						m(MeterAcrelDtsd1352.ChannelId.APPARENT_POWER_L1,
								new UnsignedDoublewordElement(0x0174), power), //
						m(MeterAcrelDtsd1352.ChannelId.APPARENT_POWER_L2,
								new UnsignedDoublewordElement(0x0176), power), //
						m(MeterAcrelDtsd1352.ChannelId.APPARENT_POWER_L3,
								new UnsignedDoublewordElement(0x0178), power), //
						m(MeterAcrelDtsd1352.ChannelId.APPARENT_POWER,
								new UnsignedDoublewordElement(0x017A), power), //
						m(MeterAcrelDtsd1352.ChannelId.POWER_FACTOR_L1, new SignedWordElement(0x017C)), //
						m(MeterAcrelDtsd1352.ChannelId.POWER_FACTOR_L2, new SignedWordElement(0x017D)), //
						m(MeterAcrelDtsd1352.ChannelId.POWER_FACTOR_L3, new SignedWordElement(0x017E)), //
						m(MeterAcrelDtsd1352.ChannelId.POWER_FACTOR, new SignedWordElement(0x017F))) //
		);
	}

	private ElectricityMeter.ChannelId forwardEnergyChannel() {
		return this.invert ? ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY
				: ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
	}

	private ElectricityMeter.ChannelId reverseEnergyChannel() {
		return this.invert ? ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY
				: ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
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
