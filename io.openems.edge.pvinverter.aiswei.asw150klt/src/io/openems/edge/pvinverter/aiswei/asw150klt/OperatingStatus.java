package io.openems.edge.pvinverter.aiswei.asw150klt;

import io.openems.common.types.OptionsEnum;

public enum OperatingStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	WAIT(0, "Wait"), //
	NORMAL(1, "Normal"), //
	FAULT(2, "Fault"), //
	CHECKING(4, "Checking"), //
	INSUFFICIENT_LIGHT(5, "Insufficient light"), //
	SHUTDOWN(99, "Shutdown");

	private final int value;
	private final String name;

	private OperatingStatus(int value, String name) {
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
