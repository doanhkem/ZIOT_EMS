package io.openems.edge.pvinverter.huawei.smartlogger;

import io.openems.common.types.OptionsEnum;

public enum PowerOnOff implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	POWER_OFF(0, "Power off all inverters"), //
	POWER_ON(1, "Power on all inverters");

	private final int value;
	private final String name;

	private PowerOnOff(int value, String name) {
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
