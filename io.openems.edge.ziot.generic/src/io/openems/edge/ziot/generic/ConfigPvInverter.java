package io.openems.edge.ziot.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ZIOT Generic PV-Inverter", //
		description = "Generic mapping-based Modbus PV inverter.")
public @interface ConfigPvInverter {

	@AttributeDefinition(name = "Component-ID")
	String id() default "pvInverter0";

	@AttributeDefinition(name = "Alias")
	String alias() default "ZIOT Generic PV-Inverter";

	@AttributeDefinition(name = "Is enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Mapping file")
	String mappingFile() default "outputs/deviceConfig_openems_fields.conf";

	@AttributeDefinition(name = "Model")
	Model model() default Model.PV_INVERTER_SUNGROW_SG110CX;

	String webconsole_configurationFactory_nameHint() default "ZIOT Generic PV-Inverter [{id}]";

	enum Model {
		PV_INVERTER_AISWEI_ASW150K_LT("MODEL/PV-Inverter.AISWEI.ASW150K-LT"), //
		PV_INVERTER_FRONIUS("MODEL/PV-Inverter.Fronius"), //
		PV_INVERTER_HUAWEI_SMARTLOGGER("MODEL/PV-Inverter.Huawei.SmartLogger"), //
		PV_INVERTER_KACO_BLUEPLANET("MODEL/PV-Inverter.KACO.blueplanet"), //
		PV_INVERTER_KOSTAL("MODEL/PV-Inverter.Kostal"), //
		PV_INVERTER_KOSTAL_PIKO("MODEL/PV-Inverter.Kostal.Piko"), //
		PV_INVERTER_SMA_CORE2("MODEL/PV-Inverter.SMA.CORE2"), //
		PV_INVERTER_SMA_SUNNY_TRIPOWER("MODEL/PV-Inverter.SMA.SunnyTripower"), //
		PV_INVERTER_SOLARLOG("MODEL/PV-Inverter.Solarlog"), //
		PV_INVERTER_SUNGROW_SG110CX("MODEL/PV-Inverter.Sungrow.SG110CX");

		private final String key;

		private Model(String key) {
			this.key = key;
		}

		String key() {
			return this.key;
		}
	}
}
