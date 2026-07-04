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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = ConfigEss.class, factory = true)
@Component(//
		name = "Ziot.Generic.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE })
public class ZiotGenericEssImpl extends AbstractOpenemsModbusComponent
		implements ZiotGenericEss, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, EventHandler,
		ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	private ConfigEss config;
	private GenericMapping mapping = new GenericMapping();

	public ZiotGenericEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ZiotGenericEss.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, ConfigEss config) throws OpenemsException {
		this.config = config;
		this.mapping = GenericMappingLoader.load(config.mappingFile(), config.model().key());
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.updateConfiguredLimits();
		this.getGridModeChannel().setNextValue(GridMode.UNDEFINED);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return GenericProtocolFactory.create(this, this.mapping, GenericChannelMap.ess(), this::map);
	}

	private io.openems.edge.bridge.modbus.api.element.ModbusElement map(
			io.openems.edge.common.channel.ChannelId channelId,
			io.openems.edge.bridge.modbus.api.element.ModbusElement element,
			io.openems.edge.bridge.modbus.api.ElementToChannelConverter converter) {
		return this.m(channelId, element, converter);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		if (EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE.equals(event.getTopic())) {
			this.updateConfiguredLimits();
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config == null || this.config.readOnly()) {
			return;
		}
		this.<IntegerWriteChannel>channel(ZiotGenericEss.ChannelId.WORKING_MODE).setNextWriteValue(2);
		this.<IntegerWriteChannel>channel(ZiotGenericEss.ChannelId.REMOTE_ACTIVE_POWER_SETPOINT)
				.setNextWriteValue(activePower);
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() + "|L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	private void updateConfiguredLimits() {
		if (this.config == null) {
			return;
		}
		this._setCapacity(this.config.capacity());
		this._setMaxApparentPower(this.config.maxApparentPower());
		this.getAllowedChargePowerChannel().setNextValue(-Math.abs(this.config.maxChargePower()));
		this._setAllowedDischargePower(Math.abs(this.config.maxDischargePower()));
	}
}
