package io.openems.edge.pvinverter.aiswei.asw150klt;

import io.openems.common.types.OptionsEnum;

public enum PowerOnOff implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	POWER_OFF(0, "Power off"), //
	POWER_ON(1, "Power on"), //
	DRM_S9_OPEN(2, "DRM S9 open"), //
	DRM_S0_CLOSE(3, "DRM S0 close"), //
	ANTI_BACKFLOW_COMMUNICATION_INTERRUPTION(4, "Anti-backflow communication interruption"), //
	DEFAULT(170, "Default");

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
