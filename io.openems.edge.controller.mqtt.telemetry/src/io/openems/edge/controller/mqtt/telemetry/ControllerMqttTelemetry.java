package io.openems.edge.controller.mqtt.telemetry;

import org.osgi.service.event.EventHandler;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerMqttTelemetry extends Controller, OpenemsComponent, EventHandler, MqttComponent {
}
