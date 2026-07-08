package io.openems.edge.ziot.generic;

import static io.openems.common.channel.AccessMode.READ_WRITE;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface ZiotGenericSensor extends ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DAILY_IRRADIATION(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Daily irradiation [kWh/m2]")), //
		TOTAL_IRRADIANCE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Total irradiance [W/m2]")), //
		RESTART_DEVICE(Doc.of(OpenemsType.INTEGER).accessMode(READ_WRITE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default Channel<Double> getDailyIrradiationChannel() {
		return this.channel(ChannelId.DAILY_IRRADIATION);
	}

	public default Value<Double> getDailyIrradiation() {
		return this.getDailyIrradiationChannel().value();
	}

	public default Channel<Double> getTotalIrradianceChannel() {
		return this.channel(ChannelId.TOTAL_IRRADIANCE);
	}

	public default Value<Double> getTotalIrradiance() {
		return this.getTotalIrradianceChannel().value();
	}
}
