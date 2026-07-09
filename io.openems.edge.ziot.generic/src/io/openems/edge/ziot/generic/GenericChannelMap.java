package io.openems.edge.ziot.generic;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

final class GenericChannelMap {

	private GenericChannelMap() {
	}

	static Map<String, ChannelId> meter() {
		var result = commonMeter();
		add(result, ZiotGenericMeter.ChannelId.values());
		alias(result, "ErrorCode1", ZiotGenericMeter.ChannelId.FAULT_CODE_1);
		alias(result, "ErrorCode2", ZiotGenericMeter.ChannelId.FAULT_CODE_2);
		alias(result, "ErrorCode3", ZiotGenericMeter.ChannelId.FAULT_CODE_3);
		alias(result, "SetCtRatio", ZiotGenericMeter.ChannelId.SET_CT_RATIO);
		alias(result, "SetPtRatio", ZiotGenericMeter.ChannelId.SET_PT_RATIO);
		alias(result, "VoltageL1L2", ZiotGenericMeter.ChannelId.VOLTAGE_L1_L2);
		alias(result, "VoltageL2L3", ZiotGenericMeter.ChannelId.VOLTAGE_L2_L3);
		alias(result, "VoltageL3L1", ZiotGenericMeter.ChannelId.VOLTAGE_L3_L1);
		return result;
	}

	static Map<String, ChannelId> pvInverter() {
		var result = commonMeter();
		add(result, ManagedSymmetricPvInverter.ChannelId.values());
		add(result, ZiotGenericPvInverter.ChannelId.values());
		alias(result, "ErrorCode1", ZiotGenericPvInverter.ChannelId.FAULT_CODE_1);
		alias(result, "ErrorCode2", ZiotGenericPvInverter.ChannelId.FAULT_CODE_2);
		alias(result, "ErrorCode3", ZiotGenericPvInverter.ChannelId.FAULT_CODE_3);
		alias(result, "VoltageL1L2", ZiotGenericPvInverter.ChannelId.VOLTAGE_L1_L2);
		alias(result, "VoltageL2L3", ZiotGenericPvInverter.ChannelId.VOLTAGE_L2_L3);
		alias(result, "VoltageL3L1", ZiotGenericPvInverter.ChannelId.VOLTAGE_L3_L1);
		alias(result, "SetActivePowerLimit", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT);
		alias(result, "SetActivePowerLimitKw", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT);
		alias(result, "ActivePowerLimitKw", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT);
		alias(result, "ActivePowerAdjustment", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT);
		alias(result, "ActivePowerLimitFixed", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT);
		alias(result, "ActivePowerAdjustmentPercent", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT);
		alias(result, "PLimitPercent", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT);
		alias(result, "ActivePowerLimitPercent", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT);
		alias(result, "SetActivePowerLimitPercent", ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT);
		return result;
	}

	static Map<String, ChannelId> ess() {
		var result = new HashMap<String, ChannelId>();
		add(result, SymmetricEss.ChannelId.values());
		add(result, ManagedSymmetricEss.ChannelId.values());
		add(result, ZiotGenericEss.ChannelId.values());
		alias(result, "State", ZiotGenericEss.ChannelId.DEVICE_STATE);
		alias(result, "ErrorCode1", ZiotGenericEss.ChannelId.FAULT_CODE_1);
		alias(result, "ErrorCode2", ZiotGenericEss.ChannelId.FAULT_CODE_2);
		alias(result, "ErrorCode3", ZiotGenericEss.ChannelId.FAULT_CODE_3);
		alias(result, "SetControlMode", ZiotGenericEss.ChannelId.SET_CONTROL_MODE);
		alias(result, "SetReactivePower", ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS);
		alias(result, "RemoteActivePowerSetpointKw", ZiotGenericEss.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT);
		alias(result, "SetActivePowerKw", ZiotGenericEss.ChannelId.SET_ACTIVE_POWER);
		alias(result, "SetActivePowerPercent", ZiotGenericEss.ChannelId.SET_ACTIVE_POWER_PERCENT);
		alias(result, "RemoteActivePowerSetpointPercent", ZiotGenericEss.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT_PERCENT);
		return result;
	}

	static Map<String, ChannelId> sensor() {
		var result = new HashMap<String, ChannelId>();
		add(result, ZiotGenericSensor.ChannelId.values());
		return result;
	}

	private static Map<String, ChannelId> commonMeter() {
		var result = new HashMap<String, ChannelId>();
		add(result, ElectricityMeter.ChannelId.values());
		return result;
	}

	private static void add(Map<String, ChannelId> target, ChannelId[] values) {
		for (var channelId : values) {
			target.put(channelId.id(), channelId);
		}
	}

	private static void alias(Map<String, ChannelId> target, String tagName, ChannelId channelId) {
		target.put(tagName, channelId);
	}
}
