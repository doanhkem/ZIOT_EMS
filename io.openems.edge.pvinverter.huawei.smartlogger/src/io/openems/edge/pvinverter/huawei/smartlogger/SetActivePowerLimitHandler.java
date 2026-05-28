package io.openems.edge.pvinverter.huawei.smartlogger;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.huawei.smartlogger.PvInverterHuaweiSmartLogger.ChannelId;

public class SetActivePowerLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private final Logger log = LoggerFactory.getLogger(SetActivePowerLimitHandler.class);
	private final PvInverterHuaweiSmartLoggerImpl parent;
	private final ManagedSymmetricPvInverter.ChannelId channelId;

	private Integer lastPowerLimit = null;

	public SetActivePowerLimitHandler(PvInverterHuaweiSmartLoggerImpl parent,
			ManagedSymmetricPvInverter.ChannelId activePowerLimit) {
		this.parent = parent;
		this.channelId = activePowerLimit;
	}

	@Override
	public void run() throws OpenemsNamedException {
		IntegerWriteChannel channel = this.parent.channel(this.channelId);
		var powerOpt = channel.getNextWriteValueAndReset();

		int maxPower = this.parent.getMaxActiveAdjustment().orElse(this.parent.config.maxActivePower());
		int power = powerOpt.orElse(maxPower);
		if (power < 0) {
			power = 0;
		}
		if (power > maxPower) {
			power = maxPower;
		}

		if (Objects.equals(this.lastPowerLimit, power)) {
			return;
		}

		this.parent.logInfo(this.log, "Apply new SmartLogger active adjustment: " + power + " W");

		IntegerWriteChannel activePowerAdjustmentChannel = this.parent.channel(ChannelId.ACTIVE_POWER_ADJUSTMENT);
		activePowerAdjustmentChannel.setNextWriteValue(power);

		this.lastPowerLimit = power;
	}
}
