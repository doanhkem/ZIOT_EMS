# ZIOT Control Configuration Guide

Tai lieu nay huong dan cau hinh dieu khien tren OpenEMS Edge hien tai.

Pham vi:

- He PV: PV inverter + grid meter.
- He Hybrid: PV inverter + grid meter + BESS.

Quy uoc dau cong suat:

```text
Grid meter ActivePower > 0  : mua dien tu luoi
Grid meter ActivePower < 0  : phat dien len luoi

ESS ActivePower > 0         : BESS xa
ESS ActivePower < 0         : BESS sac

PV ActivePower > 0          : PV dang phat
```

## 1. He PV

### 1.1 Muc tieu dieu khien

He PV chi co:

```text
PV inverter
Grid meter
```

Khong co BESS, nen Edge chi co the dieu khien PV inverter de gioi han cong suat phat len luoi.

Mode phu hop:

```text
Limit Sell To Grid / Zero Export cho PV
```

Logic:

```text
PV uu tien cap tai.
Neu PV du va co phat len luoi qua muc cho phep -> cat PV.
Neu tai tang hoac het phat luoi -> release PV len lai MaxActivePower.
```

### 1.2 Controller can tao

Tao controller:

```text
Factory PID: Controller.PvInverter.SellToGridLimit
Component ID: ctrlPvInverterSellToGridLimit0
```

Thong so chinh:

| Field | Gia tri mau | Ghi chu |
| --- | --- | --- |
| `pvInverter_id` | `pvInverter0` | ID PV inverter hoac PV cluster |
| `meter_id` | `meter0` | Grid meter |
| `maximumSellToGridPower` | `0` | Zero export tuyet doi |
| `asymmetricMode` | `false` | Thuong dung false cho 3 pha doi xung |

WS add controller:

```json
{
  "jsonrpc": "2.0",
  "id": "10000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "10000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Controller.PvInverter.SellToGridLimit",
        "properties": [
          { "name": "id", "value": "ctrlPvInverterSellToGridLimit0" },
          { "name": "alias", "value": "PV Zero Export" },
          { "name": "enabled", "value": true },
          { "name": "pvInverter_id", "value": "pvInverter0" },
          { "name": "meter_id", "value": "meter0" },
          { "name": "asymmetricMode", "value": false },
          { "name": "maximumSellToGridPower", "value": 0 }
        ]
      }
    }
  }
}
```

Neu cho phep phat len luoi 5 kW:

```text
maximumSellToGridPower = 5000
```

### 1.3 Scheduler

Dung scheduler:

```text
Factory PID: Scheduler.FixedOrder
Component ID: scheduler0
```

Danh sach controller:

```text
controllers.ids = [
  "ctrlPvInverterSellToGridLimit0"
]
```

WS add scheduler:

```json
{
  "jsonrpc": "2.0",
  "id": "11000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "11000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Scheduler.FixedOrder",
        "properties": [
          { "name": "id", "value": "scheduler0" },
          { "name": "alias", "value": "Scheduler" },
          { "name": "enabled", "value": true },
          { "name": "controllers.ids", "value": ["ctrlPvInverterSellToGridLimit0"] }
        ]
      }
    }
  }
}
```

### 1.4 Vi du hoat dong

Vi du 1: PV du, can cat PV.

```text
Load = 100 kW
PV = 180 kW
Grid = -80 kW
maximumSellToGridPower = 0
```

Ket qua:

```text
PV limit ve khoang 100 kW
Grid ve gan 0 kW
```

Vi du 2: Tai tang, khong con phat luoi.

```text
Load = 160 kW
PV dang bi limit = 100 kW
Grid = +60 kW
```

Ket qua:

```text
PV limit duoc keo len lai theo logic controller.
PV tang len neu inverter co kha nang phat.
```

### 1.5 Log van hanh

Controller se in mot dong log de nhin nhanh trang thai tinh toan:

```text
CTRL_WRITE_OK ⚡ Grid=133.59 kW | 🏭 Load=133.59 kW | ☀ PV=0.00 kW | 🎯 Limit=0.00 kW | ✍ Write=100.00% -> pvInverter0
```

Y nghia:

| Field | Ghi chu |
| --- | --- |
| `Grid` | Gia tri grid meter. Duong la mua tu luoi, am la phat len luoi |
| `Load` | Uoc tinh tai, bang `Grid + PV` |
| `PV` | Cong suat PV hien tai |
| `Limit` | `maximumSellToGridPower` |
| `Write` | Setpoint controller gui cho PV inverter |

Neu can xac nhan Modbus write that su thanh cong, xem marker:

```text
ZIOT_WRITE_OK component=pvInverter0 tag=ActivePowerLimitFixed channel=SetActivePowerLimitPercent fc=FC6 offset=5402 size=1
```

Lenh grep tren IOT:

```bash
docker logs --since 10m openems-edge 2>&1 | grep -E "CTRL_WRITE_OK|ZIOT_WRITE_OK"
```

## 2. He Hybrid

### 2.1 Muc tieu dieu khien

He Hybrid gom:

```text
PV inverter hoac PV cluster
Grid meter
BESS
```

Nen dung controller tong:

```text
Factory PID: Controller.Hybrid.PvEss
Component ID: ctrlHybridPvEss0
```

Controller nay dung chung cho moi thiet bi da them neu device implement dung nature OpenEMS:

| Device | Nature can co |
| --- | --- |
| BESS | `ManagedSymmetricEss` |
| Meter grid | `ElectricityMeter` |
| PV inverter/cluster | `ManagedSymmetricPvInverter` |

Khong nen chay dong thoi cac controller cu nhu:

```text
ctrlBalancing0
ctrlEssSellToGridLimit0
ctrlPvInverterSellToGridLimit0
ctrlPeakShaving0
```

voi `ctrlHybridPvEss0`, vi de tranh nhieu controller cung ghi setpoint len ESS/PV.

### 2.2 Controller can tao

Tao:

```text
Factory PID: Controller.Hybrid.PvEss
Component ID: ctrlHybridPvEss0
```

Thong so chung:

| Field | Gia tri mau | Ghi chu |
| --- | --- | --- |
| `mode` | `ZERO_EXPORT` / `PEAK_SHAVING` / `TOU` | Mode van hanh |
| `ess_id` | `ess0` | BESS |
| `meter_id` | `meter0` | Grid meter |
| `pvInverter_id` | `pvInverter0` | PV inverter hoac PV cluster |
| `targetGridSetpoint` | `0` | Muc tieu grid trong zero export |
| `maximumSellToGridPower` | `0` | Gioi han phat luoi |
| `maxChargePower` | `0` | 0 = dung limit thiet bi |
| `maxDischargePower` | `0` | 0 = dung limit thiet bi |
| `minSoc` | `10` | Chan xa khi SOC qua thap |
| `maxSoc` | `95` | Chan sac khi SOC dat nguong |
| `enablePvCurtailment` | `true` | Cho phep cat PV khi can zero export |
| `fallbackPvReleasePower` | `0` | Dung khi PV khong co MaxActivePower |

WS add controller:

```json
{
  "jsonrpc": "2.0",
  "id": "20000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "20000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Controller.Hybrid.PvEss",
        "properties": [
          { "name": "id", "value": "ctrlHybridPvEss0" },
          { "name": "alias", "value": "Hybrid PV ESS" },
          { "name": "enabled", "value": true },
          { "name": "mode", "value": "ZERO_EXPORT" },
          { "name": "ess_id", "value": "ess0" },
          { "name": "meter_id", "value": "meter0" },
          { "name": "pvInverter_id", "value": "pvInverter0" },
          { "name": "targetGridSetpoint", "value": 0 },
          { "name": "maximumSellToGridPower", "value": 0 },
          { "name": "peakShavingPower", "value": 300000 },
          { "name": "rechargePower", "value": 0 },
          { "name": "maxChargePower", "value": 0 },
          { "name": "maxDischargePower", "value": 0 },
          { "name": "fallbackPvReleasePower", "value": 0 },
          { "name": "minSoc", "value": 10 },
          { "name": "maxSoc", "value": 95 },
          { "name": "enablePvCurtailment", "value": true }
        ]
      }
    }
  }
}
```

Neu PV inverter/cluster khong co `MaxActivePower`, nen set:

```text
fallbackPvReleasePower = cong_suat_PV_toi_da_W
```

Vi du PV 110 kW:

```text
fallbackPvReleasePower = 110000
```

### 2.3 Scheduler

Dung:

```text
Factory PID: Scheduler.FixedOrder
Component ID: scheduler0
```

Danh sach controller:

```text
controllers.ids = [
  "ctrlHybridPvEss0"
]
```

WS add scheduler:

```json
{
  "jsonrpc": "2.0",
  "id": "21000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "21000000-0000-0000-0000-000000000001",
      "method": "createComponentConfig",
      "params": {
        "factoryPid": "Scheduler.FixedOrder",
        "properties": [
          { "name": "id", "value": "scheduler0" },
          { "name": "alias", "value": "Scheduler" },
          { "name": "enabled", "value": true },
          { "name": "controllers.ids", "value": ["ctrlHybridPvEss0"] }
        ]
      }
    }
  }
}
```

Neu can giu SOC du phong rieng, co the them reserve truoc hybrid:

```text
controllers.ids = [
  "ctrlEmergencyCapacityReserve0",
  "ctrlHybridPvEss0"
]
```

### 2.4 Mode ZERO_EXPORT

Cau hinh:

```text
mode = ZERO_EXPORT
targetGridSetpoint = 0
maximumSellToGridPower = 0
enablePvCurtailment = true
```

WS update sang Zero Export:

```json
{
  "jsonrpc": "2.0",
  "id": "22000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "22000000-0000-0000-0000-000000000001",
      "method": "updateComponentConfig",
      "params": {
        "componentId": "ctrlHybridPvEss0",
        "properties": [
          { "name": "mode", "value": "ZERO_EXPORT" },
          { "name": "targetGridSetpoint", "value": 0 },
          { "name": "maximumSellToGridPower", "value": 0 },
          { "name": "enablePvCurtailment", "value": true }
        ]
      }
    }
  }
}
```

Logic:

```text
PV thieu -> BESS xa bam tai neu SOC cho phep.
PV du -> BESS sac truoc.
BESS day hoac cham gioi han sac -> cat PV.
Khong con export -> release PV len MaxActivePower.
```

Vi du PV thieu:

```text
Load = 150 kW
PV = 100 kW
Grid = +50 kW
BESS = 0 kW
```

Ket qua:

```text
BESS xa 50 kW
Grid ve gan 0 kW
PV release len MaxActivePower
```

Vi du PV du, BESS sac du:

```text
Load = 100 kW
PV = 180 kW
Grid = -80 kW
BESS con sac duoc 100 kW
```

Ket qua:

```text
BESS sac 80 kW
Khong cat PV
Grid ve gan 0 kW
```

Vi du PV du, BESS chi sac duoc 50 kW:

```text
Load = 100 kW
PV = 180 kW
Grid = -80 kW
BESS chi sac duoc 50 kW
```

Ket qua:

```text
BESS sac 50 kW
PV bi cat khoang 30 kW
Grid ve gan 0 kW
```

### 2.5 Mode PEAK_SHAVING

Cau hinh:

```text
mode = PEAK_SHAVING
peakShavingPower = 300000
rechargePower = 0
enablePvCurtailment = true
```

WS update sang Peak Shaving:

```json
{
  "jsonrpc": "2.0",
  "id": "23000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "23000000-0000-0000-0000-000000000001",
      "method": "updateComponentConfig",
      "params": {
        "componentId": "ctrlHybridPvEss0",
        "properties": [
          { "name": "mode", "value": "PEAK_SHAVING" },
          { "name": "peakShavingPower", "value": 300000 },
          { "name": "rechargePower", "value": 0 },
          { "name": "enablePvCurtailment", "value": true }
        ]
      }
    }
  }
}
```

Logic:

```text
Grid buy > peakShavingPower -> BESS xa de cat dinh.
Grid buy < rechargePower -> BESS sac.
Nam giua 2 nguong -> BESS khong can thiep.
Neu con export sau gioi han ESS -> cat PV neu enablePvCurtailment = true.
```

Nen set:

```text
rechargePower = 0
```

neu khong muon BESS mua them luoi de sac.

Vi du cat dinh:

```text
Grid neu khong co BESS = 420 kW
peakShavingPower = 300 kW
```

Ket qua:

```text
BESS xa 120 kW
Grid con khoang 300 kW
```

Vi du PV du:

```text
Grid = -80 kW
rechargePower = 0
```

Ket qua:

```text
BESS sac 80 kW neu con kha nang sac.
Neu BESS day hoac cham maxChargePower -> cat PV de khong export.
```

### 2.6 Mode TOU

Cau hinh:

```text
mode = TOU
touScheduleJson = [...]
```

Quy tac:

```text
Toi da 20 slot.
Toi da 10 slot CHARGE va 10 slot DISCHARGE.
Khong cho overlap slot enabled.
Moi thoi diem chi co 1 slot duy nhat.
Neu khong co slot active -> chay SELF_CONSUMPTION_ZERO_EXPORT.
```

Field cua slot:

| Field | Bat buoc | Ghi chu |
| --- | --- | --- |
| `enabled` | Khong | Default `true` |
| `name` | Khong | Ten slot |
| `type` | Co | `CHARGE` hoac `DISCHARGE` |
| `start` | Co | Vi du `22:00` |
| `end` | Co | Vi du `04:00`, ho tro qua dem |
| `power` | Co | W |
| `chargeSource` | Voi CHARGE | `ANY`, `PV_SURPLUS`, `PV_ALL` |
| `minSoc` | Khong | Override minSoc chung |
| `maxSoc` | Khong | Override maxSoc chung |

Charge source:

| Source | Logic |
| --- | --- |
| `ANY` | Sac theo `power`, co the lay tu grid/PV |
| `PV_SURPLUS` | Chi sac bang PV du sau khi cap tai |
| `PV_ALL` | Uu tien dung PV hien tai de sac BESS |

Discharge:

```text
Luon xa bam tai.
Toi da bang power cau hinh.
Khong xa phat nguoc luoi.
PV duoc release len MaxActivePower neu khong gay export.
Neu PV du gay export va BESS khong sac trong discharge slot -> cat PV de giu zero export.
```

Vi du TOU:

```json
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
  },
  {
    "enabled": true,
    "name": "Charge solar surplus",
    "type": "CHARGE",
    "start": "08:00",
    "end": "17:00",
    "chargeSource": "PV_SURPLUS",
    "power": 400000,
    "maxSoc": 95
  },
  {
    "enabled": true,
    "name": "Discharge evening",
    "type": "DISCHARGE",
    "start": "17:00",
    "end": "22:00",
    "power": 400000,
    "minSoc": 20
  }
]
```

WS update sang TOU:

```json
{
  "jsonrpc": "2.0",
  "id": "24000000-0000-0000-0000-000000000000",
  "method": "edgeRpc",
  "params": {
    "edgeId": "0",
    "payload": {
      "jsonrpc": "2.0",
      "id": "24000000-0000-0000-0000-000000000001",
      "method": "updateComponentConfig",
      "params": {
        "componentId": "ctrlHybridPvEss0",
        "properties": [
          { "name": "mode", "value": "TOU" },
          {
            "name": "touScheduleJson",
            "value": "[{\"enabled\":true,\"name\":\"Charge night\",\"type\":\"CHARGE\",\"start\":\"22:00\",\"end\":\"04:00\",\"chargeSource\":\"ANY\",\"power\":100000,\"maxSoc\":95},{\"enabled\":true,\"name\":\"Discharge morning\",\"type\":\"DISCHARGE\",\"start\":\"04:00\",\"end\":\"08:00\",\"power\":400000,\"minSoc\":20},{\"enabled\":true,\"name\":\"Charge solar surplus\",\"type\":\"CHARGE\",\"start\":\"08:00\",\"end\":\"17:00\",\"chargeSource\":\"PV_SURPLUS\",\"power\":400000,\"maxSoc\":95},{\"enabled\":true,\"name\":\"Discharge evening\",\"type\":\"DISCHARGE\",\"start\":\"17:00\",\"end\":\"22:00\",\"power\":400000,\"minSoc\":20}]"
          }
        ]
      }
    }
  }
}
```

Ket qua:

```text
22:00-04:00: sac 100 kW, co the lay tu grid.
04:00-08:00: xa bam tai, toi da 400 kW.
08:00-17:00: chi sac bang PV du, toi da 400 kW.
17:00-22:00: xa bam tai, toi da 400 kW.
```

### 2.7 Checklist van hanh Hybrid

| Van de | Can kiem tra |
| --- | --- |
| BESS khong dieu khien | `readOnly = false`, scheduler co `ctrlHybridPvEss0` |
| PV khong release len | PV co `MaxActivePower` hoac set `fallbackPvReleasePower` |
| Van con export | `enablePvCurtailment = true`, PV inverter ho tro active power limit |
| TOU khong activate | Kiem tra JSON, overlap slot, qua 20 slot |
| BESS khong xa | SOC <= `minSoc` hoac device discharge limit = 0 |
| BESS khong sac | SOC >= `maxSoc` hoac device charge limit = 0 |
