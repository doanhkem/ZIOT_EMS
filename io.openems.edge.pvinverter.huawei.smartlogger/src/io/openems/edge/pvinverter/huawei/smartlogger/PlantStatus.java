package io.openems.edge.pvinverter.huawei.smartlogger;

import io.openems.common.types.OptionsEnum;

public enum PlantStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNLIMITED_POWER_OPERATION(1, "Unlimited power operation"), //
	LIMITED_POWER_OPERATION(2, "Limited power operation"), //
	IDLE(3, "Idle"), //
	OUTAGE(4, "Outage"), //
	COMMUNICATION_INTERRUPT(5, "Communication interrupt");

	private final int value;
	private final String name;

	private PlantStatus(int value, String name) {
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
