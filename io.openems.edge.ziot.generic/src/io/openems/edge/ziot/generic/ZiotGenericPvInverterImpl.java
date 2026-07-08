package io.openems.edge.ziot.generic;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = ConfigPvInverter.class, factory = true)
@Component(//
		name = "Ziot.Generic.PvInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=PRODUCTION" } //
)
public class ZiotGenericPvInverterImpl extends AbstractOpenemsModbusComponent
		implements ZiotGenericPvInverter, ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	private ConfigPvInverter config;
	private GenericMapping mapping = new GenericMapping();
	private GenericWriteCapabilities writeCapabilities = GenericWriteCapabilities.of(this.mapping,
			GenericChannelMap.pvInverter());

	public ZiotGenericPvInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ZiotGenericPvInverter.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, ConfigPvInverter config) throws OpenemsException {
		this.config = config;
		this.mapping = GenericMappingLoader.load(config.mappingFile(), config.model().key());
		this.writeCapabilities = GenericWriteCapabilities.of(this.mapping, GenericChannelMap.pvInverter());
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.updateConfiguredLimits();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return GenericProtocolFactory.create(this, this.mapping, GenericChannelMap.pvInverter(), this::map);
	}

	private io.openems.edge.bridge.modbus.api.element.ModbusElement map(
			io.openems.edge.common.channel.ChannelId channelId,
			io.openems.edge.bridge.modbus.api.element.ModbusElement element,
			io.openems.edge.bridge.modbus.api.ElementToChannelConverter converter) {
		return this.m(channelId, element, converter);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		// Generic PV profile is read-only unless a write register is configured.
	}

	@Override
	public void setActivePowerLimit(Integer value) throws OpenemsNamedException {
		if (value == null) {
			return;
		}
		if (this.writeCapabilities.has(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT)) {
			this.getActivePowerLimitChannel().setNextWriteValue(value);
			return;
		}
		if (this.writeCapabilities.has(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT)) {
			this.<IntegerWriteChannel>channel(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT)
					.setNextWriteValue(value);
			return;
		}
		if (this.writeCapabilities.has(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT)) {
			this.<IntegerWriteChannel>channel(ZiotGenericPvInverter.ChannelId.SET_ACTIVE_POWER_LIMIT_PERCENT)
					.setNextWriteValue(this.powerToPercent(value));
			return;
		}
		throw new OpenemsException("No PV active-power write register is configured.");
	}

	@Override
	public void setActivePowerLimit(int value) throws OpenemsNamedException {
		this.setActivePowerLimit(Integer.valueOf(value));
	}

	private int powerToPercent(int power) throws OpenemsException {
		var maxPower = this.getMaxApparentPower().orElse(0);
		if (maxPower <= 0) {
			throw new OpenemsException(
					"MaxApparentPower must be configured/read before writing an active-power percentage.");
		}
		return clampPercent((int) Math.round(power * 100.0 / maxPower));
	}

	private static int clampPercent(int value) {
		return Math.max(-100, Math.min(100, value));
	}

	private void updateConfiguredLimits() {
		if (this.config == null || this.config.maxApparentPower() <= 0) {
			return;
		}
		this._setMaxApparentPower(this.config.maxApparentPower());
		this._setMaxActivePower(this.config.maxApparentPower());
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
