package io.openems.edge.ziot.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractSingleWordElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.taskmanager.Priority;

final class GenericProtocolFactory {

	@FunctionalInterface
	interface Mapper {
		ModbusElement map(ChannelId channelId, ModbusElement element, ElementToChannelConverter converter);
	}

	private GenericProtocolFactory() {
	}

	static ModbusProtocol create(AbstractOpenemsModbusComponent component, GenericMapping mapping,
			Map<String, ChannelId> channels, Mapper mapper) {
		var tasks = new ArrayList<Task>();
		addReadTasks(tasks, mapping.readRegisters, channels, mapper, false);
		addReadTasks(tasks, mapping.readInputRegisters, channels, mapper, true);
		addReadTasks(tasks, mapping.watchEvents, channels, mapper, false);
		addWriteTasks(tasks, mapping.writeRegisters, channels, mapper);
		return new ModbusProtocol(component, tasks.toArray(Task[]::new));
	}

	private static void addReadTasks(List<Task> tasks, List<GenericMapping.Register> registers,
			Map<String, ChannelId> channels, Mapper mapper, boolean inputRegisters) {
		for (var register : registers) {
			var channel = channels.get(register.tagName);
			if (channel == null || !register.isMapped()) {
				continue;
			}
			var element = element(register);
			var converter = converter(register);
			var mapped = mapper.map(channel, element, converter);
			tasks.add(inputRegisters ? new FC4ReadInputRegistersTask(register.offset, Priority.HIGH, mapped)
					: new FC3ReadRegistersTask(register.offset, Priority.HIGH, mapped));
		}
	}

	private static void addWriteTasks(List<Task> tasks, List<GenericMapping.Register> registers,
			Map<String, ChannelId> channels, Mapper mapper) {
		for (var register : registers) {
			var channel = channels.get(register.tagName);
			if (channel == null || !register.isMapped() || register.size.intValue() != 1) {
				continue;
			}
			var element = element(register);
			var converter = converter(register);
			var mapped = mapper.map(channel, element, converter);
			if (mapped instanceof AbstractSingleWordElement<?, ?> singleWordElement) {
				tasks.add(new FC6WriteRegisterTask(register.offset, singleWordElement));
			}
		}
	}

	private static ModbusElement element(GenericMapping.Register register) {
		return switch (register.dataType) {
		case "int16" -> new SignedWordElement(register.offset);
		case "uint16" -> new UnsignedWordElement(register.offset);
		case "int32" -> new SignedDoublewordElement(register.offset);
		case "uint32" -> new UnsignedDoublewordElement(register.offset);
		case "int64" -> new SignedQuadruplewordElement(register.offset);
		case "uint64" -> new UnsignedQuadruplewordElement(register.offset);
		case "float32" -> new FloatDoublewordElement(register.offset);
		case "string" -> new StringWordElement(register.offset, register.size);
		default -> new UnsignedWordElement(register.offset);
		};
	}

	private static ElementToChannelConverter converter(GenericMapping.Register register) {
		if (register.scaleFactor == null || register.scaleFactor.intValue() == 0) {
			return ElementToChannelConverter.DIRECT_1_TO_1;
		}
		return ElementToChannelConverter.MULTIPLY(Math.pow(10, register.scaleFactor.intValue()));
	}
}
