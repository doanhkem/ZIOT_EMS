package io.openems.edge.ziot.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "ZIOT Meter Sum", //
		description = "Virtual meter that sums multiple meter components.")
@interface ConfigMeterSum {

	@AttributeDefinition(name = "Component-ID")
	String id() default "meterSum0";

	@AttributeDefinition(name = "Alias")
	String alias() default "ZIOT Meter Sum";

	@AttributeDefinition(name = "Is enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Add to Sum?", description = "Usually false to avoid double-counting meters in _sum.")
	boolean addToSum() default false;

	@AttributeDefinition(name = "Meter IDs", description = "IDs of meter components to aggregate.")
	String[] meter_ids() default { "meter0", "meter1" };

	String webconsole_configurationFactory_nameHint() default "ZIOT Meter Sum [{id}]";
}
