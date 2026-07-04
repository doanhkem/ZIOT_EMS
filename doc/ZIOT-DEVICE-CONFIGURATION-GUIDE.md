# ZIOT OpenEMS Edge - Device Configuration Guide

Tai lieu nay mo ta quy trinh add device trong Edge hien tai cua ZIOT/FEMS, gom:

- Add Bridge Modbus TCP/RTU
- Add Meter, PV-Inverter, ESS/BESS, Sensor
- Add PV-Inverter Cluster khi co nhieu bien tan
- Goi JSON-RPC WebSocket dung de BE/UI tao cau hinh

## 1. Ket noi WebSocket

Edge WebSocket API mac dinh:

```text
ws://<edge-ip>:8085
```

Vi du:

```text
ws://10.9.0.5:8085
```

Dang nhap truoc khi tao cau hinh:

```json
{
  "jsonrpc": "2.0",
  "id": "11111111-1111-1111-1111-111111111111",
  "method": "authenticateWithPassword",
  "params": {
    "username": "admin",
    "password": "admin"
  }
}
```

Sau khi login, cac request add/update/delete component phai boc trong `edgeRpc`.

Edge-ID cua Edge local hien tai:

```text
0
```

## 2. Format chung de add device

```json
{
  "jsonrpc": "2.0",
  "id": "22222222-2222-2222-2222-222222222222",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "22222222-2222-2222-2222-222222222223",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "<FACTORY_PID>",
        "properties": [
          { "name": "id", "value": "<componentId>" },
          { "name": "alias", "value": "<display name>" },
          { "name": "enabled", "value": true }
        ]
      }
    }
  }
}
```

Luu y:

- `id` JSON-RPC phai la UUID hop le.
- `factoryPid` phai dung PID trong code.
- Ten property co the gui dang `modbus_id` hoac `modbus.id`; server se chuyen `_` thanh `.` khi luu ConfigAdmin.
- Meter dung field `type`.
- PV-Inverter Cluster dung field `meterType`.
- PV-Inverter Cluster dung Factory PID `PvInverter.Cluster`, khong phai `PV-Inverter.Cluster`.

## 3. Add Bridge Modbus TCP

Dung khi device ket noi qua Ethernet/TCP.

Factory PID:

```text
Bridge.Modbus.Tcp
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "30000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "30000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Bridge.Modbus.Tcp",
        "properties": [
          { "name": "id", "value": "modbus0" },
          { "name": "alias", "value": "Modbus TCP Main" },
          { "name": "enabled", "value": true },
          { "name": "ip", "value": "192.168.1.101" },
          { "name": "port", "value": 502 },
          { "name": "invalidateElementsAfterReadErrors", "value": 5 }
        ]
      }
    }
  }
}
```

## 4. Add Bridge Modbus RTU

Dung khi device ket noi qua RS485/Serial.

Factory PID:

```text
Bridge.Modbus.Serial
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "31000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "31000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Bridge.Modbus.Serial",
        "properties": [
          { "name": "id", "value": "modbus0" },
          { "name": "alias", "value": "Modbus RTU Main" },
          { "name": "enabled", "value": true },
          { "name": "portName", "value": "/dev/ttyUSB0" },
          { "name": "baudRate", "value": 9600 },
          { "name": "databits", "value": 8 },
          { "name": "stopbits", "value": "ONE" },
          { "name": "parity", "value": "NONE" },
          { "name": "invalidateElementsAfterReadErrors", "value": 5 }
        ]
      }
    }
  }
}
```

## 5. Add Meter

Meter luoi chinh nen dung:

```text
id = meter0
type = GRID
```

Vi du Selec MFM383:

Factory PID:

```text
Meter.Selec.MFM383
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "40000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "40000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Meter.Selec.MFM383",
        "properties": [
          { "name": "id", "value": "meter0" },
          { "name": "alias", "value": "Grid Meter" },
          { "name": "enabled", "value": true },
          { "name": "type", "value": "GRID" },
          { "name": "invert", "value": false },
          { "name": "readRegisterType", "value": "INPUT_REGISTERS" },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 }
        ]
      }
    }
  }
}
```

Meter type hay dung:

```text
GRID
PRODUCTION
PRODUCTION_AND_CONSUMPTION
CONSUMPTION_METERED
CONSUMPTION_NOT_METERED
MANAGED_CONSUMPTION_METERED
```

Quy uoc:

- Meter diem dau noi luoi: `GRID`
- Meter do PV rieng: `PRODUCTION`
- Meter do tai rieng: `CONSUMPTION_METERED` hoac `CONSUMPTION_NOT_METERED`

## 6. Add PV-Inverter

Vi du Sungrow SG110CX.

Factory PID:

```text
PV-Inverter.Sungrow.SG110CX
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "50000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "50000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "PV-Inverter.Sungrow.SG110CX",
        "properties": [
          { "name": "id", "value": "pvInverter1" },
          { "name": "alias", "value": "Sungrow SG110CX 1" },
          { "name": "enabled", "value": true },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 }
        ]
      }
    }
  }
}
```

Quy uoc:

- Neu chi co 1 inverter: co the dung `pvInverter0`.
- Neu co nhieu inverter: nen dung inverter con `pvInverter1`, `pvInverter2`, ... va tao Cluster la `pvInverter0`.

## 7. Add PV-Inverter Cluster

Dung khi site co nhieu PV inverter va can gop thanh mot component tong.

Factory PID dung:

```text
PvInverter.Cluster
```

Khong dung:

```text
PV-Inverter.Cluster
```

Field cau hinh dung:

```text
id
alias
enabled
meterType
addToSum
pvInverter.ids
```

Goi WS add cluster:

```json
{
  "jsonrpc": "2.0",
  "id": "60000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "60000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "PvInverter.Cluster",
        "properties": [
          { "name": "id", "value": "pvInverter0" },
          { "name": "alias", "value": "PV Cluster" },
          { "name": "enabled", "value": true },
          { "name": "meterType", "value": "PRODUCTION" },
          { "name": "addToSum", "value": true },
          { "name": "pvInverter.ids", "value": ["pvInverter1", "pvInverter2"] }
        ]
      }
    }
  }
}
```

Neu update cluster da co:

```json
{
  "jsonrpc": "2.0",
  "id": "61000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "61000000-0000-0000-0000-000000000001",
      "method": "updateComponentConfig",
      "params": {
        "componentId": "pvInverter0",
        "properties": [
          { "name": "meterType", "value": "PRODUCTION" },
          { "name": "addToSum", "value": true },
          { "name": "pvInverter.ids", "value": ["pvInverter1", "pvInverter2", "pvInverter3"] }
        ]
      }
    }
  }
}
```

Nguyen tac de khong bi cong trung:

- Cac inverter con doc du lieu rieng: `pvInverter1`, `pvInverter2`, ...
- Cluster tong: `pvInverter0`
- Chi cluster nen `addToSum = true`.
- Controller/BE nen dung `pvInverter0` la PV tong.

## 8. Add ESS / BESS

Vi du ESS OMNI 430 PCS.

Factory PID:

```text
Ess.Omni430Pcs
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "70000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "70000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Ess.Omni430Pcs",
        "properties": [
          { "name": "id", "value": "ess0" },
          { "name": "alias", "value": "OMNI 430 PCS" },
          { "name": "enabled", "value": true },
          { "name": "readOnly", "value": true },
          { "name": "deviceActivePowerSign", "value": "POSITIVE_CHARGE" },
          { "name": "capacity", "value": 261000 },
          { "name": "maxChargePower", "value": 430000 },
          { "name": "maxDischargePower", "value": 430000 },
          { "name": "maxApparentPower", "value": 430000 },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "adjust_modbus_id", "value": "modbus1" },
          { "name": "modbusUnitId", "value": 1 }
        ]
      }
    }
  }
}
```

Luu y:

- `readOnly = true`: chi doc monitoring, khong ghi thanh ghi dieu khien.
- `readOnly = false`: cho phep controller ghi len PCS/BESS.
- OMNI 430 co 2 bridge: `modbus_id` cho do/monitoring va `adjust_modbus_id` cho dieu khien.
- `deviceActivePowerSign = POSITIVE_CHARGE`: device bao duong la sac, am la xa. Edge se xu ly theo logic OpenEMS cho monitoring/sum.

## 9. Add Sensor Huawei Irradiance

Factory PID:

```text
Sensor.Huawei.Irradiance
```

Goi WS:

```json
{
  "jsonrpc": "2.0",
  "id": "80000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "80000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Sensor.Huawei.Irradiance",
        "properties": [
          { "name": "id", "value": "sensor0" },
          { "name": "alias", "value": "Huawei Irradiance Sensor" },
          { "name": "enabled", "value": true },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 }
        ]
      }
    }
  }
}
```

Payload BE du kien:

```text
sensor0/DailyIrradiation
sensor0/TotalIrradiance
sensor0/ErrorCode1
sensor0/ErrorCode2
sensor0/ErrorCode3
sensor0/ModbusCommunicationFailed
sensor0/State
```

## 10. Update component da co

Vi du update Meter:

```json
{
  "jsonrpc": "2.0",
  "id": "90000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "90000000-0000-0000-0000-000000000001",
      "method": "updateComponentConfig",
      "params": {
        "componentId": "meter0",
        "properties": [
          { "name": "type", "value": "GRID" },
          { "name": "invert", "value": false }
        ]
      }
    }
  }
}
```

## 11. Delete component

```json
{
  "jsonrpc": "2.0",
  "id": "a0000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "a0000000-0000-0000-0000-000000000001",
      "method": "deleteComponentConfig",
      "params": {
        "componentId": "pvInverter1"
      }
    }
  }
}
```

## 12. Checklist loi hay gap

| Van de | Nguyen nhan hay gap | Cach xu ly |
| --- | --- | --- |
| Khong tao duoc component | Sai `factoryPid` | Kiem tra PID trong ConfigMgr/source |
| Cluster PV khong tao duoc | Dung sai PID `PV-Inverter.Cluster` | Dung `PvInverter.Cluster` |
| Cluster khong gom inverter | Sai `pvInverter.ids` | Dung dung ID inverter con |
| Meter khong len sum dung | Sai `type` | Grid meter phai la `GRID` |
| ModbusCommunicationFailed = 1 | Sai IP/port/unitId/RS485 | Kiem tra bridge va unit id |
| BESS khong dieu khien | `readOnly = true` hoac chua co Scheduler | Set `readOnly = false`, add Controller + Scheduler |
| BE khong co data moi | Backend chua connected hoac ID sai prefix | Check `ctrlBackend0/State` va `BACKEND_PAYLOAD` |
