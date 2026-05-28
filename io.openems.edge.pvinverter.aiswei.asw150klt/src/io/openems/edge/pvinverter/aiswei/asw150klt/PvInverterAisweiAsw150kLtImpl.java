package io.openems.edge.pvinverter.aiswei.asw150klt;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.AISWEI.ASW150K-LT", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class PvInverterAisweiAsw150kLtImpl extends AbstractOpenemsModbusComponent implements PvInverterAisweiAsw150kLt,
		ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave,
		TimedataProvider {

	private final SetActivePowerLimitHandler setActivePowerLimitHandler = new SetActivePowerLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	protected Config config;

	public PvInverterAisweiAsw150kLtImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterAisweiAsw150kLt.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this._setMaxApparentPower(config.maxActivePower());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		var powerLimitConverter = new ElementToChannelConverter(//
				value -> value == null ? null
						: Math.round(((Number) value).intValue() * this.config.maxActivePower() / 10000F), //
				value -> value == null ? null
						: Math.round(((Number) value).intValue() * 10000F / this.config.maxActivePower()));

		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(1001, Priority.LOW, //
						m(PvInverterAisweiAsw150kLt.ChannelId.DEVICE_ADDRESS, new UnsignedWordElement(1001)), //
						m(PvInverterAisweiAsw150kLt.ChannelId.SERIAL_NUMBER, new StringWordElement(1002, 16)), //
						m(PvInverterAisweiAsw150kLt.ChannelId.MODEL, new StringWordElement(1018, 8))), //

				new FC4ReadInputRegistersTask(1056, Priority.LOW, //
						m(PvInverterAisweiAsw150kLt.ChannelId.MANUFACTURER, new StringWordElement(1056, 6)), //
						new DummyRegisterElement(1062, 1074), //
						m(PvInverterAisweiAsw150kLt.ChannelId.PROTOCOL_VERSION, new StringWordElement(1075, 2))), //

				new FC4ReadInputRegistersTask(1301, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(1301), SCALE_FACTOR_1), //
						new DummyRegisterElement(1302, 1303), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(1304),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(1306, 1309), //
						m(PvInverterAisweiAsw150kLt.ChannelId.INTERNAL_TEMPERATURE, new SignedWordElement(1310),
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(1311, 1315), //
						m(PvInverterAisweiAsw150kLt.ChannelId.PV1_VOLTAGE, new UnsignedWordElement(1316),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(1317, 1318), //
						m(PvInverterAisweiAsw150kLt.ChannelId.PV1_CURRENT, new UnsignedWordElement(1319),
								SCALE_FACTOR_1)), //

				new FC4ReadInputRegistersTask(1358, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(1358), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(1359), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(1360), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(1361), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(1362), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(1363), SCALE_FACTOR_2), //
						new DummyRegisterElement(1364, 1367), //
						m(PvInverterAisweiAsw150kLt.ChannelId.APPARENT_POWER, new UnsignedDoublewordElement(1368)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(1370)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(1372)), //
						m(PvInverterAisweiAsw150kLt.ChannelId.POWER_FACTOR, new SignedWordElement(1374),
								SCALE_FACTOR_MINUS_2)), //

				new FC3ReadRegistersTask(5402, Priority.LOW, //
						m(PvInverterAisweiAsw150kLt.ChannelId.ACTIVE_POWER_LIMIT_FIXED,
								new UnsignedWordElement(5402), powerLimitConverter,
								new ChannelMetaInfoReadAndWrite(5402, 5402))), //
				new FC6WriteRegisterTask(5402, //
						m(PvInverterAisweiAsw150kLt.ChannelId.ACTIVE_POWER_LIMIT_FIXED,
								new UnsignedWordElement(5402), powerLimitConverter,
								new ChannelMetaInfoReadAndWrite(5402, 5402))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.config.readOnly()) {
			return;
		}
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
			try {
				this.setActivePowerLimitHandler.run();
				this.channel(PvInverterAisweiAsw150kLt.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(PvInverterAisweiAsw150kLt.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
		}
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
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}

