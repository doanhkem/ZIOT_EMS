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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

@Designate(ocd = ConfigSensor.class, factory = true)
@Component(//
		name = "Ziot.Generic.Sensor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ZiotGenericSensorImpl extends AbstractOpenemsModbusComponent
		implements ZiotGenericSensor, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	private GenericMapping mapping = new GenericMapping();

	public ZiotGenericSensorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ZiotGenericSensor.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, ConfigSensor config) throws OpenemsException {
		this.mapping = GenericMappingLoader.load(config.mappingFile(), config.model().key());
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return GenericProtocolFactory.create(this, this.mapping, GenericChannelMap.sensor(), this::map);
	}

	private io.openems.edge.bridge.modbus.api.element.ModbusElement map(
			io.openems.edge.common.channel.ChannelId channelId,
			io.openems.edge.bridge.modbus.api.element.ModbusElement element,
			io.openems.edge.bridge.modbus.api.ElementToChannelConverter converter) {
		return this.m(channelId, element, converter);
	}

	@Override
	public String debugLog() {
		return "Irr:" + this.getTotalIrradiance().asString() + "|Daily:" + this.getDailyIrradiation().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode));
	}
}
