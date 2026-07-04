# ZIOT OpenEMS Edge Review Notes

## Current Scope

This document summarizes local ZIOT changes that are useful for code review and BE/UI alignment.

## Backend Payload Error Codes

File:

- `io.openems.edge.controller.api.backend/src/io/openems/edge/controller/api/backend/SendChannelValuesWorker.java`

### Added fields

Every supported device payload group now includes three numeric error-code fields:

- `ErrorCode1`
- `ErrorCode2`
- `ErrorCode3`

Supported component ID groups:

- `pvInverter*`
- `meter*`
- `ess*`
- `battery*`
- `batteryInverter*`

If a device does not expose mapped alarm/fault/warning/protected channels, all three fields are sent as:

```text
0.0, 0.0, 0.0
```

### Error code source rule

The Edge does not decode fault bits. The BE is expected to decode the raw register value.

The Edge selects up to three existing device channels whose channel ID contains one of:

- `fault`
- `alarm`
- `warning`
- `error`
- `protected`

The selected channel values are sent as `ErrorCode1..3`.

### Send timing

The normal payload continues to be sent on fixed 5-minute buckets.

When `ErrorCode1..3` changes, Edge immediately sends a `timestampedData` payload containing the changed device error fields.

Log marker for immediate error payload:

```text
BACKEND_ERROR_PAYLOAD=...
```

Normal 5-minute payload log marker remains:

```text
BACKEND_PAYLOAD=...
```

## Supported Devices

The `ErrorCode1..3` fields are available for every device whose component ID matches the backend payload schema.

### Device type support

| Device type | Component ID pattern | Error fields | Mapping behavior |
|---|---|---|---|
| PV inverter | `pvInverter*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |
| Meter | `meter*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |
| ESS/BESS | `ess*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |
| Battery | `battery*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |
| Battery inverter | `batteryInverter*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |
| Sensor | `sensor*` | `ErrorCode1..3` | Uses mapped alarm/fault/warning/error/protected channels if present; otherwise `0.0` |

### Devices with actual mapped error registers

| Device | Component ID pattern | Supported now |
|---|---|---|
| ESS OMNI 261 | `ess*` | Yes |
| ESS OMNI 430 PCS | `ess*` | Yes |
| ESS SMA SunnyIsland | `ess*` | Yes |
| Sensor Huawei Irradiance | `sensor*` | Payload supported; no error registers mapped |

### Devices with default error fields only

These device families will still send `ErrorCode1..3`, but currently default to `0.0` unless their driver exposes a matching alarm/fault/warning/error/protected channel:

| Device family | Component ID pattern | Current error-code value |
|---|---|---|
| PV-Inverter SMA CORE2 | `pvInverter*` | `0.0, 0.0, 0.0` |
| PV-Inverter Sungrow SG110CX | `pvInverter*` | `0.0, 0.0, 0.0` |
| PV-Inverter Fronius | `pvInverter*` | `0.0, 0.0, 0.0` |
| PV-Inverter Huawei SmartLogger | `pvInverter*` | `0.0, 0.0, 0.0` if no matching active fault channel is exposed |
| PV-Inverter KACO/Kostal/SolarLog/AISWEI | `pvInverter*` | `0.0, 0.0, 0.0` if no matching numeric error channel is exposed |
| Meter Acrel/Chint/Eastron/Janitza/Phoenix/Schneider/Selec/SMA | `meter*` | `0.0, 0.0, 0.0` unless driver exposes matching numeric error channel |
| Sensor Huawei Irradiance | `sensor*` | `0.0, 0.0, 0.0` |

## Payload Field Samples

### PV inverter payload fields

Example component: `pvInverter0`

```text
pvInverter0/State
pvInverter0/ModbusCommunicationFailed
pvInverter0/ErrorCode1
pvInverter0/ErrorCode2
pvInverter0/ErrorCode3
pvInverter0/ActivePower
pvInverter0/ReactivePower
pvInverter0/PowerFactor
pvInverter0/Frequency
pvInverter0/ActiveProductionEnergy
pvInverter0/VoltageL1
pvInverter0/VoltageL2
pvInverter0/VoltageL3
pvInverter0/CurrentL1
pvInverter0/CurrentL2
pvInverter0/CurrentL3
pvInverter0/PV1Voltage..PV20Voltage
pvInverter0/PV1Current..PV20Current
```

### Meter payload fields

Example component: `meter0`

```text
meter0/State
meter0/ModbusCommunicationFailed
meter0/ErrorCode1
meter0/ErrorCode2
meter0/ErrorCode3
meter0/ActivePower
meter0/ReactivePower
meter0/PowerFactor
meter0/Frequency
meter0/ActiveProductionEnergy
meter0/ActiveConsumptionEnergy
meter0/VoltageL1
meter0/VoltageL2
meter0/VoltageL3
meter0/VoltageL1L2
meter0/VoltageL2L3
meter0/VoltageL3L1
meter0/CurrentL1
meter0/CurrentL2
meter0/CurrentL3
```

### ESS/BESS payload fields

Example component: `ess0`

```text
ess0/State
ess0/ErrorCode1
ess0/ErrorCode2
ess0/ErrorCode3
ess0/Soc
ess0/Capacity
ess0/GridMode
ess0/ActivePower
ess0/ReactivePower
ess0/MaxApparentPower
ess0/ActiveChargeEnergy
ess0/ActiveDischargeEnergy
ess0/MinCellVoltage
ess0/MaxCellVoltage
ess0/MinCellTemperature
ess0/MaxCellTemperature
```

### Battery payload fields

Example component: `battery0`

```text
battery0/State
battery0/ErrorCode1
battery0/ErrorCode2
battery0/ErrorCode3
battery0/Soc
battery0/Capacity
battery0/Voltage
battery0/Current
battery0/ChargeMaxCurrent
battery0/DischargeMaxCurrent
battery0/MinCellVoltage
battery0/MaxCellVoltage
battery0/MinCellTemperature
battery0/MaxCellTemperature
```

### Battery inverter payload fields

Example component: `batteryInverter0`

```text
batteryInverter0/State
batteryInverter0/ErrorCode1
batteryInverter0/ErrorCode2
batteryInverter0/ErrorCode3
batteryInverter0/ActivePower
batteryInverter0/ReactivePower
batteryInverter0/MaxApparentPower
batteryInverter0/Voltage
batteryInverter0/Current
batteryInverter0/DcVoltage
batteryInverter0/DcCurrent
batteryInverter0/DcPower
```

### Sensor payload fields

Example component: `sensor0`

```text
sensor0/State
sensor0/ModbusCommunicationFailed
sensor0/ErrorCode1
sensor0/ErrorCode2
sensor0/ErrorCode3
sensor0/DailyIrradiation
sensor0/TotalIrradiance
```

## Sensor Huawei Irradiance

PID:

```text
Sensor.Huawei.Irradiance
```

Bundle:

```text
io.openems.edge.sensor.huawei.irradiance
```

ConfigMgr name:

```text
Sensor Huawei Irradiance
```

Mapped Modbus registers:

| Edge channel | Register | Modbus function | Type | Scale | BE field |
|---|---:|---|---|---:|---|
| `DAILY_IRRADIATION` | `40043` | FC3 Holding Registers | `uint32` | `0.001` | `sensor*/DailyIrradiation` |
| `TOTAL_IRRADIANCE` | `40035` | FC3 Holding Registers | `uint16` | `0.1` | `sensor*/TotalIrradiance` |

Units are documented in channel text:

| BE field | Unit |
|---|---|
| `sensor*/DailyIrradiation` | `kWh/m2` |
| `sensor*/TotalIrradiance` | `W/m2` |

### Sum payload fields

Example component: `_sum`

```text
_sum/State
_sum/ProductionActivePower
_sum/ProductionActiveEnergy
_sum/ConsumptionActivePower
_sum/ConsumptionActiveEnergy
_sum/GridActivePower
_sum/GridBuyActiveEnergy
_sum/GridSellActiveEnergy
_sum/EssActivePower
_sum/EssSoc
_sum/EssCapacity
_sum/EssActiveChargeEnergy
_sum/EssActiveDischargeEnergy
```

## Current Error Mapping

The current implementation selects alarm channels by channel name order. With the current code, the effective mapping is:

### ESS OMNI 261

| BE field | Edge channel | Register |
|---|---|---:|
| `ess*/ErrorCode1` | `BMS_FAULT_WORD` | `25216` |
| `ess*/ErrorCode2` | `BMS_PROTECTED_WORD` | `25218` |
| `ess*/ErrorCode3` | `BMS_WARNING_WORD` | `25217` |

Other mapped OMNI 261 fault registers currently read by Edge but not included in `ErrorCode1..3`:

| Edge channel | Register |
|---|---:|
| `PCS_FAULT_STATUS` | `25102` |
| `PCS_FAULT_WORD_1` | `25132` |
| `PCS_FAULT_WORD_2` | `25133` |
| `PCS_FAULT_WORD_3` | `25134` |
| `PCS_FAULT_WORD_4` | `25135` |

### ESS OMNI 430 PCS

| BE field | Edge channel | Register |
|---|---|---:|
| `ess*/ErrorCode1` | `BMS_FAULT_WORD` | `33184` |
| `ess*/ErrorCode2` | `BMS_PROTECTED_WORD` | `33181` |
| `ess*/ErrorCode3` | `BMS_WARNING_WORD` | `33182` |

Other mapped OMNI 430 PCS fault registers currently read by Edge but not included in `ErrorCode1..3`:

| Edge channel | Register |
|---|---:|
| `PCS_FAULT_STATUS` | `32457` |
| `PCS_FAULT_WORD_1` | `32499` |
| `PCS_FAULT_WORD_2` | `32500` |
| `PCS_FAULT_WORD_3` | `32501` |
| `PCS_FAULT_WORD_4` | `32502` |

### ESS SMA SunnyIsland

| BE field | Edge channel | Register |
|---|---|---:|
| `ess*/ErrorCode1` | `FAULT_CORRECTION_MEASURE` | `30215` |
| `ess*/ErrorCode2` | default | `0.0` |
| `ess*/ErrorCode3` | default | `0.0` |

## Review Note

For stable BE decoding, the recommended follow-up is to replace automatic channel-name selection with fixed per-model mapping.

Suggested OMNI mapping:

| BE field | Meaning |
|---|---|
| `ErrorCode1` | PCS fault/status code |
| `ErrorCode2` | BMS fault code |
| `ErrorCode3` | BMS warning/protection code |

This avoids BE decoding changing if new channels are added or channel names sort differently.

## ConfigMgr Device Filtering

File:

- `io.openems.edge.application/EdgeApp.bndrun`

Purpose:

- Reduce visible unused device factories in Felix ConfigMgr.
- Keep core OpenEMS Edge, bridge, backend, REST, scheduler, timedata, and selected device bundles available.

Important note:

- Mixed bundles such as `io.openems.edge.sma` can expose multiple SMA factories from one bundle.
- Hiding only one factory inside a mixed bundle requires source-level factory disabling or bundle split, not only `EdgeApp.bndrun` filtering.

## Validation

Command used:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat buildEdge --no-daemon --console=plain --warn
```

Result:

```text
Built ...\build\openems-edge.jar!
```
