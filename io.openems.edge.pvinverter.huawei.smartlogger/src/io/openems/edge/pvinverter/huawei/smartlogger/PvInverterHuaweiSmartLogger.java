package io.openems.edge.pvinverter.huawei.smartlogger;

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

public interface PvInverterHuaweiSmartLogger
		extends ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		INPUT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PLANT_STATUS(Doc.of(PlantStatus.values())), //
		ACTIVE_POWER_CONTROL_MODE(Doc.of(ActivePowerControlMode.values())), //
		ACTIVE_POWER_SCHEDULING_TARGET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		ACTIVE_SCHEDULING_PERCENTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		ACTIVE_POWER_ADJUSTMENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.WATT)), //
		ACTIVE_POWER_ADJUSTMENT_PERCENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT)), //
		POWER_ON_OFF(Doc.of(PowerOnOff.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		MAX_ACTIVE_ADJUSTMENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
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
