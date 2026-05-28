package io.openems.edge.pvinverter.aiswei.asw150klt;

import io.openems.common.types.OptionsEnum;

public enum PrefForInjection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PERCENT_MAX_OUTPUT(0, "Percentage of max output active power for grid port"), //
	PERCENT_NOMINAL_OUTPUT(1, "Percentage of nominal output active power"), //
	PERCENT_INSTANTANEOUS_POWER(2, "Percentage of instantaneous power PM"), //
	FIXED_VALUE(3, "Fixed value setting");

	private final int value;
	private final String name;

	private PrefForInjection(int value, String name) {
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
