# ZIOT Edge

**ZIOT Edge by DoanhGn** là bản OpenEMS Edge được tối giản cho hệ sinh thái ZIOT và tối ưu để triển khai trên **Siemens IOT2050** bằng Docker container ARM64.

Mục tiêu chính: pull image về IOT2050 là chạy được, không cần cài Java trên máy host.

## Chạy Trên IOT2050

Trên IOT2050 chỉ cần Docker/Docker Compose. Image đã đóng sẵn Java runtime, `ziot-edge.jar` và file mapping trong `outputs/`.

```bash
mkdir -p /opt/openems-edge/config /opt/openems-edge/data /opt/openems-edge/outputs
cd /opt/openems-edge
wget -O docker-compose.yml https://raw.githubusercontent.com/doanhkem/ZIOT_EMS/main/docker-compose.iot2050.yml
docker compose pull
docker compose up -d
```

Mở Felix Web Console:

```text
http://<IP-IOT2050>:8080/system/console/configMgr
```

Image mặc định:

```text
doanhnguyen01/ziot:ziot-edge-iot2050
```

Container dùng `network_mode: host` để Edge truy cập Modbus/TCP, Modbus/RTU gateway và Web Console trực tiếp trên mạng của IOT2050.

## Cấu Hình Runtime

Các thư mục trên IOT2050:

```text
/opt/openems-edge/config   Felix/OpenEMS config
/opt/openems-edge/data     dữ liệu runtime
/opt/openems-edge/outputs  mapping thiết bị ZIOT Generic
```

Nếu muốn chỉnh mapping thiết bị trực tiếp trên IOT2050, sửa:

```text
/opt/openems-edge/outputs/deviceConfig_openems_fields.conf
```

Sau đó restart:

```bash
docker compose restart
```

## Thiết Bị ZIOT Generic

Các factory thiết bị được giữ:

```text
Ziot.Generic.Meter
Ziot.Generic.PvInverter
Ziot.Generic.Ess
Ziot.Generic.Sensor
```

Các factory này đọc mapping từ:

```text
outputs/deviceConfig_openems_fields.conf
```

Model được chọn bằng dropdown trong Felix Web Console, không cần nhập tay model key.

## Controller Được Giữ

```text
Controller.PvInverter.FixPowerLimit
Controller.PvInverter.SellToGridLimit
Controller.Hybrid.PvEss
Controller.Symmetric.LimitActivePower
```

Ý nghĩa nhanh:

- `Controller.PvInverter.FixPowerLimit`: đặt giới hạn công suất cố định cho một inverter.
- `Controller.PvInverter.SellToGridLimit`: giới hạn công suất bán lên lưới.
- `Controller.Hybrid.PvEss`: điều khiển phối hợp PV inverter và ESS.
- `Controller.Symmetric.LimitActivePower`: giới hạn active power theo component được cấu hình.

## Build Cho Developer

Build jar local trên máy dev:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
.\gradlew.bat buildZiotEdge
```

Kết quả:

```text
build\ziot-edge.jar
```

Đây là bước dành cho developer. Khi triển khai IOT2050, dùng Docker image thay vì chạy `java -jar` trên host.

## CI/CD

GitHub Actions đang build theo target IOT2050:

- `build.yml`: build và kiểm tra `ziot-edge.jar`.
- `docker.yml`: build image `linux/arm64` cho IOT2050 và push image.
- `release.yml`: tạo GitHub Release draft kèm `ziot-edge-iot2050.jar`.

DockerHub image:

```text
doanhnguyen01/ziot:ziot-edge-iot2050
```

GHCR image:

```text
ghcr.io/doanhkem/ziot_ems:ziot-edge-iot2050
```

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

File mapping model/tag/register dùng cho thiết bị ZIOT Generic.

```text
docker-compose.iot2050.yml
```

Compose file triển khai nhanh trên IOT2050.

## License

ZIOT Edge by DoanhGn là bản tùy biến dựa trên OpenEMS.

OpenEMS là Open Source Energy Management System do OpenEMS Association e.V. phát triển, với các thành phần do FENECON GmbH đóng góp.

License gốc trong repo:

- OpenEMS Edge / Backend: [Eclipse Public License 2.0](LICENSE-EPL-2.0)
- OpenEMS UI: [GNU Affero General Public License 3.0](LICENSE-AGPL-3.0)
