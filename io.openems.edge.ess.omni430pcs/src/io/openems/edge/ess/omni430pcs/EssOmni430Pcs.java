package io.openems.edge.ess.omni430pcs;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.DEGREE_CELSIUS;
import static io.openems.common.channel.Unit.DEZIDEGREE_CELSIUS;
import static io.openems.common.channel.Unit.KILOWATT_HOURS;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.MILLIHERTZ;
import static io.openems.common.channel.Unit.MILLIVOLT;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.THOUSANDTH;
import static io.openems.common.channel.Unit.VOLT_AMPERE;
import static io.openems.common.channel.Unit.VOLT_AMPERE_REACTIVE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface EssOmni430Pcs
		extends ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SYSTEM_OUTPUT_POWER(Doc.of(INTEGER).unit(WATT)), //
		SYSTEM_OPERATION_STATUS(Doc.of(INTEGER)), //
		SYSTEM_SOC(Doc.of(INTEGER).unit(PERCENT)), //
		BESS_CHARGE_ENERGY_TODAY(Doc.of(INTEGER).unit(KILOWATT_HOURS)), //
		BESS_DISCHARGE_ENERGY_TODAY(Doc.of(INTEGER).unit(KILOWATT_HOURS)), //

		PCS_OPERATION_STATUS(Doc.of(INTEGER)), //
		PCS_FAULT_STATUS(Doc.of(INTEGER)), //
		PCS_ACTIVE_POWER(Doc.of(INTEGER).unit(WATT)), //
		PCS_REACTIVE_POWER(Doc.of(INTEGER).unit(VOLT_AMPERE_REACTIVE)), //
		PCS_APPARENT_POWER(Doc.of(INTEGER).unit(VOLT_AMPERE)), //
		PCS_GRID_STATUS(Doc.of(INTEGER)), //
		PCS_AC_VOLTAGE_L1(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PCS_AC_VOLTAGE_L2(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PCS_AC_VOLTAGE_L3(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PCS_AC_CURRENT_L1(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PCS_AC_CURRENT_L2(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PCS_AC_CURRENT_L3(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PCS_AC_ACTIVE_POWER(Doc.of(INTEGER).unit(WATT)), //
		PCS_AC_REACTIVE_POWER(Doc.of(INTEGER).unit(VOLT_AMPERE_REACTIVE)), //
		PCS_AC_APPARENT_POWER(Doc.of(INTEGER).unit(VOLT_AMPERE)), //
		PCS_AC_FREQUENCY(Doc.of(INTEGER).unit(MILLIHERTZ)), //
		PCS_POWER_FACTOR(Doc.of(INTEGER).unit(THOUSANDTH)), //
		PCS_DC_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PCS_DC_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PCS_DC_POWER(Doc.of(INTEGER).unit(WATT)), //
		PCS_TEMPERATURE_1(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_2(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_3(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_4(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_5(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_6(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_7(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_TEMPERATURE_8(Doc.of(INTEGER).unit(DEZIDEGREE_CELSIUS)), //
		PCS_ACTIVE_POWER_L1(Doc.of(INTEGER).unit(WATT)), //
		PCS_ACTIVE_POWER_L2(Doc.of(INTEGER).unit(WATT)), //
		PCS_ACTIVE_POWER_L3(Doc.of(INTEGER).unit(WATT)), //
		PCS_FAULT_WORD_1(Doc.of(INTEGER)), //
		PCS_FAULT_WORD_2(Doc.of(INTEGER)), //
		PCS_FAULT_WORD_3(Doc.of(INTEGER)), //
		PCS_FAULT_WORD_4(Doc.of(INTEGER)), //

		BESS_DC_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		BESS_DC_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		BESS_SOC(Doc.of(INTEGER).unit(PERCENT)), //
		BESS_SOH(Doc.of(INTEGER).unit(PERCENT)), //
		BMS_REQUEST_MAX_CHARGE_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		BMS_REQUEST_MAX_DISCHARGE_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		BMS_MAX_CELL_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		BMS_MIN_CELL_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		BMS_MAX_CELL_VOLTAGE_NUMBER(Doc.of(INTEGER)), //
		BMS_MIN_CELL_VOLTAGE_NUMBER(Doc.of(INTEGER)), //
		BMS_MAX_CELL_TEMPERATURE(Doc.of(INTEGER).unit(DEGREE_CELSIUS)), //
		BMS_MIN_CELL_TEMPERATURE(Doc.of(INTEGER).unit(DEGREE_CELSIUS)), //
		BMS_MAX_CELL_TEMPERATURE_NUMBER(Doc.of(INTEGER)), //
		BMS_MIN_CELL_TEMPERATURE_NUMBER(Doc.of(INTEGER)), //
		BMS_CELL_TEMPERATURE_DELTA(Doc.of(INTEGER).unit(DEGREE_CELSIUS)), //
		BMS_CELL_VOLTAGE_DELTA(Doc.of(INTEGER).unit(MILLIVOLT)), //
		BMS_FAULT_WORD(Doc.of(INTEGER)), //
		BMS_WARNING_WORD(Doc.of(INTEGER)), //
		BMS_PROTECTED_WORD(Doc.of(INTEGER)), //
		BMS_SYSTEM_STATUS_HIGH(Doc.of(INTEGER)), //
		BMS_SYSTEM_STATUS_LOW(Doc.of(INTEGER)), //

		SYSTEM_SHUTDOWN(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		GRID_MODE_SWITCH(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		WORKING_MODE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		REMOTE_ACTIVE_POWER_SETPOINT(Doc.of(INTEGER).unit(WATT).accessMode(READ_WRITE)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
