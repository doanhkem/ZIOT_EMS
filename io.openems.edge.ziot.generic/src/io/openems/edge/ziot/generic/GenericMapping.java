package io.openems.edge.ziot.generic;

import java.util.ArrayList;
import java.util.List;

final class GenericMapping {

	final List<Register> readRegisters = new ArrayList<>();
	final List<Register> readInputRegisters = new ArrayList<>();
	final List<Register> watchEvents = new ArrayList<>();
	final List<Register> writeRegisters = new ArrayList<>();

	static final class Register {
		final String tagName;
		final Integer offset;
		final String dataType;
		final Integer scaleFactor;
		final Integer size;

		Register(String tagName, Integer offset, String dataType, Integer scaleFactor, Integer size) {
			this.tagName = tagName;
			this.offset = offset;
			this.dataType = dataType == null ? null : dataType.toLowerCase();
			this.scaleFactor = scaleFactor;
			this.size = size;
		}

		boolean isMapped() {
			return this.offset != null && this.dataType != null && this.size != null;
		}
	}
}
