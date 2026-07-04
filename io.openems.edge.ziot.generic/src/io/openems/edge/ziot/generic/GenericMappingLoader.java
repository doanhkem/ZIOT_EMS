package io.openems.edge.ziot.generic;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;

final class GenericMappingLoader {

	private GenericMappingLoader() {
	}

	static GenericMapping load(String file, String model) throws OpenemsException {
		try (var reader = new FileReader(Path.of(file).toFile())) {
			var root = JsonParser.parseReader(reader).getAsJsonObject();
			var modelObject = findModel(root, model);
			if (modelObject == null) {
				throw new OpenemsException("Model not found in mapping file: " + model);
			}
			var tasks = modelObject.getAsJsonObject("tasks");
			if (tasks == null) {
				throw new OpenemsException("Model has no tasks object: " + model);
			}
			var result = new GenericMapping();
			readRegisters(tasks, "read_registers", result.readRegisters);
			readRegisters(tasks, "read_input_registers", result.readInputRegisters);
			readRegisters(tasks, "watch_events", result.watchEvents);
			readRegisters(tasks, "write_registers", result.writeRegisters);
			return result;
		} catch (IOException | IllegalStateException e) {
			throw new OpenemsException("Unable to load mapping file [" + file + "] model [" + model + "]: "
					+ e.getMessage());
		}
	}

	private static JsonObject findModel(JsonObject root, String model) {
		var modbusTcp = root.getAsJsonObject("modbustcp");
		if (modbusTcp != null && modbusTcp.has(model)) {
			return modbusTcp.getAsJsonObject(model);
		}
		if (root.has(model)) {
			return root.getAsJsonObject(model);
		}
		return null;
	}

	private static void readRegisters(JsonObject tasks, String taskName, java.util.List<GenericMapping.Register> target) {
		var element = tasks.get(taskName);
		if (element == null || element.isJsonNull()) {
			return;
		}
		for (var item : element.getAsJsonArray()) {
			target.add(readRegister(item));
		}
	}

	private static GenericMapping.Register readRegister(JsonElement element) {
		var object = element.getAsJsonObject();
		return new GenericMapping.Register(//
				stringOrNull(object, "tagName"), //
				intOrNull(object, "offSet"), //
				stringOrNull(object, "dataType"), //
				intOrNull(object, "PF"), //
				intOrNull(object, "size"));
	}

	private static String stringOrNull(JsonObject object, String name) {
		var value = object.get(name);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static Integer intOrNull(JsonObject object, String name) {
		var value = object.get(name);
		return value == null || value.isJsonNull() ? null : value.getAsInt();
	}
}
