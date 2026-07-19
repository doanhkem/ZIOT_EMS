# ZIOT EMS Edge

**ZIOT EMS Edge by DoanhGn** là bản OpenEMS Edge được tối giản cho hệ sinh thái ZIOT, tập trung vào thiết bị Generic, điều khiển PV/BESS và triển khai nhẹ trên **Siemens IOT2050**.

Mục tiêu triển khai: trên IOT2050 chỉ cần Docker/Docker Compose, không cần cài Java trên host. Image đã đóng sẵn Java runtime, `ziot-edge.jar` và cấu hình runtime cần thiết.

## Lệnh Nhanh Trên Windows

Mở PowerShell tại thư mục dự án:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
```

Build ZIOT Edge:

```powershell
.\build-ziot-edge.bat
```

Chạy Edge sau khi build:

```powershell
java -jar build\ziot-edge.jar
```

Mở giao diện cấu hình:

```text
http://localhost:8080/system/console/configMgr
```

## Tài Liệu Chi Tiết

- [Tổng quan tài liệu ZIOT EMS](doc/README.md)
- [Kiến trúc ZIOT Generic Edge](doc/ZIOT-GENERIC-EDGE-ARCHITECTURE.md)
- [Hướng dẫn khai báo thiết bị ZIOT](doc/ZIOT-DEVICE-CONFIGURATION-GUIDE.md)
- [Hướng dẫn cấu hình điều khiển ZIOT](doc/ZIOT-CONTROL-CONFIGURATION-GUIDE.md)
- [Yêu cầu giao diện điều khiển thiết bị trên mobile](doc/ZIOT_MOBILE_DEVICE_CONTROL_REQUIREMENTS.md)

## Thành Phần Chính

Các bundle thiết bị ZIOT Generic:

```text
Ziot.Generic.Meter
Ziot.Generic.PvInverter
Ziot.Generic.Ess
Ziot.Generic.Sensor
```

Các controller/cluster quan trọng được giữ lại:

```text
Controller.PvInverter.FixPowerLimit
Controller.PvInverter.SellToGridLimit
Controller.Hybrid.PvEss
Controller.Symmetric.LimitActivePower
PV-Inverter Cluster
ESS Cluster
```

Các API/điều khiển mới:

```text
restartDevice
Kênh ghi RestartDevice
Kênh SOH cho payload ESS
```

## Mapping Thiết Bị

Mapping chính:

```text
outputs/deviceConfig_openems_fields.conf
```

Các file template write-control:

```text
outputs/deviceConfig_write_control_kw.conf
outputs/deviceConfig_write_control_percent.conf
```

Trong `write_registers`, field restart đã được chuẩn hóa:

```json
{"tagName": "RestartDevice", "unit": "", "offSet": null, "dataType": null, "PF": null, "size": null}
```

Khi có thông tin thanh ghi restart của thiết bị, điền ví dụ:

```json
{"tagName": "RestartDevice", "unit": "", "offSet": 12345, "dataType": "uint16", "PF": 0, "size": 1}
```

Hiện Generic Modbus write đang hỗ trợ FC6 cho `size = 1`. Nếu thiết bị cần ghi nhiều thanh ghi bằng FC16 thì cần bổ sung thêm.

### watch_events và ErrorCode

`watch_events` dùng để đọc các thanh ghi lỗi/cảnh báo như `ErrorCode1..3`.

Quy tắc function code:

```text
Nếu model có read_input_registers -> watch_events đọc bằng FC4
Nếu model chỉ có read_registers   -> watch_events đọc bằng FC3
```

Ví dụ ASW150K manual ghi `31378/31379`, nhưng khi khai báo Modbus offset trong
Edge thì dùng:

```json
{"tagName": "ErrorCode1", "unit": "", "offSet": 1377, "dataType": "uint16", "PF": 0, "size": 1}
{"tagName": "ErrorCode2", "unit": "", "offSet": 1378, "dataType": "uint16", "PF": 0, "size": 1}
```

Giá trị này sẽ hiển thị trên channel `FaultCode1/2`, và payload BE sẽ gửi
`ErrorCode1/2`.

## BE Gọi Restart Thiết Bị

BE/mobile nên gọi JSON-RPC method `restartDevice`, không cần biết trực tiếp tên channel Modbus.

Restart một thiết bị:

```json
{
  "jsonrpc": "2.0",
  "id": "00000000-0000-0000-0000-000000000001",
  "method": "restartDevice",
  "params": {
    "componentId": "pvInverter0",
    "value": 1
  }
}
```

Restart nhiều thiết bị:

```json
{
  "jsonrpc": "2.0",
  "id": "00000000-0000-0000-0000-000000000002",
  "method": "restartDevice",
  "params": {
    "componentIds": ["pvInverter0", "pvInverter1", "ess0"],
    "value": 1
  }
}
```

Endpoint này ghi một lần xuống channel `RestartDevice`, không ghi lặp theo timeout như `setChannelValue`.

## Chạy Trên IOT2050

Yêu cầu trên IOT2050:

- Docker
- Docker Compose plugin
- Kết nối mạng để pull image

Lệnh triển khai:

```bash
mkdir -p /opt/openems-edge/config /opt/openems-edge/data /opt/openems-edge/outputs
cd /opt/openems-edge
wget -O docker-compose.yml https://raw.githubusercontent.com/doanhkem/ZIOT_EMS/main/docker-compose.iot2050.yml
docker compose pull
docker compose up -d
```

Image mặc định:

```text
doanhnguyen01/ziot:ziot-edge-iot2050
```

Mở Felix Web Console:

```text
http://<IP-IOT2050>:8080/system/console/configMgr
```

Xem log:

```bash
docker logs -f openems-edge
```

Nếu site dùng container name khác, ví dụ `ziot-edge`, thay tên container trong
lệnh log/restart tương ứng.

Restart container:

```bash
docker compose restart openems-edge
```

Cập nhật image thủ công:

```bash
docker compose pull
docker compose up -d
```

Compose có kèm `watchtower` để tự cập nhật image theo label.

## Thư Mục Runtime Trên IOT2050

```text
/opt/openems-edge/config   Felix/OpenEMS config
/opt/openems-edge/data     dữ liệu runtime
/opt/openems-edge/outputs  mapping thiết bị ZIOT Generic
```

Nếu chỉnh mapping trực tiếp trên IOT2050:

```bash
nano /opt/openems-edge/outputs/deviceConfig_openems_fields.conf
docker compose restart openems-edge
```

## Biên Dịch Trên Windows

Yêu cầu:

- Windows 10/11
- JDK 21
- PowerShell
- Repo đặt tại đường dẫn không bị hạn chế quyền ghi

Mở PowerShell tại root dự án:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
```

Build jar ZIOT Edge:

```powershell
.\build-ziot-edge.bat
```

Kết quả mong đợi:

```text
build\ziot-edge.jar
```

Nếu chỉ muốn export app jar từ bndrun:

```powershell
.\gradlew.bat :io.openems.edge.application:export.ZiotEdgeApp --console=plain --warn
```

Kết quả:

```text
io.openems.edge.application\generated\distributions\executable\ZiotEdgeApp.jar
```

Có thể copy sang `build\ziot-edge.jar` nếu cần dùng cùng tên file deploy:

```powershell
Copy-Item `
  "io.openems.edge.application\generated\distributions\executable\ZiotEdgeApp.jar" `
  "build\ziot-edge.jar" `
  -Force
```

## Chạy Edge Trên Windows

Lệnh chạy nhanh sau khi build:

```powershell
java -jar build\ziot-edge.jar
```

Nếu muốn chỉ định riêng thư mục cấu hình/dữ liệu khi chạy local, tạo thư mục runtime:

```powershell
New-Item -ItemType Directory -Force C:\openems\config | Out-Null
New-Item -ItemType Directory -Force C:\openems\data | Out-Null
New-Item -ItemType Directory -Force C:\openems\outputs | Out-Null
Copy-Item -Force outputs\deviceConfig_openems_fields.conf C:\openems\outputs\
```

Chạy bằng jar đã build với thư mục runtime riêng:

```powershell
java `
  -Dfelix.cm.dir=C:\openems\config `
  -Dopenems.data.dir=C:\openems\data `
  -jar build\ziot-edge.jar
```

Nếu chưa có `build\ziot-edge.jar` nhưng đã có jar export:

```powershell
java `
  -Dfelix.cm.dir=C:\openems\config `
  -Dopenems.data.dir=C:\openems\data `
  -jar "io.openems.edge.application\generated\distributions\executable\ZiotEdgeApp.jar"
```

Mở Felix Web Console:

```text
http://localhost:8080/system/console/configMgr
```

REST/JSON API nếu đã bật controller REST:

```text
http://localhost:8084/rest
```

## Log Điều Khiển

Controller PV Sell-to-Grid Limit có log để xem nhanh controller đang tính và
gửi setpoint gì:

```text
CTRL_WRITE_OK ⚡ Grid=133.59 kW | 🏭 Load=133.59 kW | ☀ PV=0.00 kW | 🎯 Limit=0.00 kW | ✍ Write=100.00% -> pvInverter0
```

Ý nghĩa:

```text
Grid  : công suất tại grid meter; dương là mua từ lưới, âm là phát lên lưới
Load  : ước tính tải = Grid + PV
PV    : công suất PV hiện tại
Limit : maximumSellToGridPower
Write : setpoint controller gửi cho inverter
```

Log xác nhận ghi Modbus thành công:

```text
ZIOT_WRITE_OK component=pvInverter0 tag=ActivePowerLimitFixed channel=SetActivePowerLimitPercent fc=FC6 offset=5402 size=1
```

Lệnh grep nhanh:

```bash
docker logs --since 10m openems-edge 2>&1 | grep -E "CTRL_WRITE_OK|ZIOT_WRITE_OK"
```

## Telemetry MQTT

ZIOT Edge có 3 phần MQTT chính:

```text
Bridge.Mqtt               Kết nối MQTT broker dùng chung
Controller.Api.MQTT       Publish channel OpenEMS lên MQTT
Controller.MQTT.Telemetry Gửi payload telemetry gọn
```

`Controller.MQTT.Telemetry` tự phân biệt thiết bị:

```text
ZIOT Generic Meter       -> payload meter gọn: kiloWatts, kWH, data, timeStamp
ZIOT Generic PV-Inverter -> payload inverter đầy đủ: Watts, WH, VoltAN/BN/CN, AmpsA/B/C, DCVolt, DCAmps, ...
```

Payload meter mẫu:

```json
{
  "kiloWatts": 133.59,
  "kWH": 115763.0,
  "data": [
    { "name": "kiloWatts", "value": 133.59, "unit": "kW" },
    { "name": "kWH", "value": 115763.0, "unit": "kWh" }
  ],
  "timeStamp": "2026-07-14 02:55:00.000000"
}
```

## Cấu Hình Thiết Bị Trên Felix

Trong Felix Web Console, tạo các component ZIOT Generic:

```text
ZIOT Generic Meter
ZIOT Generic PV-Inverter
ZIOT Generic ESS
ZIOT Generic Sensor
```

Các field quan trọng:

```text
Modbus Unit-ID
Mapping file
Model key
Read only
```

`Model key` dùng dropdown lấy theo mapping file. Khi thêm model mới vào `deviceConfig_openems_fields.conf`, app sẽ đọc danh sách model từ file config.

## Yêu Cầu Điều Khiển PV/BESS

Yêu cầu cho mobile app nằm tại:

```text
doc/ZIOT_MOBILE_DEVICE_CONTROL_REQUIREMENTS.md
```

Tóm tắt:

- `PV Plan`: khởi động lại, bám tải, chạy theo giá trị cố định.
- `PV Plan`: inverter ngoài cluster bắt buộc nhập công suất cố định.
- `PV+BESS Plan`: khởi động lại, bám tải, peak shaving, TOU.
- `PV+BESS Plan`: ESS ngoài cluster mặc định là không điều khiển.
- TOU hỗ trợ tối đa 10 khung thời gian.

## CI/CD

Workflow chính:

```text
.github/workflows/ziot-ems-iot2050.yml
```

Chức năng:

- Build `ziot-edge.jar`.
- Kiểm tra bundle runtime quan trọng.
- Tạo Docker image `linux/arm64`.
- Đẩy image lên Docker Hub.

Docker image:

```text
doanhnguyen01/ziot:ziot-edge-iot2050
```

Tag theo commit:

```text
doanhnguyen01/ziot:ziot-edge-iot2050-<GITHUB_SHA>
```

Tag theo Git tag:

```text
doanhnguyen01/ziot:ziot-edge-iot2050-<TAG>
```

## Xử Lý Lỗi Build Trên Windows

Nếu gặp lỗi Kotlin daemon kiểu `AccessDeniedException` trong:

```text
C:\Users\<user>\AppData\Local\kotlin\daemon
```

Thử dừng Gradle daemon rồi build lại:

```powershell
.\gradlew.bat --stop
.\build-ziot-edge.bat
```

Nếu gặp lỗi thiếu bundle generated dạng:

```text
Bundle file ".../generated/*.jar" does not exist
```

Thường là workspace bnd/Gradle chưa sinh đủ generated jar. Thử build lại không dùng `--rerun-tasks` trước:

```powershell
.\build-ziot-edge.bat
```

Nếu vẫn lỗi, kiểm tra các module vừa sửa có compile được không:

```powershell
.\gradlew.bat :io.openems.common:compileJava --console=plain --warn
```

Sau đó chạy lại build jar.

## Cấu Trúc Quan Trọng

```text
io.openems.edge.ziot.generic
```

Bundle thiết bị ZIOT Generic: meter, PV inverter, ESS, sensor.

```text
io.openems.edge.application/ZiotEdgeApp.bndrun
```

Profile runtime quyết định bundle nào được đóng vào ZIOT Edge.

```text
outputs/deviceConfig_openems_fields.conf
```

File mapping model/tag/register dùng cho ZIOT Generic.

```text
docker-compose.iot2050.yml
```

Compose file triển khai nhanh trên IOT2050.

## License

ZIOT EMS Edge by DoanhGn là bản tùy biến dựa trên OpenEMS.

OpenEMS là Open Source Energy Management System do OpenEMS Association e.V. phát triển, với các thành phần do FENECON GmbH đóng góp.

License gốc trong repo:

- OpenEMS Edge / Backend: [Eclipse Public License 2.0](LICENSE-EPL-2.0)
- OpenEMS UI: [GNU Affero General Public License 3.0](LICENSE-AGPL-3.0)
