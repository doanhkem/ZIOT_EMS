package io.openems.edge.meter.selec.mfm383;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterSelecMfm383 extends ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_L12(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		VOLTAGE_L23(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		VOLTAGE_L31(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		VOLTAGE_AVG_LN(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		VOLTAGE_AVG_LL(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		CURRENT_AVG(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE)), //
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)), //
		POWER_FACTOR_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)), //
		POWER_FACTOR_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)), //
		POWER_FACTOR_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)), //
		STATUS_REGISTER(Doc.of(OpenemsType.INTEGER)); //

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
