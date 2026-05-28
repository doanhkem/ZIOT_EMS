package io.openems.edge.pvinverter.huawei.smartlogger;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.channel.IntegerReadChannel;
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
		name = "PV-Inverter.Huawei.SmartLogger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class PvInverterHuaweiSmartLoggerImpl extends AbstractOpenemsModbusComponent
		implements PvInverterHuaweiSmartLogger, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	private static final ElementToChannelConverter POWER_ADJUSTMENT_CONVERTER = new ElementToChannelConverter(
			value -> ((Number) value).intValue() * 100, //
			value -> Math.round(((Number) value).intValue() / 100F));

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

	public PvInverterHuaweiSmartLoggerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterHuaweiSmartLogger.ChannelId.values() //
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
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(40202, //
						m(PvInverterHuaweiSmartLogger.ChannelId.POWER_ON_OFF, new UnsignedWordElement(40202))), //

				new FC3ReadRegistersTask(40420, Priority.LOW, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_ADJUSTMENT,
								new UnsignedDoublewordElement(40420), POWER_ADJUSTMENT_CONVERTER,
								new ChannelMetaInfoReadAndWrite(40420, 40420))), //
				new FC16WriteRegistersTask(40420, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_ADJUSTMENT,
								new UnsignedDoublewordElement(40420), POWER_ADJUSTMENT_CONVERTER,
								new ChannelMetaInfoReadAndWrite(40420, 40420))), //

				new FC3ReadRegistersTask(40428, Priority.LOW, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_ADJUSTMENT_PERCENT,
								new UnsignedWordElement(40428), new ChannelMetaInfoReadAndWrite(40428, 40428))), //
				new FC16WriteRegistersTask(40428, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_ADJUSTMENT_PERCENT,
								new UnsignedWordElement(40428), new ChannelMetaInfoReadAndWrite(40428, 40428))), //

				new FC3ReadRegistersTask(40521, Priority.HIGH, //
						m(PvInverterHuaweiSmartLogger.ChannelId.INPUT_POWER, new UnsignedDoublewordElement(40521)), //
						new DummyRegisterElement(40523, 40524), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(40525))), //
				new FC3ReadRegistersTask(40543, Priority.HIGH, //
						m(PvInverterHuaweiSmartLogger.ChannelId.PLANT_STATUS, new UnsignedWordElement(40543)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(40544))), //
				new FC3ReadRegistersTask(40560, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(40560),
								SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(40562),
								SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(40572, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(40572), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(40573), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(40574), SCALE_FACTOR_3), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(40575), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(40576), SCALE_FACTOR_2), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(40577), SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(40697, Priority.LOW, //
						m(PvInverterHuaweiSmartLogger.ChannelId.MAX_ACTIVE_ADJUSTMENT,
								new UnsignedDoublewordElement(40697), POWER_ADJUSTMENT_CONVERTER)), //
				new FC3ReadRegistersTask(40737, Priority.LOW, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_CONTROL_MODE,
								new UnsignedWordElement(40737)), //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_POWER_SCHEDULING_TARGET,
								new UnsignedDoublewordElement(40738), POWER_ADJUSTMENT_CONVERTER)), //
				new FC3ReadRegistersTask(40802, Priority.LOW, //
						m(PvInverterHuaweiSmartLogger.ChannelId.ACTIVE_SCHEDULING_PERCENTAGE,
								new UnsignedDoublewordElement(40802))));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.config.readOnly()) {
			return;
		}
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
			try {
				this.setActivePowerLimitHandler.run();
				this.channel(PvInverterHuaweiSmartLogger.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(PvInverterHuaweiSmartLogger.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
		}
	}

	public Value<Integer> getMaxActiveAdjustment() {
		IntegerReadChannel channel = this.channel(PvInverterHuaweiSmartLogger.ChannelId.MAX_ACTIVE_ADJUSTMENT);
		return channel.value();
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
