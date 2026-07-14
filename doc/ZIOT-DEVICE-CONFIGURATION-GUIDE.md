# ZIOT OpenEMS Edge - Device Configuration Guide

Tai lieu nay mo ta quy trinh add device trong Edge hien tai cua ZIOT/FEMS, gom:

- Add Bridge Modbus TCP/RTU
- Add ZIOT Generic Meter, PV-Inverter, ESS/BESS, Sensor
- Add PV-Inverter Cluster khi co nhieu bien tan
- Goi JSON-RPC WebSocket dung de BE/UI tao cau hinh

Neu khai bao device ZIOT Generic, dung cac Factory PID `Ziot.Generic.*` o muc 5. Cac muc legacy phia sau chi de tham khao khi can dung component OpenEMS goc.

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
- ZIOT Generic dung field `mappingFile` va `model` de map vao `deviceConfig_openems_fields.conf`.

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

## 5. Add ZIOT Generic Device

Day la cach khai bao chuan cho device ZIOT Generic. Tat ca device se doc mapping tu file:

```text
outputs/deviceConfig_openems_fields.conf
```

### 5.1 Factory PID

| Loai device | Factory PID | Component ID goi y |
| --- | --- | --- |
| Meter | `Ziot.Generic.Meter` | `meter0`, `meter1` |
| PV-Inverter | `Ziot.Generic.PvInverter` | `pvInverter0`, `pvInverter1` |
| ESS/BESS | `Ziot.Generic.Ess` | `ess0`, `ess1` |
| Sensor | `Ziot.Generic.Sensor` | `sensor0`, `sensor1` |

### 5.2 Property chung

| Property | Bat buoc | Ghi chu |
| --- | --- | --- |
| `id` | Co | Component ID trong Edge |
| `alias` | Khong | Ten hien thi |
| `enabled` | Co | `true` de chay |
| `modbus_id` | Co | Bridge Modbus da tao, vi du `modbus0` |
| `modbusUnitId` | Co | Unit ID/Slave ID cua device |
| `mappingFile` | Co | Thuong la `outputs/deviceConfig_openems_fields.conf` |
| `model` | Co | Gia tri enum tren ConfigMgr, map toi key `MODEL/...` trong file conf |

Luu y ve `model`:

- Tren ConfigMgr, `model` la enum da co trong code, vi du `PV_INVERTER_SUNGROW_SG110CX`.
- Trong file conf, enum nay map toi key that, vi du `MODEL/PV-Inverter.Sungrow.SG110CX`.
- Neu chi sua `offSet`, `PF`, `unit`, `dataType`, `wordOrder`, `byteOrder` cho model da co thi chi can sua file conf va restart/reload Edge.
- Neu them model hoan toan moi chua co trong dropdown ConfigMgr thi phai them enum trong code, build image/jar moi, roi moi chon duoc tren UI.

### 5.3 ZIOT Generic Meter

Factory PID:

```text
Ziot.Generic.Meter
```

Property rieng:

| Property | Gia tri | Ghi chu |
| --- | --- | --- |
| `type` | `GRID`, `PRODUCTION`, `CONSUMPTION`, ... | Vai tro meter trong OpenEMS Sum |
| `invert` | `true`/`false` | Dao dau active/reactive power |
| `useCtRatio` | `true`/`false` | Bat nhan CT ratio |
| `ctRatio` | So thuc | He so CT, mac dinh `1.0` |
| `usePtRatio` | `true`/`false` | Bat nhan PT ratio |
| `ptRatio` | So thuc | He so PT, mac dinh `1.0` |

Vi du:

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
        "factoryPid": "Ziot.Generic.Meter",
        "properties": [
          { "name": "id", "value": "meter0" },
          { "name": "alias", "value": "Grid Meter" },
          { "name": "enabled", "value": true },
          { "name": "type", "value": "GRID" },
          { "name": "invert", "value": false },
          { "name": "useCtRatio", "value": false },
          { "name": "ctRatio", "value": 1.0 },
          { "name": "usePtRatio", "value": false },
          { "name": "ptRatio", "value": 1.0 },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 },
          { "name": "mappingFile", "value": "outputs/deviceConfig_openems_fields.conf" },
          { "name": "model", "value": "METER_SELEC_MFM383" }
        ]
      }
    }
  }
}
```

### 5.4 ZIOT Generic PV-Inverter

Factory PID:

```text
Ziot.Generic.PvInverter
```

Property rieng:

| Property | Gia tri | Ghi chu |
| --- | --- | --- |
| `maxApparentPower` | W/VA | Cong suat bieu kien lon nhat de controller tinh gioi han |

Vi du:

```json
{
  "jsonrpc": "2.0",
  "id": "51000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "51000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Ziot.Generic.PvInverter",
        "properties": [
          { "name": "id", "value": "pvInverter0" },
          { "name": "alias", "value": "PV Inverter 1" },
          { "name": "enabled", "value": true },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 },
          { "name": "mappingFile", "value": "outputs/deviceConfig_openems_fields.conf" },
          { "name": "model", "value": "PV_INVERTER_SUNGROW_SG110CX" },
          { "name": "maxApparentPower", "value": 110000 }
        ]
      }
    }
  }
}
```

### 5.5 ZIOT Generic ESS/BESS

Factory PID:

```text
Ziot.Generic.Ess
```

Property rieng:

| Property | Gia tri | Ghi chu |
| --- | --- | --- |
| `readOnly` | `true`/`false` | `false` neu cho controller ghi setpoint |
| `activePowerSign` | `POSITIVE_IS_DISCHARGE` hoac `POSITIVE_IS_CHARGE` | Quy uoc dau cua thanh ghi device |
| `capacity` | Wh | Dung cho SOC/capacity |
| `maxApparentPower` | VA | Cong suat bieu kien lon nhat |
| `maxChargePower` | W | Gioi han sac |
| `maxDischargePower` | W | Gioi han xa |

Vi du:

```json
{
  "jsonrpc": "2.0",
  "id": "52000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "52000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Ziot.Generic.Ess",
        "properties": [
          { "name": "id", "value": "ess0" },
          { "name": "alias", "value": "BESS 1" },
          { "name": "enabled", "value": true },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 },
          { "name": "mappingFile", "value": "outputs/deviceConfig_openems_fields.conf" },
          { "name": "model", "value": "ESS_OMNI261" },
          { "name": "readOnly", "value": false },
          { "name": "activePowerSign", "value": "POSITIVE_IS_CHARGE" },
          { "name": "capacity", "value": 261000 },
          { "name": "maxApparentPower", "value": 430000 },
          { "name": "maxChargePower", "value": 430000 },
          { "name": "maxDischargePower", "value": 430000 }
        ]
      }
    }
  }
}
```

### 5.6 ZIOT Generic Sensor

Factory PID:

```text
Ziot.Generic.Sensor
```

Vi du:

```json
{
  "jsonrpc": "2.0",
  "id": "53000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "53000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Ziot.Generic.Sensor",
        "properties": [
          { "name": "id", "value": "sensor0" },
          { "name": "alias", "value": "Irradiance Sensor" },
          { "name": "enabled", "value": true },
          { "name": "modbus_id", "value": "modbus0" },
          { "name": "modbusUnitId", "value": 1 },
          { "name": "mappingFile", "value": "outputs/deviceConfig_openems_fields.conf" },
          { "name": "model", "value": "SENSOR_HUAWEI_IRRADIANCE" }
        ]
      }
    }
  }
}
```

## 6. Add Legacy Meter

Day la cach add component Meter goc cua OpenEMS. Neu dang dung ZIOT Generic thi uu tien muc 5.3.

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

## 7. Add Legacy PV-Inverter

Day la cach add component PV-Inverter goc cua OpenEMS. Neu dang dung ZIOT Generic thi uu tien muc 5.4.

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

## 8. Add PV-Inverter Cluster

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

## 9. Add Legacy ESS / BESS

Day la cach add component ESS goc cua OpenEMS. Neu dang dung ZIOT Generic thi uu tien muc 5.5.

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

## 10. Add Legacy Sensor Huawei Irradiance

Day la cach add component Sensor goc cua OpenEMS. Neu dang dung ZIOT Generic thi uu tien muc 5.6.

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

## 11. Update component da co

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

## 12. Delete component

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

## 13. Checklist loi hay gap

| Van de | Nguyen nhan hay gap | Cach xu ly |
| --- | --- | --- |
| Khong tao duoc component | Sai `factoryPid` | Kiem tra PID trong ConfigMgr/source |
| Khong thay model ZIOT moi trong dropdown | Moi them key trong file conf, chua them enum trong code | Them enum vao `ConfigMeter/ConfigPvInverter/ConfigEss/ConfigSensor`, build lai |
| Cluster PV khong tao duoc | Dung sai PID `PV-Inverter.Cluster` | Dung `PvInverter.Cluster` |
| Cluster khong gom inverter | Sai `pvInverter.ids` | Dung dung ID inverter con |
| Meter khong len sum dung | Sai `type` | Grid meter phai la `GRID` |
| ModbusCommunicationFailed = 1 | Sai IP/port/unitId/RS485 | Kiem tra bridge va unit id |
| BESS khong dieu khien | `readOnly = true` hoac chua co Scheduler | Set `readOnly = false`, add Controller + Scheduler |
| BE khong co data moi | Backend chua connected hoac ID sai prefix | Check `ctrlBackend0/State` va `BACKEND_PAYLOAD` |

## 14. Mapping task trong `deviceConfig_openems_fields.conf`

Khi them model moi, cac field trong `tasks` duoc Edge doc theo quy tac:

| Task | Function code | Ghi chu |
| --- | --- | --- |
| `read_registers` | FC3 | Holding registers |
| `read_input_registers` | FC4 | Input registers |
| `watch_events` | Tu dong theo model | FC4 neu model co `read_input_registers`, nguoc lai FC3 |
| `write_registers` | FC6/FC16 | FC6 voi `size = 1`, FC16 voi multi-register |

Quy tac dia chi:

```text
Dung offset Modbus thuc te, khong dung so hien thi 3xxxx/4xxxx trong manual neu manual dung style reference address.
```

Vi du manual ASW150K ghi:

```text
31378 Error message
31379 Warning message
```

Thi khai bao trong Edge:

```json
{"tagName": "ErrorCode1", "unit": "", "offSet": 1377, "dataType": "uint16", "PF": 0, "size": 1}
{"tagName": "ErrorCode2", "unit": "", "offSet": 1378, "dataType": "uint16", "PF": 0, "size": 1}
```

Kiem tra log:

```bash
docker logs --since 10m openems-edge 2>&1 | grep -E "BACKEND_PAYLOAD|CTRL_WRITE_OK|ZIOT_WRITE_OK|Execute failed"
```
