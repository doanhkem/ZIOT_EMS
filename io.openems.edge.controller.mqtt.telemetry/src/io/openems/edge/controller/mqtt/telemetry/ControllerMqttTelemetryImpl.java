package io.openems.edge.controller.mqtt.telemetry;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S1;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S120;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S121;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S103;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.MQTT.Telemetry", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerMqttTelemetryImpl extends AbstractOpenemsComponent
		implements ControllerMqttTelemetry, Controller, OpenemsComponent, MqttComponent {

	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

	private final Logger log = LoggerFactory.getLogger(ControllerMqttTelemetryImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY, //
			target = "(enabled=true)")
	private BridgeMqtt mqttBridge;

	private Config config;
	private long lastPublishMillis = 0;

	public ControllerMqttTelemetryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.applyConfig(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private void applyConfig(Config config) {
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled()) {
			return;
		}

		var intervalMillis = Math.max(1, this.config.intervalSeconds()) * 1000L;
		var now = System.currentTimeMillis();
		if (now - this.lastPublishMillis < intervalMillis) {
			return;
		}
		this.lastPublishMillis = now;

		try {
			var payload = this.buildPayload();
			this.mqttBridge.publish(this.config.topic(), payload.toString(), QoS.AT_LEAST_ONCE, this.config.retained())
					.get(5, TimeUnit.SECONDS);
			this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(false);
		} catch (Exception e) {
			this.logWarn(this.log, "Unable to publish MQTT telemetry: " + e.getMessage());
			this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(true);
		}
	}

	private JsonObject buildPayload() throws OpenemsNamedException {
		var component = this.componentManager.getComponent(this.config.pvInverter_id());
		if (isStandaloneMeter(component)) {
			return this.buildMeterPayload(component);
		}

		var data = new JsonArray();

		add(data, "deviceAddress", firstNumber(component, 1.0, "DeviceAddress")
				.or(() -> firstSunSpecNumber(component, 1.0, S1.DA)).orElse((double) this.config.deviceAddress()), null);
		add(data, "sn",
				firstString(component, "SerialNumber").or(() -> firstSunSpecString(component, S1.SN))
						.orElse(emptyToNull(this.config.serialNumber())),
				null);
		add(data, "version",
				firstString(component, "ProtocolVersion", "Version").or(() -> firstSunSpecString(component, S1.VR))
						.orElse(emptyToNull(this.config.version())),
				null);
		add(data, "model",
				firstString(component, "Model").or(() -> firstSunSpecString(component, S1.MD))
						.orElse(emptyToNull(this.config.model())),
				null);
		add(data, "manufacturer",
				firstString(component, "Manufacturer").or(() -> firstSunSpecString(component, S1.MN))
						.orElse(emptyToNull(this.config.manufacturer())),
				null);
		add(data, "VoltRef", firstNumber(component, 0.001, "NominalGridVoltage")
				.or(() -> firstSunSpecNumber(component, 1.0, S121.V_REF)).orElse((double) this.config.voltRef()),
				"V");

		var maxWatts = firstNumber(component, 1.0, "RatedPower", "MaxActivePower", "MaxApparentPower")
				.or(() -> firstSunSpecNumber(component, 1.0, S121.W_MAX, S120.W_RTG))
				.orElse((double) this.config.nameplateWatts());
		add(data, "WattsMax", maxWatts == 0 ? null : maxWatts, "W");
		add(data, "nameplateWatts", maxWatts == 0 ? null : maxWatts, "W");
		add(data, "operatingState", firstNumber(component, 1.0, "OperatingState", "OperatingStatus")
				.or(() -> firstSunSpecNumber(component, 1.0, S103.ST)).orElse(null), null);
		add(data, "tmpCab",
				firstNumber(component, 1.0, "TmpCab", "InternalTemperature", "CabinetTemperature")
						.or(() -> firstSunSpecNumber(component, 1.0, S103.TMP_CAB))
						.orElse(null),
				"C");
		add(data, "DCWatts", firstNumber(component, 1.0, "DCWatts", "DcWatts", "DcPower", "Pdc", "ActivePower")
				.or(() -> firstSunSpecNumber(component, 1.0, S103.DCW))
				.orElse(null), "W");
		add(data, "DCVolt",
				firstNumber(component, 0.001, "PV1Voltage").or(() -> firstNumber(component, 1.0, "DCVolt", "DcVolt", "DcVoltage", "Udc"))
						.or(() -> firstSunSpecNumber(component, 1.0, S103.DCV)).orElse(null),
				"V");
		add(data, "DCAmps", firstNumber(component, 0.001, "PV1Current").or(() -> firstNumber(component, 1.0, "DCAmps", "DcAmps", "DcCurrent"))
				.or(() -> firstSunSpecNumber(component, 1.0, S103.DCA)).orElse(null),
				"A");
		var wattHours = firstNumber(component, 1.0, "ActiveProductionEnergy").orElse(null);
		add(data, "WH", wattHours, "Wh");
		add(data, "PF",
				firstNumber(component, 1.0, "PF", "PowerFactor").or(() -> firstSunSpecNumber(component, 1.0, S103.PF))
						.orElse(null),
				"Pct");
		add(data, "VAr", firstNumber(component, 1.0, "ReactivePower").orElse(null), "VAr");
		add(data, "VA",
				firstNumber(component, 1.0, "ApparentPower").or(() -> firstSunSpecNumber(component, 1.0, S103.VA))
						.orElse(null),
				"VA");
		add(data, "Hz", firstNumber(component, 0.001, "Frequency").orElse(null), "Hz");
		var watts = firstNumber(component, 1.0, "ActivePower").orElse(null);
		add(data, "Watts", watts, "W");
		add(data, "VoltCN", firstNumber(component, 0.001, "VoltageL3").orElse(null), "V");
		add(data, "VoltBN", firstNumber(component, 0.001, "VoltageL2").orElse(null), "V");
		add(data, "VoltAN", firstNumber(component, 0.001, "VoltageL1").orElse(null), "V");
		add(data, "AmpsC", firstNumber(component, 0.001, "CurrentL3").orElse(null), "A");
		add(data, "AmpsB", firstNumber(component, 0.001, "CurrentL2").orElse(null), "A");
		add(data, "AmpsA", firstNumber(component, 0.001, "CurrentL1").orElse(null), "A");

		var payload = new JsonObject();
		addTopLevelNumber(payload, "kiloWatts", watts == null ? null : watts / 1000.0);
		addTopLevelNumber(payload, "kWH", wattHours == null ? null : wattHours / 1000.0);
		payload.add("data", data);
		payload.addProperty("timeStamp", this.utcTimestamp());
		return payload;
	}

	private JsonObject buildMeterPayload(OpenemsComponent component) {
		var data = new JsonArray();
		var kiloWatts = firstNumber(component, 0.001, "ActivePower").orElse(null);
		var kiloWattHours = firstNumber(component, 0.001, "ActiveProductionEnergy", "ActiveConsumptionEnergy").orElse(null);
		add(data, "kiloWatts", kiloWatts, "kW");
		add(data, "kWH", kiloWattHours, "kWh");

		var payload = new JsonObject();
		addTopLevelNumber(payload, "kiloWatts", kiloWatts);
		addTopLevelNumber(payload, "kWH", kiloWattHours);
		payload.add("data", data);
		payload.addProperty("timeStamp", this.utcTimestamp());
		return payload;
	}

	private String utcTimestamp() {
		return this.componentManager.getClock().instant().atZone(ZoneOffset.UTC).format(TIMESTAMP_FORMAT);
	}

	private static boolean isStandaloneMeter(OpenemsComponent component) {
		return component instanceof ElectricityMeter && !(component instanceof ManagedSymmetricPvInverter);
	}

	private static String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static void add(JsonArray data, String name, Object value, String unit) {
		var item = new JsonObject();
		item.addProperty("name", name);
		if (value == null) {
			item.add("value", JsonNull.INSTANCE);
		} else if (value instanceof Number number) {
			item.addProperty("value", normalize(number));
		} else if (value instanceof Boolean bool) {
			item.addProperty("value", bool);
		} else {
			item.addProperty("value", value.toString());
		}
		if (unit == null) {
			item.add("unit", JsonNull.INSTANCE);
		} else {
			item.addProperty("unit", unit);
		}
		data.add(item);
	}

	private static void addTopLevelNumber(JsonObject payload, String name, Number value) {
		if (value == null) {
			payload.add(name, JsonNull.INSTANCE);
		} else {
			payload.addProperty(name, normalize(value));
		}
	}

	private static Number normalize(Number value) {
		var doubleValue = value.doubleValue();
		if (Double.isFinite(doubleValue) && Math.rint(doubleValue) == doubleValue) {
			return Long.valueOf((long) doubleValue);
		}
		return Double.valueOf(Math.round(doubleValue * 1000.0) / 1000.0);
	}

	private static java.util.Optional<Double> firstNumber(OpenemsComponent component, double factor, String... channelIds) {
		for (var channelId : channelIds) {
			var value = getRawValue(component, channelId);
			if (value instanceof Number number) {
				return java.util.Optional.of(number.doubleValue() * factor);
			}
			if (value instanceof OptionsEnum option) {
				return java.util.Optional.of((double) option.getValue());
			}
		}
		return java.util.Optional.empty();
	}

	private static java.util.Optional<String> firstString(OpenemsComponent component, String... channelIds) {
		for (var channelId : channelIds) {
			var value = getRawValue(component, channelId);
			if (value instanceof String string) {
				var trimmed = cleanString(string);
				if (!trimmed.isEmpty()) {
					return java.util.Optional.of(trimmed);
				}
			}
		}
		return java.util.Optional.empty();
	}

	private static java.util.Optional<Double> firstSunSpecNumber(OpenemsComponent component, double factor,
			SunSpecPoint... points) {
		for (var point : points) {
			var value = getSunSpecRawValue(component, point);
			if (value instanceof Number number) {
				return java.util.Optional.of(number.doubleValue() * factor);
			}
			if (value instanceof OptionsEnum option) {
				return java.util.Optional.of((double) option.getValue());
			}
		}
		return java.util.Optional.empty();
	}

	private static java.util.Optional<String> firstSunSpecString(OpenemsComponent component, SunSpecPoint... points) {
		for (var point : points) {
			var value = getSunSpecRawValue(component, point);
			if (value instanceof String string) {
				var trimmed = cleanString(string);
				if (!trimmed.isEmpty()) {
					return java.util.Optional.of(trimmed);
				}
			}
		}
		return java.util.Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private static Object getSunSpecRawValue(OpenemsComponent component, SunSpecPoint point) {
		for (Class<?> clazz = component.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			try {
				var method = clazz.getDeclaredMethod("getSunSpecChannel", SunSpecPoint.class);
				method.setAccessible(true);
				var optional = (java.util.Optional<Channel<?>>) method.invoke(component, point);
				if (optional.isEmpty()) {
					return null;
				}
				return optional.get().value().get();
			} catch (NoSuchMethodException e) {
				// Try parent class.
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	private static String cleanString(String value) {
		return value.replace("\0", "").trim();
	}

	@SuppressWarnings("deprecation")
	private static Object getRawValue(OpenemsComponent component, String channelId) {
		try {
			Channel<?> channel = component._channel(channelId);
			if (channel == null) {
				return null;
			}
			return channel.value().get();
		} catch (RuntimeException e) {
			return null;
		}
	}

	@Override
	public void retryMqttCommunication() {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(false);
	}

	@Override
	public void handleEvent(Event event) {
		// This controller is executed by the Scheduler via run().
	}
}
