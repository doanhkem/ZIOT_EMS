package io.openems.edge.ess.omni430pcs;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;

import java.util.concurrent.atomic.AtomicReference;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Omni430Pcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EssOmni430PcsImpl extends AbstractOpenemsModbusComponent
		implements EssOmni430Pcs, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, EventHandler,
		ModbusSlave, TimedataProvider {

	private static final String ADJUST_PROTOCOL_SUFFIX = ".adjust";

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)
	private volatile Timedata timedata = null;

	private final AtomicReference<BridgeModbus> adjustModbus = new AtomicReference<>(null);

	private Config config;
	private ModbusProtocol adjustProtocol = null;

	public EssOmni430PcsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssOmni430Pcs.ChannelId.values() //
		);
	}

	@Override
	@Reference(//
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(//
			name = "AdjustModbus", //
			policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY //
	)
	protected void setAdjustModbus(BridgeModbus modbus) {
		this.adjustModbus.set(modbus);
	}

	protected void unsetAdjustModbus(BridgeModbus modbus) {
		this.adjustModbus.compareAndSet(modbus, null);
		if (modbus != null) {
			modbus.removeProtocol(this.adjustProtocolId());
		}
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "AdjustModbus",
				config.adjust_modbus_id())) {
			return;
		}

		this.addAdjustProtocol();
		this.updateConfiguredLimits();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		var adjustModbus = this.adjustModbus.getAndSet(null);
		if (adjustModbus != null) {
			adjustModbus.removeProtocol(this.adjustProtocolId());
		}
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30021, Priority.HIGH, //
						m(new SignedWordElement(30021)) //
								.m(EssOmni430Pcs.ChannelId.SYSTEM_OUTPUT_POWER, MULTIPLY(100)) //
								.m(SymmetricEss.ChannelId.ACTIVE_POWER, MULTIPLY(100)) //
								.build()), //
				new FC3ReadRegistersTask(30024, Priority.HIGH, //
						m(new SignedWordElement(30024)) //
								.m(SymmetricEss.ChannelId.REACTIVE_POWER, MULTIPLY(100)) //
								.build()), //
				new FC3ReadRegistersTask(30037, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.SYSTEM_SOC, new SignedWordElement(30037))), //
				new FC3ReadRegistersTask(30108, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.SYSTEM_OPERATION_STATUS, new SignedWordElement(30108))), //
				new FC3ReadRegistersTask(32352, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.BESS_CHARGE_ENERGY_TODAY, new SignedWordElement(32352), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.BESS_DISCHARGE_ENERGY_TODAY, new SignedWordElement(32353),
								MULTIPLY(0.1))), //
				new FC3ReadRegistersTask(32376, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, new SignedDoublewordElement(32376),
								MULTIPLY(100)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, new SignedDoublewordElement(32378),
								MULTIPLY(100))), //
				new FC3ReadRegistersTask(32455, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.PCS_OPERATION_STATUS, new UnsignedWordElement(32456)), //
						m(EssOmni430Pcs.ChannelId.PCS_FAULT_STATUS, new UnsignedWordElement(32457))), //
				new FC3ReadRegistersTask(32499, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.PCS_FAULT_WORD_1, new UnsignedWordElement(32499)), //
						m(EssOmni430Pcs.ChannelId.PCS_FAULT_WORD_2, new UnsignedWordElement(32500)), //
						m(EssOmni430Pcs.ChannelId.PCS_FAULT_WORD_3, new UnsignedWordElement(32501)), //
						m(EssOmni430Pcs.ChannelId.PCS_FAULT_WORD_4, new UnsignedWordElement(32502))), //
				new FC3ReadRegistersTask(32516, Priority.HIGH, //
						m(new SignedWordElement(32516)) //
								.m(EssOmni430Pcs.ChannelId.PCS_GRID_STATUS, DIRECT_1_TO_1) //
								.build()
								.onUpdateCallback(this::updateGridMode)), //
				new FC3ReadRegistersTask(32520, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.PCS_ACTIVE_POWER, new SignedWordElement(32520), MULTIPLY(100))), //
				new FC3ReadRegistersTask(32523, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.PCS_REACTIVE_POWER, new SignedWordElement(32523), MULTIPLY(100))), //
				new FC3ReadRegistersTask(32525, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.PCS_POWER_FACTOR, new SignedWordElement(32525)), //
						m(EssOmni430Pcs.ChannelId.PCS_APPARENT_POWER, new SignedWordElement(32526), MULTIPLY(100)), //
						m(EssOmni430Pcs.ChannelId.PCS_AC_FREQUENCY, new SignedWordElement(32527), MULTIPLY(10))), //
				new FC3ReadRegistersTask(32542, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_1, new SignedWordElement(32542), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_2, new SignedWordElement(32543), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_3, new SignedWordElement(32544), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_4, new SignedWordElement(32545), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_5, new SignedWordElement(32546), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_6, new SignedWordElement(32547), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_7, new SignedWordElement(32548), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_TEMPERATURE_8, new SignedWordElement(32549), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.PCS_DC_VOLTAGE, new SignedWordElement(32550), MULTIPLY(100)), //
						m(EssOmni430Pcs.ChannelId.PCS_DC_CURRENT, new SignedWordElement(32551), MULTIPLY(100)), //
						m(EssOmni430Pcs.ChannelId.PCS_DC_POWER, new SignedWordElement(32552), MULTIPLY(100))), //
				new FC3ReadRegistersTask(33135, Priority.HIGH, //
						m(new SignedWordElement(33135)) //
								.m(EssOmni430Pcs.ChannelId.BESS_SOC, MULTIPLY(0.1)) //
								.m(SymmetricEss.ChannelId.SOC, MULTIPLY(0.1)) //
								.build(), //
						m(EssOmni430Pcs.ChannelId.BESS_SOH, new SignedWordElement(33136), MULTIPLY(0.1)), //
						m(EssOmni430Pcs.ChannelId.BESS_DC_VOLTAGE, new SignedWordElement(33137), MULTIPLY(100)), //
						m(EssOmni430Pcs.ChannelId.BESS_DC_CURRENT, new SignedWordElement(33138), MULTIPLY(100))), //
				new FC3ReadRegistersTask(33161, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.BMS_REQUEST_MAX_CHARGE_CURRENT, new SignedWordElement(33161),
								MULTIPLY(100)), //
						m(EssOmni430Pcs.ChannelId.BMS_REQUEST_MAX_DISCHARGE_CURRENT, new SignedWordElement(33162),
								MULTIPLY(100))), //
				new FC3ReadRegistersTask(33165, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.BMS_MAX_CELL_VOLTAGE_NUMBER, new SignedWordElement(33165)), //
						m(new SignedWordElement(33166)) //
								.m(EssOmni430Pcs.ChannelId.BMS_MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(EssOmni430Pcs.ChannelId.BMS_MIN_CELL_VOLTAGE_NUMBER, new SignedWordElement(33169)), //
						m(new SignedWordElement(33170)) //
								.m(EssOmni430Pcs.ChannelId.BMS_MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build()), //
				new FC3ReadRegistersTask(33174, Priority.HIGH, //
						m(EssOmni430Pcs.ChannelId.BMS_MAX_CELL_TEMPERATURE_NUMBER, new SignedWordElement(33174)), //
						m(new SignedWordElement(33175)) //
								.m(EssOmni430Pcs.ChannelId.BMS_MAX_CELL_TEMPERATURE, MULTIPLY(0.1)) //
								.m(SymmetricEss.ChannelId.MAX_CELL_TEMPERATURE, MULTIPLY(0.1)) //
								.build(), //
						m(EssOmni430Pcs.ChannelId.BMS_MIN_CELL_TEMPERATURE_NUMBER, new SignedWordElement(33178)), //
						m(new SignedWordElement(33179)) //
								.m(EssOmni430Pcs.ChannelId.BMS_MIN_CELL_TEMPERATURE, MULTIPLY(0.1)) //
								.m(SymmetricEss.ChannelId.MIN_CELL_TEMPERATURE, MULTIPLY(0.1)) //
								.build()), //
				new FC3ReadRegistersTask(33181, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.BMS_PROTECTED_WORD, new UnsignedWordElement(33181)), //
						m(EssOmni430Pcs.ChannelId.BMS_WARNING_WORD, new UnsignedWordElement(33182)), //
						m(EssOmni430Pcs.ChannelId.BMS_FAULT_WORD, new UnsignedWordElement(33184))) //
		);
	}

	private ModbusProtocol defineAdjustModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(40045, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.WORKING_MODE, new SignedWordElement(40045))), //
				new FC3ReadRegistersTask(40612, Priority.LOW, //
						m(EssOmni430Pcs.ChannelId.GRID_MODE_SWITCH, new SignedWordElement(40612)), //
						m(EssOmni430Pcs.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT, new SignedWordElement(40613),
								MULTIPLY(100))), //
				new FC6WriteRegisterTask(40045, m(EssOmni430Pcs.ChannelId.WORKING_MODE, new SignedWordElement(40045))), //
				new FC6WriteRegisterTask(40612,
						m(EssOmni430Pcs.ChannelId.GRID_MODE_SWITCH, new SignedWordElement(40612))), //
				new FC6WriteRegisterTask(40613, m(EssOmni430Pcs.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT,
						new SignedWordElement(40613), MULTIPLY(100))) //
		);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.updateConfiguredLimits();
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnly()) {
			return;
		}
		this.<IntegerWriteChannel>channel(EssOmni430Pcs.ChannelId.WORKING_MODE).setNextWriteValue(300);
		this.<IntegerWriteChannel>channel(EssOmni430Pcs.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT)
				.setNextWriteValue(activePower);
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() + "|L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private void addAdjustProtocol() {
		var adjustModbus = this.adjustModbus.get();
		if (this.isEnabled() && adjustModbus != null) {
			adjustModbus.addProtocol(this.adjustProtocolId(), this.getAdjustModbusProtocol());
			adjustModbus.retryModbusCommunication(this.adjustProtocolId());
		}
	}

	private ModbusProtocol getAdjustModbusProtocol() {
		var adjustProtocol = this.adjustProtocol;
		if (adjustProtocol != null) {
			return adjustProtocol;
		}
		this.adjustProtocol = this.defineAdjustModbusProtocol();
		return this.adjustProtocol;
	}

	private String adjustProtocolId() {
		return this.id() + ADJUST_PROTOCOL_SUFFIX;
	}

	private void updateConfiguredLimits() {
		if (this.config == null) {
			return;
		}
		this._setCapacity(this.config.capacity());
		this._setMaxApparentPower(this.config.maxApparentPower());
		this.getAllowedChargePowerChannel().setNextValue(-Math.abs(this.config.maxChargePower()));
		this._setAllowedDischargePower(Math.abs(this.config.maxDischargePower()));
	}

	private void updateGridMode(Object value) {
		if (!(value instanceof Number number)) {
			this.getGridModeChannel().setNextValue(GridMode.UNDEFINED);
			return;
		}
		switch (number.intValue()) {
		case 1 -> this.getGridModeChannel().setNextValue(GridMode.ON_GRID);
		case 2 -> this.getGridModeChannel().setNextValue(GridMode.OFF_GRID);
		default -> this.getGridModeChannel().setNextValue(GridMode.UNDEFINED);
		}
	}
}
