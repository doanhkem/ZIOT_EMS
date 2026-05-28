package io.openems.edge.pvinverter.aiswei.asw150klt;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.aiswei.asw150klt.PvInverterAisweiAsw150kLt.ChannelId;

public class SetActivePowerLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private final Logger log = LoggerFactory.getLogger(SetActivePowerLimitHandler.class);
	private final PvInverterAisweiAsw150kLtImpl parent;
	private final ManagedSymmetricPvInverter.ChannelId channelId;

	private Integer lastPowerLimit = null;

	public SetActivePowerLimitHandler(PvInverterAisweiAsw150kLtImpl parent,
			ManagedSymmetricPvInverter.ChannelId activePowerLimit) {
		this.parent = parent;
		this.channelId = activePowerLimit;
	}

	@Override
	public void run() throws OpenemsNamedException {
		IntegerWriteChannel channel = this.parent.channel(this.channelId);
		var powerOpt = channel.getNextWriteValueAndReset();

		int power = powerOpt.orElse(this.parent.config.maxActivePower());
		if (power < 0) {
			power = 0;
		}
		if (power > this.parent.config.maxActivePower()) {
			power = this.parent.config.maxActivePower();
		}

		if (Objects.equals(this.lastPowerLimit, power)) {
			return;
		}

		this.parent.logInfo(this.log, "Apply new limit: " + power + " W");

		EnumWriteChannel activePowerControlChannel = this.parent.channel(ChannelId.ACTIVE_POWER_CONTROL);
		activePowerControlChannel.setNextWriteValue(ActivePowerControl.ENABLE);

		EnumWriteChannel prefForInjectionChannel = this.parent.channel(ChannelId.PREF_FOR_INJECTION);
		prefForInjectionChannel.setNextWriteValue(PrefForInjection.FIXED_VALUE);

		IntegerWriteChannel activePowerLimitFixedChannel = this.parent.channel(ChannelId.ACTIVE_POWER_LIMIT_FIXED);
		activePowerLimitFixedChannel.setNextWriteValue(power);

		this.lastPowerLimit = power;
	}
}
