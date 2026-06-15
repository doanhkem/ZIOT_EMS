package io.openems.edge.controller.api.backend;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.TreeBasedTable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * Method {@link #collectData()} is called Synchronously with the Core.Cycle to
 * collect values of Channels. Sending of values is then delegated to an
 * asynchronous task.
 *
 * <p>
 * The logic sends one synchronized snapshot of all values on fixed five-minute
 * buckets.
 */
public class SendChannelValuesWorker {

	private static final int AGGREGATION_MINUTES = 5;
	private static final int SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS = 300; /* 5 minutes */
	private static final long SEND_VALUES_OF_ALL_CHANNELS_AFTER_MILLIS = SEND_VALUES_OF_ALL_CHANNELS_AFTER_SECONDS
			* 1_000L;
	private static final Set<String> INVERTER_CHANNELS = Set.of(//
			"State", "ModbusCommunicationFailed", //
			"CurrentL1", "CurrentL2", "CurrentL3", //
			"VoltageL1", "VoltageL2", "VoltageL3", //
			"VoltageL1L2", "VoltageL2L3", "VoltageL3L1", //
			"ActivePower", "Frequency", "ReactivePower", "PowerFactor", //
			"ActiveProductionEnergy", "TmpCab", "OperatingStatus", //
			"PV1Voltage", "PV2Voltage", "PV3Voltage", "PV4Voltage", "PV5Voltage", //
			"PV6Voltage", "PV7Voltage", "PV8Voltage", "PV9Voltage", "PV10Voltage", //
			"PV11Voltage", "PV12Voltage", "PV13Voltage", "PV14Voltage", "PV15Voltage", //
			"PV16Voltage", "PV17Voltage", "PV18Voltage", "PV19Voltage", "PV20Voltage", //
			"PV1Current", "PV2Current", "PV3Current", "PV4Current", "PV5Current", //
			"PV6Current", "PV7Current", "PV8Current", "PV9Current", "PV10Current", //
			"PV11Current", "PV12Current", "PV13Current", "PV14Current", "PV15Current", //
			"PV16Current", "PV17Current", "PV18Current", "PV19Current", "PV20Current");
	private static final Set<String> METER_CHANNELS = Set.of(//
			"State", "ModbusCommunicationFailed", //
			"VoltageL1", "VoltageL2", "VoltageL3", //
			"VoltageL1L2", "VoltageL2L3", "VoltageL3L1", //
			"CurrentL1", "CurrentL2", "CurrentL3", //
			"ActivePower", "Frequency", "ReactivePower", "PowerFactor", //
			"ActiveProductionEnergy", "ActiveConsumptionEnergy");
	private static final Set<String> ESS_CHANNELS = Set.of(//
			"State", "Soc", "Capacity", "GridMode", //
			"ActivePower", "ReactivePower", "MaxApparentPower", //
			"ActiveChargeEnergy", "ActiveDischargeEnergy", //
			"MinCellVoltage", "MaxCellVoltage", "MinCellTemperature", "MaxCellTemperature");
	private static final Set<String> BATTERY_CHANNELS = Set.of(//
			"State", "Soc", "Capacity", "Voltage", "Current", //
			"ChargeMaxCurrent", "DischargeMaxCurrent", //
			"MinCellVoltage", "MaxCellVoltage", "MinCellTemperature", "MaxCellTemperature");
	private static final Set<String> BATTERY_INVERTER_CHANNELS = Set.of(//
			"State", "ActivePower", "ReactivePower", "MaxApparentPower", //
			"Voltage", "Current", "DcVoltage", "DcCurrent", "DcPower");
	private static final Set<String> SUM_CHANNELS = Set.of(//
			"State", //
			"ProductionActivePower", "ProductionActiveEnergy", //
			"ConsumptionActivePower", "ConsumptionActiveEnergy", //
			"GridActivePower", "GridBuyActiveEnergy", "GridSellActiveEnergy", //
			"EssActivePower", "EssSoc", "EssCapacity", //
			"EssActiveChargeEnergy", "EssActiveDischargeEnergy");

	private final Logger log = LoggerFactory.getLogger(SendChannelValuesWorker.class);

	private final ControllerApiBackendImpl parent;
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(1), //
			new ThreadFactoryBuilder().setNameFormat(ControllerApiBackendImpl.COMPONENT_NAME + ":SendWorker-%d")
					.build(), //
			new ThreadPoolExecutor.DiscardOldestPolicy());

	private final ScheduledExecutorService aggregatedExecutor = Executors.newScheduledThreadPool(1,
			new ThreadFactoryBuilder()
					.setNameFormat(ControllerApiBackendImpl.COMPONENT_NAME + ":SendAggregatedWorker-%d").build());
	private final int randomWaitSeconds = new Random().nextInt((int) (AGGREGATION_MINUTES * 60 * 0.9));

	private final AtomicBoolean sendValuesOfAllChannelsAggregated = new AtomicBoolean(true);

	private long lastSendValuesOfAllChannelsBucketStart = Long.MIN_VALUE;

	private Instant lastSendAggregatedDataTimestamp;

	protected SendChannelValuesWorker(ControllerApiBackendImpl parent) {
		this.parent = parent;
	}

	/**
	 * Triggers sending all Channel values once.
	 */
	public synchronized void sendValuesOfAllChannelsOnce() {
		this.sendValuesOfAllChannelsAggregated.set(true);
	}

	/**
	 * Stops the {@link SendChannelValuesWorker}.
	 */
	public void deactivate() {
		// Shutdown executor
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.aggregatedExecutor, 5);
	}

	/**
	 * Called synchronously on AFTER_PROCESS_IMAGE event. Collects all the data and
	 * triggers asynchronous sending.
	 */
	public synchronized void collectData() {
		final var now = ZonedDateTime.now(this.parent.componentManager.getClock());

		// Update the values of all channels
		final var enabledComponents = this.parent.componentManager.getEnabledComponents();
		final var allValues = this.collectData(enabledComponents);
		final var aggregatedValues = this.collectAggregatedData(now, enabledComponents);

		this.executor.execute(new SendTask(this, now.toInstant(), allValues));
		if (aggregatedValues != null && !aggregatedValues.isEmpty()) {
			aggregatedValues.rowMap().forEach((timestamp, data) -> {
				this.aggregatedExecutor.schedule(
						new SendAggregatedDataTask(this, Instant.ofEpochMilli(timestamp), data), this.randomWaitSeconds,
						TimeUnit.SECONDS);
			});
		}
	}

	/**
	 * Cycles through all Channels and collects the value.
	 *
	 * @param enabledComponents the enabled components
	 * @return collected data
	 */
	private ImmutableMap<String, JsonElement> collectData(List<OpenemsComponent> enabledComponents) {
		try {
			final var result = ImmutableMap.<String, JsonElement>builder();
			for (var component : enabledComponents) {
				final var channels = getDeviceChannels(component.id());
				if (channels == null) {
					continue;
				}
				for (var channelId : channels) {
					result.put(component.id() + "/" + channelId, getChannelValue(component, channelId));
				}
			}
			return result.build();
		} catch (Exception e) {
			// ConcurrentModificationException can happen if Channels are dynamically added
			// or removed
			this.parent.logWarn(this.log, "Unable to collect data: " + e.getMessage());
			return ImmutableMap.of();
		}
	}

	protected static Set<String> getDeviceChannels(String componentId) {
		final var id = componentId.toLowerCase();
		if (id.equals("_sum")) {
			return SUM_CHANNELS;
		}
		if (id.startsWith("pvinverter")) {
			return INVERTER_CHANNELS;
		}
		if (id.startsWith("meter")) {
			return METER_CHANNELS;
		}
		if (id.startsWith("ess")) {
			return ESS_CHANNELS;
		}
		if (id.startsWith("batteryinverter")) {
			return BATTERY_INVERTER_CHANNELS;
		}
		if (id.startsWith("battery")) {
			return BATTERY_CHANNELS;
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	protected static JsonElement getChannelValue(OpenemsComponent component, String channelId) {
		final var channel = component._channel(channelId);
		if (channel == null || channel.channelDoc().getAccessMode() == AccessMode.WRITE_ONLY) {
			return JsonNull.INSTANCE;
		}
		return normalizeDeviceValue(channel.channelDoc().getUnit(), channel.value().asJson());
	}

	protected static JsonElement normalizeDeviceValue(Unit unit, JsonElement value) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			return value;
		}

		final var factor = getDeviceValueFactor(unit);
		return new JsonPrimitive(normalize(value.getAsDouble() * factor));
	}

	private static double getDeviceValueFactor(Unit unit) {
		if (unit == Unit.THOUSANDTH) {
			return 0.001;
		}
		if (unit == Unit.TENTHOUSANDTH) {
			return 0.0001;
		}
		if (unit.baseUnit == null) {
			return 1.0;
		}
		return Math.pow(10, unit.scaleFactor);
	}

	private static Double normalize(double value) {
		return Double.valueOf(Math.round(value * 1000.0) / 1000.0);
	}

	private TreeBasedTable<Long, String, JsonElement> collectAggregatedData(ZonedDateTime now,
			List<OpenemsComponent> enabledComponents) {
		final var endTime = now.truncatedTo(DurationUnit.ofMinutes(AGGREGATION_MINUTES));
		final var startTime = endTime.minusMinutes(AGGREGATION_MINUTES);

		final var timestamp = startTime.toInstant();
		if (this.lastSendAggregatedDataTimestamp == null) {
			this.lastSendAggregatedDataTimestamp = timestamp;
			return null;
		}
		if (timestamp.equals(this.lastSendAggregatedDataTimestamp)) {
			return null;
		}
		this.lastSendAggregatedDataTimestamp = timestamp;
		final var timestampMillis = timestamp.toEpochMilli();

		final var sendAllChannels = this.sendValuesOfAllChannelsAggregated.getAndSet(false);

		final var table = TreeBasedTable.<Long, String, JsonElement>create();
		enabledComponents.stream() //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> // Ignore WRITE_ONLY Channels
				channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY //
						// Send only the configured device telemetry schema
						&& Optional.ofNullable(getDeviceChannels(channel.address().getComponentId()))
								.map(channels -> channels.contains(channel.address().getChannelId())).orElse(false) //
						// Ignore Low-Priority Channels
						&& channel.channelDoc().getRemotePersistencePriority()
								.isAtLeast(this.parent.config.aggregationPriority()))
				.forEach(channel -> {
					try {
						// This is the highest timestamp before `startTime`. If existing it is used for
						// the tailMap to make sure we get a Value even for Channels where the value has
						// not changed within the last 5 minutes.
						var channelStartTime = Optional
								.ofNullable(channel.getPastValues().floorKey(startTime.toLocalDateTime()))
								.orElse(startTime.toLocalDateTime());

						var value = channel.getPastValues() //
								.tailMap(channelStartTime, true) //
								.entrySet() //
								.stream() //
								.filter(e -> e.getKey().isBefore(endTime.toLocalDateTime())) //
								.filter(e -> e.getValue().isDefined()).map(e -> e.getValue().get()) //
								.collect(aggregateCollector(channel.channelDoc().getUnit().isCumulated(), //
										channel.getType()));

						// TODO aggregation should be modifiable in Doc e. g. not every EnumDoc may want
						// this behaviour
						if (channel.channelDoc() instanceof EnumDoc) {
							value = aggregateEnumChannel(channel, channelStartTime, endTime.toLocalDateTime());
						}

						if (!sendAllChannels && value.isJsonNull()) {
							return;
						}
						table.put(timestampMillis, channel.address().toString(),
								normalizeDeviceValue(channel.channelDoc().getUnit(), value));
					} catch (IllegalArgumentException e) {
						// unable to collect data because types are not matching the expected one
						e.printStackTrace();
					}
				});
		return table;
	}

	// TODO aggregation should be moved to doc
	protected static JsonElement aggregateEnumChannel(//
			Channel<?> channel, //
			LocalDateTime channelStartTime, //
			LocalDateTime endTime //
	) {
		final var doc = channel.channelDoc();
		if (!(doc instanceof EnumDoc)) {
			return JsonNull.INSTANCE;
		}
		final var numberOfValuesPerOption = channel.getPastValues() //
				.tailMap(channelStartTime, true) //
				.entrySet() //
				.stream() //
				.filter(e -> e.getKey().isBefore(endTime)) //
				.filter(e -> e.getValue().isDefined()) //
				.map(e -> (Integer) e.getValue().get()) //
				.collect(groupingBy(Function.identity(), counting()));

		final var values = numberOfValuesPerOption.entrySet().stream() //
				.sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue())) //
				.toList();

		final var maxValues = new ArrayList<Integer>();
		var maxCount = -1L;
		for (var entry : values) {
			if (entry.getValue() < maxCount) {
				break;
			}
			if (entry.getValue() == maxCount) {
				maxValues.add(entry.getKey());
				continue;
			}
			maxCount = entry.getValue();
			maxValues.clear();
			maxValues.add(entry.getKey());
		}

		// pick first value with most appearances
		for (var entry : channel.getPastValues().descendingMap().entrySet()) {
			for (var optionValue : maxValues) {
				if (!entry.getValue().isDefined()) {
					continue;
				}
				final var entryValue = entry.getValue().get();
				if (((Integer) entryValue).intValue() == optionValue) {
					return new JsonPrimitive(optionValue);
				}
			}
		}
		return JsonNull.INSTANCE;
	}

	protected static Collector<Object, ?, JsonElement> aggregateCollector(//
			final boolean isCumulated, //
			final OpenemsType type //
	) {
		return Collector.of(ArrayList::new, //
				(a, b) -> a.add(b), //
				(a, b) -> {
					b.addAll(a);
					return b;
				}, //
				t -> aggregate(isCumulated, type, t) //
		);
	}

	protected static JsonElement aggregate(boolean isCumulated, OpenemsType type, Collection<Object> values)
			throws IllegalArgumentException {
		switch (type) {
		case DOUBLE, FLOAT -> {
			final var stream = values.stream() //
					.mapToDouble(item -> TypeUtils.getAsType(OpenemsType.DOUBLE, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsDouble());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(avgOpt.getAsDouble());
				}
			}
		}
		// round averages to their type
		case BOOLEAN, LONG, INTEGER, SHORT -> {
			final var stream = values.stream() //
					.mapToLong(item -> TypeUtils.getAsType(OpenemsType.LONG, item));
			if (isCumulated) {
				final var maxOpt = stream.max();
				if (maxOpt.isPresent()) {
					return new JsonPrimitive(maxOpt.getAsLong());
				}
			} else {
				final var avgOpt = stream.average();
				if (avgOpt.isPresent()) {
					return new JsonPrimitive(Math.round(avgOpt.getAsDouble()));
				}
			}
		}
		case STRING -> {
			// return first string for now
			for (var item : values) {
				return new JsonPrimitive(TypeUtils.<String>getAsType(type, item));
			}
		}
		}
		return JsonNull.INSTANCE;
	}

	private static class SendTask implements Runnable {

		private final SendChannelValuesWorker parent;
		private final Instant timestamp;
		private final Map<String, JsonElement> allValues;

		public SendTask(SendChannelValuesWorker parent, Instant timestamp, Map<String, JsonElement> allValues) {
			this.parent = parent;
			this.timestamp = timestamp;
			this.allValues = allValues;
		}

		@Override
		public void run() {
			final var bucketStart = getFiveMinuteBucketStart(this.timestamp);
			if (bucketStart == Long.MIN_VALUE || bucketStart == this.parent.lastSendValuesOfAllChannelsBucketStart) {
				return;
			}

			final var message = new TimestampedDataNotification();
			message.add(bucketStart, this.allValues);

			if (this.parent.parent.config.debugMode()) {
				this.parent.parent.logInfo(this.parent.log, "Sending five-minute snapshot at ["
						+ Instant.ofEpochMilli(bucketStart) + "] with [" + this.allValues.size() + " values]");
			}

			this.parent.parent.logInfo(this.parent.log, "BACKEND_PAYLOAD=" + message.getParams());

			if (this.parent.parent.websocket.sendMessage(message)) {
				this.parent.lastSendValuesOfAllChannelsBucketStart = bucketStart;
			}
		}
	}

	protected static long getFiveMinuteBucketStart(Instant timestamp) {
		final var epochMillis = timestamp.toEpochMilli();
		final var bucketOffset = Math.floorMod(epochMillis, SEND_VALUES_OF_ALL_CHANNELS_AFTER_MILLIS);

		// Send on the first Core.Cycle within minute 00/05/10/15..., while the
		// packet timestamp is fixed to the beginning of that five-minute bucket.
		if (bucketOffset >= 60_000L) {
			return Long.MIN_VALUE;
		}
		return epochMillis - bucketOffset;
	}

	private static final class SendAggregatedDataTask implements Runnable {

		private final SendChannelValuesWorker parent;
		private final Instant timestamp;
		private final Map<String, JsonElement> allValues;

		public SendAggregatedDataTask(SendChannelValuesWorker parent, Instant timestamp,
				Map<String, JsonElement> allValues) {
			super();
			this.parent = parent;
			this.timestamp = timestamp;
			this.allValues = allValues;
		}

		@Override
		public void run() {
			final var message = new AggregatedDataNotification();
			message.add(this.timestamp.toEpochMilli(), this.allValues);

			final var wasSent = this.parent.parent.websocket.sendMessage(message);

			// Set the UNABLE_TO_SEND channel
			this.parent.parent.getUnableToSendChannel().setNextValue(!wasSent);
		}

	}

}
