package io.openems.edge.ziot.generic;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.MILLIVOLT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;

import org.osgi.service.event.EventHandler;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface ZiotGenericPvInverter extends ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FAULT_CODE_1(Doc.of(INTEGER)), //
		FAULT_CODE_2(Doc.of(INTEGER)), //
		FAULT_CODE_3(Doc.of(INTEGER)), //
		POWER_FACTOR(Doc.of(FLOAT)), //
		TMP_CAB(Doc.of(INTEGER)), //
		OPERATING_STATUS(Doc.of(INTEGER)), //
		SET_ACTIVE_POWER_LIMIT(Doc.of(INTEGER).unit(WATT).accessMode(READ_WRITE)), //
		PV1_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV2_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV3_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV4_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV5_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV6_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV7_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV8_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV9_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV10_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV11_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV12_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV13_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV14_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV15_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV16_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV17_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV18_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV19_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV20_VOLTAGE(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV1_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV2_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV3_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV4_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV5_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV6_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV7_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV8_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV9_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV10_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV11_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV12_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV13_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV14_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV15_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV16_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV17_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV18_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV19_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE)), //
		PV20_CURRENT(Doc.of(INTEGER).unit(MILLIAMPERE));

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
