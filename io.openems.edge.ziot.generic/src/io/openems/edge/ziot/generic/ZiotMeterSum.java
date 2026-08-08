package io.openems.edge.ziot.generic;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.meter.api.ElectricityMeter;

public interface ZiotMeterSum extends ElectricityMeter, OpenemsComponent, ModbusSlave, SumOptions {

	public String[] getMeterIds();
}
