package io.openems.edge.ziot.generic;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface ZiotGenericEss extends ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent,
		ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FAULT_CODE_1(Doc.of(INTEGER)), //
		FAULT_CODE_2(Doc.of(INTEGER)), //
		FAULT_CODE_3(Doc.of(INTEGER)), //
		SOH(Doc.of(INTEGER).unit(PERCENT)), //
		RESTART_DEVICE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		SYSTEM_SHUTDOWN(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		GRID_MODE_SWITCH(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		WORKING_MODE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		REMOTE_ACTIVE_POWER_SETPOINT(Doc.of(INTEGER).unit(WATT).accessMode(READ_WRITE)), //
		REMOTE_ACTIVE_POWER_SETPOINT_PERCENT(Doc.of(INTEGER).unit(PERCENT).accessMode(READ_WRITE)), //
		SET_ACTIVE_POWER(Doc.of(INTEGER).unit(WATT).accessMode(READ_WRITE)), //
		SET_ACTIVE_POWER_PERCENT(Doc.of(INTEGER).unit(PERCENT).accessMode(READ_WRITE));

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
