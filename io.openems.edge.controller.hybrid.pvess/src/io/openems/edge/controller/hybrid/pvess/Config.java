package io.openems.edge.controller.hybrid.pvess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.PropertyChannel;

@ObjectClassDefinition(//
		name = "Controller Hybrid PV ESS", //
		description = "Coordinates ESS and PV-Inverter for hybrid Zero Export and Peak Shaving.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlHybridPvEss0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Hybrid operating mode.")
	HybridMode mode() default HybridMode.ZERO_EXPORT;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Grid-Meter-ID", description = "ID of the Grid-Meter.")
	String meter_id() default "meter0";

	@AttributeDefinition(name = "PV-Inverter-ID", description = "ID of PV-Inverter device or PV-Inverter Cluster.")
	String pvInverter_id() default "pvInverter0";

	@AttributeDefinition(name = "Target Grid Setpoint [W]", description = "Positive for buy-from-grid; negative for sell-to-grid. Used in Zero Export mode.")
	@PropertyChannel(localPersistencePriority = PersistencePriority.HIGH, remotePersistencePriority = PersistencePriority.HIGH)
	int targetGridSetpoint() default 0;

	@AttributeDefinition(name = "Maximum allowed Sell-To-Grid power [W]", description = "Target export limit. Usually 0 W for Zero Export.")
	@PropertyChannel(localPersistencePriority = PersistencePriority.HIGH, remotePersistencePriority = PersistencePriority.HIGH)
	int maximumSellToGridPower() default 0;

	@AttributeDefinition(name = "Peak-Shaving power [W]", description = "Grid purchase power above this value is shaved by discharging ESS.")
	@PropertyChannel(localPersistencePriority = PersistencePriority.HIGH, remotePersistencePriority = PersistencePriority.HIGH)
	int peakShavingPower() default 0;

	@AttributeDefinition(name = "Recharge power [W]", description = "If uncompensated grid power is below this value the ESS is charged. Use 0 to charge only from surplus/export.")
	@PropertyChannel(localPersistencePriority = PersistencePriority.HIGH, remotePersistencePriority = PersistencePriority.HIGH)
	int rechargePower() default 0;

	@AttributeDefinition(name = "Max Charge Power [W]", description = "Positive value. 0 means use device limit.")
	int maxChargePower() default 0;

	@AttributeDefinition(name = "Max Discharge Power [W]", description = "Positive value. 0 means use device limit.")
	int maxDischargePower() default 0;

	@AttributeDefinition(name = "Fallback PV Release Power [W]", description = "Used as PV release limit if PV-Inverter MaxActivePower is unavailable. 0 means use current PV power.")
	int fallbackPvReleasePower() default 0;

	@AttributeDefinition(name = "Minimum SOC for discharge [%]", description = "ESS discharge is blocked at or below this SOC. 0 disables this guard.")
	int minSoc() default 0;

	@AttributeDefinition(name = "Maximum SOC for charge [%]", description = "ESS charge is blocked at or above this SOC. 100 disables this guard.")
	int maxSoc() default 100;

	@AttributeDefinition(name = "Enable PV curtailment", description = "If enabled, PV is curtailed only for export that remains after ESS handling.")
	boolean enablePvCurtailment() default true;

	@AttributeDefinition(name = "TOU Schedule JSON", description = "Array of max 20 non-overlapping slots. Fields: enabled, name, type, start, end, power, chargeSource, minSoc, maxSoc.")
	String touScheduleJson() default """
			[
			  {
			    "enabled": true,
			    "name": "Charge night",
			    "type": "CHARGE",
			    "start": "22:00",
			    "end": "04:00",
			    "chargeSource": "ANY",
			    "power": 100000,
			    "maxSoc": 95
			  },
			  {
			    "enabled": true,
			    "name": "Discharge morning",
			    "type": "DISCHARGE",
			    "start": "04:00",
			    "end": "08:00",
			    "power": 400000,
			    "minSoc": 20
			  }
			]""";

	String webconsole_configurationFactory_nameHint() default "Controller Hybrid PV ESS [{id}]";
}
