package io.openems.common.jsonrpc.request;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Triggers a device restart by writing a value to the device's RestartDevice
 * channel.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "restartDevice",
 *   "params": {
 *     "componentId": string,
 *     "value"?: number
 *   }
 * }
 * </pre>
 *
 * or for multiple devices:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "restartDevice",
 *   "params": {
 *     "componentIds": [string],
 *     "value"?: number
 *   }
 * }
 * </pre>
 */
public class RestartDeviceRequest extends JsonrpcRequest {

	public static final String METHOD = "restartDevice";
	public static final String CHANNEL_ID = "RestartDevice";
	public static final int DEFAULT_VALUE = 1;

	public static RestartDeviceRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentIds = new ArrayList<String>();

		var componentIdsOpt = JsonUtils.getAsOptionalJsonArray(p, "componentIds");
		if (componentIdsOpt.isPresent()) {
			for (JsonElement element : componentIdsOpt.get()) {
				componentIds.add(JsonUtils.getAsString(element));
			}
		} else {
			componentIds.add(JsonUtils.getAsString(p, "componentId"));
		}

		if (componentIds.isEmpty()) {
			throw new OpenemsException("Missing componentId/componentIds for restartDevice");
		}

		var value = JsonUtils.getAsOptionalInt(p, "value").orElse(DEFAULT_VALUE);
		return new RestartDeviceRequest(r, componentIds, value);
	}

	private final List<String> componentIds;
	private final int value;

	public RestartDeviceRequest(String componentId) {
		this(List.of(componentId), DEFAULT_VALUE);
	}

	public RestartDeviceRequest(List<String> componentIds, int value) {
		super(METHOD);
		this.componentIds = List.copyOf(componentIds);
		this.value = value;
	}

	private RestartDeviceRequest(JsonrpcRequest request, List<String> componentIds, int value) {
		super(request, METHOD);
		this.componentIds = List.copyOf(componentIds);
		this.value = value;
	}

	@Override
	public JsonObject getParams() {
		var componentIds = new JsonArray();
		this.componentIds.forEach(componentIds::add);
		return JsonUtils.buildJsonObject() //
				.add("componentIds", componentIds) //
				.addProperty("value", this.value) //
				.build();
	}

	public List<String> getComponentIds() {
		return this.componentIds;
	}

	public int getValue() {
		return this.value;
	}
}
