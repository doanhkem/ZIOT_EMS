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
		OPERATING_STATUS(Doc.of(OpenemsType.INTEGER)), //
		PV1_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV2_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV3_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV4_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV5_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV6_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV7_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV8_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV9_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV10_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV11_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV12_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV13_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV14_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV15_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV16_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV17_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV18_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV19_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV20_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
		PV1_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV2_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV3_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV4_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV5_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV6_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV7_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV8_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV9_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV10_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV11_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV12_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV13_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV14_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV15_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV16_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV17_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV18_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV19_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)), //
		PV20_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		@Override
		public String id() {
			final var id = io.openems.edge.common.channel.ChannelId.channelIdUpperToCamel(this.name());
			return id.startsWith("Pv") ? "PV" + id.substring(2) : id;
		}
	}
}
