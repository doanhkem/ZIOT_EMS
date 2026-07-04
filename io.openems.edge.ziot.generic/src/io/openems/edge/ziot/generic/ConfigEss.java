package io.openems.edge.ziot.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ZIOT Generic ESS", //
		description = "Generic mapping-based Modbus ESS/BESS device.")
public @interface ConfigEss {

	@AttributeDefinition(name = "Component-ID")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias")
	String alias() default "ZIOT Generic ESS";

	@AttributeDefinition(name = "Is enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Mapping file")
	String mappingFile() default "outputs/deviceConfig_openems_fields.conf";

	@AttributeDefinition(name = "Model")
	Model model() default Model.ESS_OMNI261;

	@AttributeDefinition(name = "Read only")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Capacity [Wh]")
	int capacity() default 0;

	@AttributeDefinition(name = "Max apparent power [VA]")
	int maxApparentPower() default 0;

	@AttributeDefinition(name = "Max charge power [W]")
	int maxChargePower() default 0;

	@AttributeDefinition(name = "Max discharge power [W]")
	int maxDischargePower() default 0;

	String webconsole_configurationFactory_nameHint() default "ZIOT Generic ESS [{id}]";

	enum Model {
		ESS_OMNI261("MODEL/Ess.Omni261"), //
		ESS_OMNI430PCS("MODEL/Ess.Omni430Pcs"), //
		ESS_SMA_STP_SE_BATTERY("MODEL/Ess.SMA.StpSe.Battery"), //
		ESS_SMA_STP_SE_DC_CHARGER("MODEL/Ess.SMA.StpSe.DcCharger"), //
		ESS_SMA_STP_SE_INVERTER("MODEL/Ess.SMA.StpSe.Inverter"), //
		ESS_SMA_SUNNY_ISLAND("MODEL/Ess.SMA.SunnyIsland");

		private final String key;

		private Model(String key) {
			this.key = key;
		}

		String key() {
			return this.key;
		}
	}
}
