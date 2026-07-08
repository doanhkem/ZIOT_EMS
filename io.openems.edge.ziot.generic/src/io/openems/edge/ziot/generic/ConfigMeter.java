package io.openems.edge.ziot.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "ZIOT Generic Meter", //
		description = "Generic mapping-based Modbus meter.")
public @interface ConfigMeter {

	@AttributeDefinition(name = "Component-ID")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias")
	String alias() default "ZIOT Generic Meter";

	@AttributeDefinition(name = "Is enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Invert active/reactive power")
	boolean invert() default false;

	@AttributeDefinition(name = "Use CT ratio")
	boolean useCtRatio() default false;

	@AttributeDefinition(name = "CT ratio")
	double ctRatio() default 1.0;

	@AttributeDefinition(name = "Use PT ratio")
	boolean usePtRatio() default false;

	@AttributeDefinition(name = "PT ratio")
	double ptRatio() default 1.0;

	@AttributeDefinition(name = "Modbus-ID")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Mapping file")
	String mappingFile() default "outputs/deviceConfig_openems_fields.conf";

	@AttributeDefinition(name = "Model")
	Model model() default Model.METER_SELEC_MFM383;

	String webconsole_configurationFactory_nameHint() default "ZIOT Generic Meter [{id}]";

	enum Model {
		METER_ACREL_KACCCC("MODEL/Meter.Acrel.kacccc"), //
		METER_ACREL_DTSD1352("MODEL/Meter.Acrel.DTSD1352"), //
		METER_CHINT_DDSU666("MODEL/Meter.Chint.DDSU666"), //
		METER_CHINT_DTSU666("MODEL/Meter.Chint.DTSU666"), //
		METER_EASTRON_SDM120("MODEL/Meter.Eastron.SDM120"), //
		METER_FRONIUS("MODEL/Meter.Fronius"), //
		METER_JANITZA_UMG104("MODEL/Meter.Janitza.UMG104"), //
		METER_JANITZA_UMG511("MODEL/Meter.Janitza.UMG511"), //
		METER_JANITZA_UMG604("MODEL/Meter.Janitza.UMG604"), //
		METER_JANITZA_UMG801("MODEL/Meter.Janitza.UMG801"), //
		METER_JANITZA_UMG806("MODEL/Meter.Janitza.UMG806"), //
		METER_JANITZA_UMG96RME("MODEL/Meter.Janitza.UMG96RME"), //
		METER_MICROCARE_SDM630("MODEL/Meter.Microcare.SDM630"), //
		METER_PHOENIX_CONTACT("MODEL/Meter.PhoenixContact"), //
		METER_SCHNEIDER_ACTI9_SMARTLINK("MODEL/Meter.Schneider.Acti9.Smartlink"), //
		METER_SELEC_CUSTOM("MODEL/Meter.Selec.Custom"), //
		METER_SELEC_MFM383("MODEL/Meter.Selec.MFM383"), //
		METER_SMA_SHM20("MODEL/Meter.SMA.SHM20");

		private final String key;

		private Model(String key) {
			this.key = key;
		}

		String key() {
			return this.key;
		}
	}
}
