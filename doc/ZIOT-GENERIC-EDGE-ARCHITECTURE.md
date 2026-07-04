# ZIOT Generic Edge Architecture

This document locks the direction for the new ZIOT Edge build.

## Goal

The Edge must keep the stable OpenEMS runtime, while device support moves to a
mapping-first model.

OpenEMS continues to provide:

- component lifecycle
- channels
- Modbus bridge
- scheduler and controllers
- backend and WebSocket APIs
- ConfigAdmin persistence

ZIOT adds a generic device layer that turns a mapping profile into normal
OpenEMS components.

## Runtime Layers

```text
BE/UI
  |
  | fixed payload fields by device type
  v
Controller / Backend API
  |
  | OpenEMS channel names
  v
ZIOT Generic Device Layer
  |
  | deviceConfig mapping profile
  v
OpenEMS Modbus Bridge
  |
  | FC3 / FC4 / FC6
  v
Physical device
```

## Device Contract

The contract is `deviceType` first. Every device with the same `deviceType`
must expose the same BE/UI field list.

Supported first-phase types:

- `meter`
- `pvInverter`
- `bess`
- `sensor`

Later types:

- `battery`
- `batteryInverter`

If a device model does not support a field, the mapping must keep the field and
set `offSet`, `dataType`, `PF`, and `size` to `null`.

## Mapping Rules

The mapping file is split by task:

- `read_registers`: telemetry from holding registers, FC3
- `read_input_registers`: telemetry from input registers, FC4
- `watch_events`: alarm/error registers, read like telemetry but used for
  immediate backend error payloads
- `write_registers`: control registers, FC6 in the first implementation

`State` and `ModbusCommunicationFailed` are OpenEMS runtime channels. They
should stay in the BE/UI schema, but do not need physical register mapping.

`ErrorCode1..3` in the BE payload are produced from numeric channels whose
channel ID contains `fault`, `alarm`, `warning`, `error`, or `protected`.

## First Generic Bundle

The first code bundle is:

```text
io.openems.edge.ziot.generic
```

It provides three factory PIDs:

```text
Ziot.Generic.Meter
Ziot.Generic.PvInverter
Ziot.Generic.Ess
```

Each component:

- reads one `model` from `deviceConfig_openems_fields.conf`
- maps supported fields to standard OpenEMS channels
- ignores fields with `offSet: null`
- keeps the existing OpenEMS natures so existing controllers continue to work

The old model-specific drivers remain available during migration. The generic
bundle is opt-in and should be enabled per site/model only after validation.

## Migration Order

1. Keep current model drivers active.
2. Build and validate the generic bundle.
3. Test one meter profile first, for example `Meter.Selec.MFM383`.
4. Test one read-only PV profile, for example `PV-Inverter.Sungrow.SG110CX`.
5. Test one ESS profile with write registers, for example `Ess.Omni261`.
6. Migrate additional models only after the real device values and control
   direction are verified.

## Safety Rules

- Do not remove model-specific drivers until the matching generic profile has
  been tested on a real device.
- Do not auto-enable generic devices in default Edge config.
- Control writes must be limited to `write_registers`.
- If a mapping field is unknown to the generic runtime, skip it and log/keep
  Edge running.
- The backend payload schema must not depend on model-specific channel names.
