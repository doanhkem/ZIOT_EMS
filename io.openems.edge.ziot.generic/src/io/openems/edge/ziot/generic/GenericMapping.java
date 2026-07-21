package io.openems.edge.ziot.generic;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.bridge.modbus.api.element.WordOrder;

final class GenericMapping {

	final List<Register> readRegisters = new ArrayList<>();
	final List<Register> readInputRegisters = new ArrayList<>();
	final List<Register> watchEvents = new ArrayList<>();
	final List<Register> writeRegisters = new ArrayList<>();
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	WordOrder wordOrder = WordOrder.MSWLSW;
	String deviceType = "";

	static final class Register {
		final String tagName;
		final Integer offset;
		final String dataType;
		final Integer scaleFactor;
		final String unit;
		final Integer size;

		Register(String tagName, Integer offset, String dataType, Integer scaleFactor, String unit, Integer size) {
			this.tagName = tagName;
			this.offset = offset;
			this.dataType = dataType == null ? null : dataType.toLowerCase();
			this.scaleFactor = scaleFactor;
			this.unit = unit == null ? null : unit.trim();
			this.size = size;
		}

		boolean isMapped() {
			return this.offset != null && this.dataType != null && this.size != null;
		}
	}
}
