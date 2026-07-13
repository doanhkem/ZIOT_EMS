package io.openems.edge.controller.mqtt.telemetry;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller MQTT Telemetry", //
		description = "Publishes selected inverter values to MQTT using a compact telemetry JSON payload.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlMqttTelemetry0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "PV-Inverter-ID", description = "Component-ID of the PV inverter to publish.")
	String pvInverter_id() default "pvInverter0";

	@AttributeDefinition(name = "MQTT Topic", description = "Topic to publish the telemetry payload to.")
	String topic() default "openems/telemetry/pvInverter0";

	@AttributeDefinition(name = "Publish interval [s]", description = "Minimum interval between telemetry publishes.")
	int intervalSeconds() default 1;

	@AttributeDefinition(name = "Retained", description = "Whether the MQTT payload should be retained by the broker.")
	boolean retained() default false;

	@AttributeDefinition(name = "Device Address", description = "Static deviceAddress field in telemetry payload.")
	int deviceAddress() default 1;

	@AttributeDefinition(name = "Serial Number", description = "Static sn field in telemetry payload.")
	String serialNumber() default "";

	@AttributeDefinition(name = "Version", description = "Static version field in telemetry payload.")
	String version() default "";

	@AttributeDefinition(name = "Model", description = "Static model field in telemetry payload.")
	String model() default "";

	@AttributeDefinition(name = "Manufacturer", description = "Static manufacturer field in telemetry payload.")
	String manufacturer() default "";

	@AttributeDefinition(name = "Voltage Reference [V]", description = "Static VoltRef field in telemetry payload.")
	double voltRef() default 415.0;

	@AttributeDefinition(name = "Nameplate Watts [W]", description = "Fallback value for WattsMax and nameplateWatts.")
	int nameplateWatts() default 0;

	String webconsole_configurationFactory_nameHint() default "Controller MQTT Telemetry [{id}]";
}
