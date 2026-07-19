package io.openems.edge.ziot.generic;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.DEGREE_CELSIUS;
import static io.openems.common.channel.Unit.MILLIVOLT;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT;
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
		TMP_CAB(Doc.of(FLOAT).unit(DEGREE_CELSIUS)), //
		OPERATING_STATUS(Doc.of(INTEGER)), //
		RESTART_DEVICE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		POWER_ON_OFF(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		SET_ACTIVE_POWER_LIMIT(Doc.of(INTEGER).unit(WATT).accessMode(READ_WRITE)), //
		SET_ACTIVE_POWER_LIMIT_PERCENT(Doc.of(INTEGER).unit(PERCENT).accessMode(READ_WRITE)), //
		ACTIVE_POWER_LIMIT_ENABLE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		ACTIVE_POWER_LIMIT_REVERT_TIME(Doc.of(INTEGER).unit(SECONDS)), //
		P_LIMIT_TYPE(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		WATCH_DOG_TAG(Doc.of(INTEGER).accessMode(READ_WRITE)), //
		VOLTAGE_L1_L2(Doc.of(INTEGER).unit(MILLIVOLT)), //
		VOLTAGE_L2_L3(Doc.of(INTEGER).unit(MILLIVOLT)), //
		VOLTAGE_L3_L1(Doc.of(INTEGER).unit(MILLIVOLT)), //
		PV1_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV2_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV3_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV4_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV5_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV6_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV7_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV8_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV9_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV10_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV11_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV12_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV13_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV14_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV15_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV16_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV17_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV18_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV19_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV20_VOLTAGE(Doc.of(FLOAT).unit(VOLT)), //
		PV1_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV2_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV3_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV4_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV5_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV6_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV7_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV8_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV9_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV10_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV11_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV12_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV13_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV14_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV15_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV16_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV17_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV18_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV19_CURRENT(Doc.of(FLOAT).unit(AMPERE)), //
		PV20_CURRENT(Doc.of(FLOAT).unit(AMPERE));

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
