package io.openems.edge.pvinverter.sungrow.sg110cx;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Sungrow.SG110CX", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
public class PvInverterSungrowSg110CxImpl extends AbstractOpenemsModbusComponent implements PvInverterSungrowSg110Cx,
		ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private static final ElementToChannelConverter POWER_FACTOR_SCALE_FACTOR = new ElementToChannelConverter(//
			value -> value == null ? null : ((Number) value).floatValue() * 0.001F, //
			value -> value == null ? null : Math.round(((Number) value).floatValue() * 1000F));

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(//
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public PvInverterSungrowSg110CxImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterSungrowSg110Cx.ChannelId.values() //
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
				new FC3ReadRegistersTask(5004, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
								new UnsignedDoublewordElement(5004).wordOrder(LSWMSW), SCALE_FACTOR_3)), //
				new FC3ReadRegistersTask(5008, Priority.HIGH, //
						m(PvInverterSungrowSg110Cx.ChannelId.TMP_CAB, new SignedWordElement(5008),
								SCALE_FACTOR_MINUS_1)), //
				new FC3ReadRegistersTask(5011, Priority.HIGH, //
						m(new UnsignedWordElement(5011)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV1_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV2_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5012), //
						m(new UnsignedWordElement(5013)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV3_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV4_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5014), //
						m(new UnsignedWordElement(5015)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV5_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV6_VOLTAGE, SCALE_FACTOR_2) //
								.build()), //
				new FC3ReadRegistersTask(5019, Priority.HIGH, //
						m(PvInverterSungrowSg110Cx.ChannelId.VOLTAGE_L1_L2, new UnsignedWordElement(5019),
								SCALE_FACTOR_2), //
						m(PvInverterSungrowSg110Cx.ChannelId.VOLTAGE_L2_L3, new UnsignedWordElement(5020),
								SCALE_FACTOR_2), //
						m(PvInverterSungrowSg110Cx.ChannelId.VOLTAGE_L3_L1, new UnsignedWordElement(5021),
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(5022), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(5023), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(5024), SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(5031, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER,
								new UnsignedDoublewordElement(5031).wordOrder(LSWMSW)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(5033).wordOrder(LSWMSW)), //
						m(PvInverterSungrowSg110Cx.ChannelId.POWER_FACTOR, new SignedWordElement(5035),
								POWER_FACTOR_SCALE_FACTOR), //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(5036), SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(5038, Priority.HIGH, //
						m(PvInverterSungrowSg110Cx.ChannelId.OPERATING_STATUS, new UnsignedWordElement(5038))), //
				new FC3ReadRegistersTask(5115, Priority.HIGH, //
						m(new UnsignedWordElement(5115)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV7_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV8_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5116), //
						m(new UnsignedWordElement(5117)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV9_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV10_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5118), //
						m(new UnsignedWordElement(5119)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV11_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV12_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5120), //
						m(new UnsignedWordElement(5121)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV13_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV14_VOLTAGE, SCALE_FACTOR_2) //
								.build(), //
						new DummyRegisterElement(5122), //
						m(new UnsignedWordElement(5123)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV15_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV16_VOLTAGE, SCALE_FACTOR_2) //
								.build()), //
				new FC3ReadRegistersTask(5130, Priority.HIGH, //
						m(new UnsignedWordElement(5130)) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV17_VOLTAGE, SCALE_FACTOR_2) //
								.m(PvInverterSungrowSg110Cx.ChannelId.PV18_VOLTAGE, SCALE_FACTOR_2) //
								.build()), //
				new FC3ReadRegistersTask(7013, Priority.HIGH, //
						m(PvInverterSungrowSg110Cx.ChannelId.PV1_CURRENT, new UnsignedWordElement(7013),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV2_CURRENT, new UnsignedWordElement(7014),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV3_CURRENT, new UnsignedWordElement(7015),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV4_CURRENT, new UnsignedWordElement(7016),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV5_CURRENT, new UnsignedWordElement(7017),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV6_CURRENT, new UnsignedWordElement(7018),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV7_CURRENT, new UnsignedWordElement(7019),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV8_CURRENT, new UnsignedWordElement(7020),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV9_CURRENT, new UnsignedWordElement(7021),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV10_CURRENT, new UnsignedWordElement(7022),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV11_CURRENT, new UnsignedWordElement(7023),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV12_CURRENT, new UnsignedWordElement(7024),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV13_CURRENT, new UnsignedWordElement(7025),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV14_CURRENT, new UnsignedWordElement(7026),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV15_CURRENT, new UnsignedWordElement(7027),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV16_CURRENT, new UnsignedWordElement(7028),
								SCALE_FACTOR_1), //
						m(PvInverterSungrowSg110Cx.ChannelId.PV17_CURRENT, new UnsignedWordElement(7029),
								SCALE_FACTOR_1)), //
				new FC3ReadRegistersTask(7032, Priority.HIGH, //
						m(PvInverterSungrowSg110Cx.ChannelId.PV18_CURRENT, new UnsignedWordElement(7032),
								SCALE_FACTOR_1)) //
		);
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
		// Read-only telemetry driver.
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}
}
