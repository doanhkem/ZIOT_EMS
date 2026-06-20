package io.openems.edge.ess.omni261;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.MULTIPLY;

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
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
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
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Omni261", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class EssOmni261Impl extends AbstractOpenemsModbusComponent
		implements EssOmni261, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, EventHandler,
		ModbusSlave, TimedataProvider {

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

	private Config config;
	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	public EssOmni261Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssOmni261.ChannelId.values() //
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

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.updateConfiguredLimits();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(570, Priority.LOW, //
						m(EssOmni261.ChannelId.SYSTEM_OUTPUT_POWER, new SignedWordElement(570), MULTIPLY(100))), //
				new FC3ReadRegistersTask(587, Priority.LOW, //
						m(EssOmni261.ChannelId.SYSTEM_SOC, new UnsignedWordElement(587), MULTIPLY(0.1)), //
						m(EssOmni261.ChannelId.BESS_CHARGE_ENERGY_TODAY, new UnsignedDoublewordElement(588)), //
						m(EssOmni261.ChannelId.BESS_DISCHARGE_ENERGY_TODAY, new UnsignedDoublewordElement(590))), //
				new FC3ReadRegistersTask(952, Priority.LOW, //
						m(EssOmni261.ChannelId.SYSTEM_OPERATION_STATUS, new UnsignedWordElement(952))), //

				new FC3ReadRegistersTask(25101, Priority.HIGH, //
						m(EssOmni261.ChannelId.PCS_OPERATION_STATUS, new UnsignedWordElement(25101)), //
						m(EssOmni261.ChannelId.PCS_FAULT_STATUS, new UnsignedWordElement(25102)), //
						m(new SignedWordElement(25103)) //
								.m(EssOmni261.ChannelId.PCS_ACTIVE_POWER, MULTIPLY(100)) //
								.m(SymmetricEss.ChannelId.ACTIVE_POWER, MULTIPLY(100)) //
								.build(), //
						m(new SignedWordElement(25104)) //
								.m(EssOmni261.ChannelId.PCS_REACTIVE_POWER, MULTIPLY(100)) //
								.m(SymmetricEss.ChannelId.REACTIVE_POWER, MULTIPLY(100)) //
								.build(), //
						m(EssOmni261.ChannelId.PCS_APPARENT_POWER, new SignedWordElement(25105), MULTIPLY(100)), //
						m(new UnsignedWordElement(25106)) //
								.m(EssOmni261.ChannelId.PCS_GRID_STATUS, DIRECT_1_TO_1) //
								.build()
								.onUpdateCallback(this::updateGridMode), //
						m(EssOmni261.ChannelId.PCS_AC_VOLTAGE_L1, new UnsignedWordElement(25107), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_VOLTAGE_L2, new UnsignedWordElement(25108), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_VOLTAGE_L3, new UnsignedWordElement(25109), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_CURRENT_L1, new SignedWordElement(25110), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_CURRENT_L2, new SignedWordElement(25111), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_CURRENT_L3, new SignedWordElement(25112), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_ACTIVE_POWER, new SignedWordElement(25113), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_REACTIVE_POWER, new SignedWordElement(25114), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_APPARENT_POWER, new SignedWordElement(25115), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_AC_FREQUENCY, new UnsignedWordElement(25116), MULTIPLY(10)), //
						m(EssOmni261.ChannelId.PCS_POWER_FACTOR, new SignedWordElement(25117)), //
						m(EssOmni261.ChannelId.PCS_DC_VOLTAGE, new UnsignedWordElement(25118), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_DC_CURRENT, new SignedWordElement(25119), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_DC_POWER, new SignedWordElement(25120), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_1, new SignedWordElement(25121)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_2, new SignedWordElement(25122)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_3, new SignedWordElement(25123)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_4, new SignedWordElement(25124)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_5, new SignedWordElement(25125)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_6, new SignedWordElement(25126)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_7, new SignedWordElement(25127)), //
						m(EssOmni261.ChannelId.PCS_TEMPERATURE_8, new SignedWordElement(25128)), //
						m(EssOmni261.ChannelId.PCS_ACTIVE_POWER_L1, new SignedWordElement(25129), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_ACTIVE_POWER_L2, new SignedWordElement(25130), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_ACTIVE_POWER_L3, new SignedWordElement(25131), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.PCS_FAULT_WORD_1, new UnsignedWordElement(25132)), //
						m(EssOmni261.ChannelId.PCS_FAULT_WORD_2, new UnsignedWordElement(25133)), //
						m(EssOmni261.ChannelId.PCS_FAULT_WORD_3, new UnsignedWordElement(25134)), //
						m(EssOmni261.ChannelId.PCS_FAULT_WORD_4, new UnsignedWordElement(25135))), //

				new FC3ReadRegistersTask(25200, Priority.HIGH, //
						m(EssOmni261.ChannelId.BESS_DC_VOLTAGE, new SignedWordElement(25200), MULTIPLY(100)), //
						m(EssOmni261.ChannelId.BESS_DC_CURRENT, new SignedWordElement(25201), MULTIPLY(1000)), //
						m(new SignedWordElement(25202)) //
								.m(EssOmni261.ChannelId.BESS_SOC, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.SOC, DIRECT_1_TO_1) //
								.build(), //
						m(EssOmni261.ChannelId.BESS_SOH, new SignedWordElement(25203)), //
						m(EssOmni261.ChannelId.BMS_REQUEST_MAX_CHARGE_CURRENT, new SignedWordElement(25204), MULTIPLY(1000)), //
						m(EssOmni261.ChannelId.BMS_REQUEST_MAX_DISCHARGE_CURRENT, new SignedWordElement(25205), MULTIPLY(1000)), //
						m(new SignedWordElement(25206)) //
								.m(EssOmni261.ChannelId.BMS_MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MAX_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(new SignedWordElement(25207)) //
								.m(EssOmni261.ChannelId.BMS_MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MIN_CELL_VOLTAGE, DIRECT_1_TO_1) //
								.build(), //
						m(EssOmni261.ChannelId.BMS_MAX_CELL_VOLTAGE_NUMBER, new SignedWordElement(25208)), //
						m(EssOmni261.ChannelId.BMS_MIN_CELL_VOLTAGE_NUMBER, new SignedWordElement(25209)), //
						m(new SignedWordElement(25210)) //
								.m(EssOmni261.ChannelId.BMS_MAX_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MAX_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.build(), //
						m(new SignedWordElement(25211)) //
								.m(EssOmni261.ChannelId.BMS_MIN_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.m(SymmetricEss.ChannelId.MIN_CELL_TEMPERATURE, DIRECT_1_TO_1) //
								.build(), //
						m(EssOmni261.ChannelId.BMS_MAX_CELL_TEMPERATURE_NUMBER, new SignedWordElement(25212)), //
						m(EssOmni261.ChannelId.BMS_MIN_CELL_TEMPERATURE_NUMBER, new SignedWordElement(25213)), //
						m(EssOmni261.ChannelId.BMS_CELL_TEMPERATURE_DELTA, new SignedWordElement(25214)), //
						m(EssOmni261.ChannelId.BMS_CELL_VOLTAGE_DELTA, new SignedWordElement(25215)), //
						m(EssOmni261.ChannelId.BMS_FAULT_WORD, new UnsignedWordElement(25216)), //
						m(EssOmni261.ChannelId.BMS_WARNING_WORD, new UnsignedWordElement(25217)), //
						m(EssOmni261.ChannelId.BMS_PROTECTED_WORD, new UnsignedWordElement(25218)), //
						m(EssOmni261.ChannelId.BMS_SYSTEM_STATUS_HIGH, new UnsignedWordElement(25219)), //
						m(EssOmni261.ChannelId.BMS_SYSTEM_STATUS_LOW, new UnsignedWordElement(25220))), //

				new FC3ReadRegistersTask(611, Priority.LOW, //
						m(EssOmni261.ChannelId.SYSTEM_SHUTDOWN, new SignedWordElement(611)), //
						m(EssOmni261.ChannelId.GRID_MODE_SWITCH, new SignedWordElement(612))), //
				new FC3ReadRegistersTask(615, Priority.LOW, //
						m(EssOmni261.ChannelId.WORKING_MODE, new UnsignedWordElement(615))), //
				new FC3ReadRegistersTask(618, Priority.LOW, //
						m(EssOmni261.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT, new SignedWordElement(618), MULTIPLY(100))), //

				new FC6WriteRegisterTask(611, m(EssOmni261.ChannelId.SYSTEM_SHUTDOWN, new SignedWordElement(611))), //
				new FC6WriteRegisterTask(612, m(EssOmni261.ChannelId.GRID_MODE_SWITCH, new SignedWordElement(612))), //
				new FC6WriteRegisterTask(615, m(EssOmni261.ChannelId.WORKING_MODE, new UnsignedWordElement(615))), //
				new FC6WriteRegisterTask(618,
						m(EssOmni261.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT, new SignedWordElement(618), MULTIPLY(100))) //
		);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.updateConfiguredLimits();
			this.updateEnergyFromActivePower();
		}
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
		this.<IntegerWriteChannel>channel(EssOmni261.ChannelId.WORKING_MODE).setNextWriteValue(2);
		this.<IntegerWriteChannel>channel(EssOmni261.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT).setNextWriteValue(activePower);
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

	private void updateConfiguredLimits() {
		if (this.config == null) {
			return;
		}
		this._setCapacity(this.config.capacity());
		this._setMaxApparentPower(this.config.maxApparentPower());
		this.getAllowedChargePowerChannel().setNextValue(-Math.abs(this.config.maxChargePower()));
		this._setAllowedDischargePower(Math.abs(this.config.maxDischargePower()));
	}

	private void updateEnergyFromActivePower() {
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			this.calculateChargeEnergy.update(-activePower);
			this.calculateDischargeEnergy.update(0);
		}
	}

	private void updateGridMode(Object value) {
		if (!(value instanceof Number number)) {
			this.getGridModeChannel().setNextValue(GridMode.UNDEFINED);
			return;
		}
		switch (number.intValue()) {
		case 3 -> this.getGridModeChannel().setNextValue(GridMode.ON_GRID);
		case 4 -> this.getGridModeChannel().setNextValue(GridMode.OFF_GRID);
		default -> this.getGridModeChannel().setNextValue(GridMode.UNDEFINED);
		}
	}

}
