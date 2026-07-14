# ZIOT EMS Documentation

Thu muc nay gom cac tai lieu van hanh va tich hop rieng cho ZIOT EMS Edge.

## Tai lieu chinh

| File | Muc dich |
| --- | --- |
| `../README.md` | README chinh: build, run, deploy IOT2050, Docker, MQTT, log |
| `ZIOT-GENERIC-EDGE-ARCHITECTURE.md` | Kien truc ZIOT Generic device, mapping, Modbus task |
| `ZIOT-DEVICE-CONFIGURATION-GUIDE.md` | Cach them model/device vao `deviceConfig_openems_fields.conf` |
| `ZIOT-CONTROL-CONFIGURATION-GUIDE.md` | Cau hinh controller PV/BESS, Sell-to-Grid, Hybrid, TOU |
| `ZIOT_MOBILE_DEVICE_CONTROL_REQUIREMENTS.md` | Requirement mobile app cho dieu khien thiet bi |

## Log quan trong

Xem nhanh controller PV Sell-to-Grid:

```bash
docker logs --since 10m openems-edge 2>&1 | grep CTRL_WRITE_OK
```

Xac nhan Modbus write thanh cong:

```bash
docker logs --since 10m openems-edge 2>&1 | grep ZIOT_WRITE_OK
```

Vi du:

```text
CTRL_WRITE_OK ⚡ Grid=133.59 kW | 🏭 Load=133.59 kW | ☀ PV=0.00 kW | 🎯 Limit=0.00 kW | ✍ Write=100.00% -> pvInverter0
ZIOT_WRITE_OK component=pvInverter0 tag=ActivePowerLimitFixed channel=SetActivePowerLimitPercent fc=FC6 offset=5402 size=1
```

## Mapping ghi nho

```text
read_registers       -> FC3
read_input_registers -> FC4
watch_events         -> FC4 neu model co read_input_registers, nguoc lai FC3
write_registers      -> FC6 voi size=1, FC16 voi multi-register
```

## MQTT

MQTT gom:

```text
Bridge.Mqtt
Controller.Api.MQTT
Controller.MQTT.Telemetry
```

`Controller.MQTT.Telemetry` tu phan biet meter va inverter:

```text
Meter       -> kiloWatts, kWH, data, timeStamp
PV inverter -> telemetry day du cua inverter
```
