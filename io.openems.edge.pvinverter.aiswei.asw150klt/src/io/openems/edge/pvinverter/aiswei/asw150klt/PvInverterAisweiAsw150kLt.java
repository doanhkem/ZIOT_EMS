package io.openems.edge.pvinverter.aiswei.asw150klt;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface PvInverterAisweiAsw150kLt
		extends ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING)), //
		MODEL(Doc.of(OpenemsType.STRING)), //
		MANUFACTURER(Doc.of(OpenemsType.STRING)), //
		PROTOCOL_VERSION(Doc.of(OpenemsType.STRING)), //
		DEVICE_ADDRESS(Doc.of(OpenemsType.INTEGER)), //
		RATED_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		NOMINAL_GRID_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		OPERATING_STATUS(Doc.of(OperatingStatus.values())), //
		PV1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		PV1_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		POWER_FACTOR(Doc.of(OpenemsType.FLOAT)), //
		INTERNAL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //

		ACTIVE_POWER_CONTROL(Doc.of(ActivePowerControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		PREF_FOR_INJECTION(Doc.of(PrefForInjection.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVE_POWER_LIMIT_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT)), //
		ACTIVE_POWER_LIMIT_FIXED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_INCREASE_GRADIENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVE_POWER_REDUCE_GRADIENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		POWER_ON_OFF(Doc.of(PowerOnOff.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Limit failed"));

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
