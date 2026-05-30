package io.openems.edge.sma.pvinverter.core2;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface PvInverterSmaCore2 extends ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING)), //
		MODEL(Doc.of(OpenemsType.STRING)), //
		MANUFACTURER(Doc.of(OpenemsType.STRING)), //
		VERSION(Doc.of(OpenemsType.STRING)), //
		DEVICE_ADDRESS(Doc.of(OpenemsType.INTEGER)), //
		DC_WATTS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DC_VOLT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		DC_AMPS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		POWER_FACTOR(Doc.of(OpenemsType.FLOAT)), //
		TMP_CAB(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		OPERATING_STATUS(Doc.of(OpenemsType.INTEGER));

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
