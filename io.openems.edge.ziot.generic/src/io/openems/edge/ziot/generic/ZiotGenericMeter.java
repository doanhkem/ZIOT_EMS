package io.openems.edge.ziot.generic;

import static io.openems.common.channel.Unit.THOUSANDTH;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface ZiotGenericMeter extends ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FAULT_CODE_1(Doc.of(INTEGER)), //
		FAULT_CODE_2(Doc.of(INTEGER)), //
		FAULT_CODE_3(Doc.of(INTEGER)), //
		POWER_FACTOR(Doc.of(FLOAT)), //
		VOLTAGE_L1_L2(Doc.of(INTEGER).unit(VOLT)), //
		VOLTAGE_L2_L3(Doc.of(INTEGER).unit(VOLT)), //
		VOLTAGE_L3_L1(Doc.of(INTEGER).unit(VOLT)), //
		POWER_FACTOR_RAW(Doc.of(INTEGER).unit(THOUSANDTH));

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
