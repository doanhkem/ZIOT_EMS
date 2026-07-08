package io.openems.edge.ziot.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractMultipleWordsElement;
import io.openems.edge.bridge.modbus.api.element.AbstractSingleWordElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
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
		addReadTasks(tasks, mapping, mapping.readRegisters, channels, mapper, false);
		addReadTasks(tasks, mapping, mapping.readInputRegisters, channels, mapper, true);
		addReadTasks(tasks, mapping, mapping.watchEvents, channels, mapper, false);
		addWriteTasks(tasks, mapping, mapping.writeRegisters, channels, mapper);
		return new ModbusProtocol(component, tasks.toArray(Task[]::new));
	}

	private static void addReadTasks(List<Task> tasks, GenericMapping mapping, List<GenericMapping.Register> registers,
			Map<String, ChannelId> channels, Mapper mapper, boolean inputRegisters) {
		for (var register : registers) {
			var channel = channels.get(register.tagName);
			if (channel == null || !register.isMapped()) {
				continue;
			}
			var element = element(mapping, register);
			var converter = converter(register, channel);
			var mapped = mapper.map(channel, element, converter);
			tasks.add(inputRegisters ? new FC4ReadInputRegistersTask(register.offset, Priority.HIGH, mapped)
					: new FC3ReadRegistersTask(register.offset, Priority.HIGH, mapped));
		}
	}

	private static void addWriteTasks(List<Task> tasks, GenericMapping mapping, List<GenericMapping.Register> registers,
			Map<String, ChannelId> channels, Mapper mapper) {
		for (var register : registers) {
			var channel = channels.get(register.tagName);
			if (channel == null || !register.isMapped()) {
				continue;
			}
			var element = element(mapping, register);
			var converter = converter(register, channel);
			var mapped = mapper.map(channel, element, converter);
			if (register.size.intValue() == 1 && mapped instanceof AbstractSingleWordElement<?, ?> singleWordElement) {
				tasks.add(new FC6WriteRegisterTask(register.offset, singleWordElement));
			} else {
				tasks.add(new FC16WriteRegistersTask(register.offset, mapped));
			}
		}
	}

	private static ModbusElement element(GenericMapping mapping, GenericMapping.Register register) {
		var element = switch (register.dataType) {
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
		applyDataFormat(element, mapping);
		return element;
	}

	private static void applyDataFormat(ModbusElement element, GenericMapping mapping) {
		if (element instanceof ModbusRegisterElement<?, ?> registerElement) {
			registerElement.byteOrder(mapping.byteOrder);
		}
		if (element instanceof AbstractMultipleWordsElement<?, ?> multipleWordsElement) {
			multipleWordsElement.wordOrder(mapping.wordOrder);
		}
	}

	private static ElementToChannelConverter converter(GenericMapping.Register register, ChannelId channel) {
		var factor = Math.pow(10, register.scaleFactor == null ? 0 : register.scaleFactor.intValue());
		factor *= unitFactor(register.unit, channel.doc().getUnit());
		if (Double.compare(factor, 1.0) == 0) {
			return ElementToChannelConverter.DIRECT_1_TO_1;
		}
		if (channel.doc().getType() == OpenemsType.FLOAT || channel.doc().getType() == OpenemsType.DOUBLE) {
			return floatingPointConverter(factor);
		}
		return ElementToChannelConverter.MULTIPLY(factor);
	}

	private static ElementToChannelConverter floatingPointConverter(double factor) {
		return new ElementToChannelConverter(value -> multiplyFloatingPoint(value, factor),
				value -> multiplyFloatingPoint(value, 1 / factor));
	}

	private static Object multiplyFloatingPoint(Object value, double factor) {
		return switch (value) {
		case null -> null;
		case Boolean b -> b;
		case Number n -> Double.valueOf(n.doubleValue() * factor);
		case String s -> s;
		default -> value;
		};
	}

	private static double unitFactor(String registerUnitSymbol, Unit channelUnit) {
		if (registerUnitSymbol == null || registerUnitSymbol.isBlank() || channelUnit == null
				|| channelUnit == Unit.NONE) {
			return 1.0;
		}
		var registerUnit = unitFromSymbol(registerUnitSymbol);
		if (registerUnit == Unit.NONE) {
			return 1.0;
		}
		var registerDiscrete = discreteUnit(registerUnit);
		var channelDiscrete = discreteUnit(channelUnit);
		if (baseUnit(registerDiscrete) != baseUnit(channelDiscrete)) {
			return 1.0;
		}
		return Math.pow(10, scaleFactor(registerDiscrete) - scaleFactor(channelDiscrete));
	}

	private static Unit unitFromSymbol(String symbol) {
		var normalized = symbol.trim();
		var unit = Unit.fromSymbolOrElse(normalized, Unit.NONE);
		if (unit != Unit.NONE) {
			return unit;
		}
		return switch (normalized.toLowerCase(Locale.ROOT)) {
		case "c", "degc", "degreecelsius" -> Unit.DEGREE_CELSIUS;
		case "dc", "dezc", "decicelsius" -> Unit.DEZIDEGREE_CELSIUS;
		case "kvar" -> Unit.KILOVOLT_AMPERE_REACTIVE;
		case "kvarh" -> Unit.KILOVOLT_AMPERE_REACTIVE_HOURS;
		default -> Unit.NONE;
		};
	}

	private static Unit discreteUnit(Unit unit) {
		return unit.discreteUnit == null ? unit : unit.discreteUnit;
	}

	private static Unit baseUnit(Unit unit) {
		return unit.baseUnit == null ? unit : unit.baseUnit;
	}

	private static int scaleFactor(Unit unit) {
		return unit.baseUnit == null ? 0 : unit.scaleFactor;
	}
}
