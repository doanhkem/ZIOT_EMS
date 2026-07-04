package io.openems.edge.ziot.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ZIOT Generic Sensor", //
		description = "Generic mapping-based Modbus sensor.")
public @interface ConfigSensor {

	@AttributeDefinition(name = "Component-ID")
	String id() default "sensor0";

	@AttributeDefinition(name = "Alias")
	String alias() default "ZIOT Generic Sensor";

	@AttributeDefinition(name = "Is enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Mapping file")
	String mappingFile() default "outputs/deviceConfig_openems_fields.conf";

	@AttributeDefinition(name = "Model")
	Model model() default Model.SENSOR_HUAWEI_IRRADIANCE;

	String webconsole_configurationFactory_nameHint() default "ZIOT Generic Sensor [{id}]";

	enum Model {
		SENSOR_HUAWEI_IRRADIANCE("MODEL/Sensor.Huawei.Irradiance");

		private final String key;

		private Model(String key) {
			this.key = key;
		}

		String key() {
			return this.key;
		}
	}
}
