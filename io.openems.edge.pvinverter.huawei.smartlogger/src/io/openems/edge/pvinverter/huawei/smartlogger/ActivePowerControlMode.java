package io.openems.edge.pvinverter.huawei.smartlogger;

import io.openems.common.types.OptionsEnum;

public enum ActivePowerControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_LIMIT(0, "No limit"), //
	DI_ACTIVE_SCHEDULING(1, "DI active scheduling"), //
	PERCENTAGE_FIXED_VALUE_LIMITATION(3, "Percentage fixed-value limitation"), //
	REMOTE_SCHEDULING(4, "Remote scheduling"), //
	EXPORT_LIMITATION_KW(6, "Export limitation kW"), //
	REMOTE_OUTPUT_CONTROL(200, "Remote output control"), //
	SLAVE_SMARTLOGGER(65533, "Slave SmartLogger"), //
	NO_SCHEDULING(65534, "No scheduling");

	private final int value;
	private final String name;

	private ActivePowerControlMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
