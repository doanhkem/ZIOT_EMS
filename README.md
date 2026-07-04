# ZIOT Edge

**ZIOT Edge by DoanhGn** là bản OpenEMS Edge được tối giản và tùy biến cho hệ sinh thái ZIOT. Mục tiêu của bản này là giữ lại phần lõi Edge, API, controller cần thiết và gom cấu hình thiết bị về một nhóm `ZIOT Generic`, thay vì đóng gói hàng loạt driver/vendor riêng lẻ.

Repo này vẫn dựa trên OpenEMS, nhưng bản chạy chính trong dự án là ZIOT Edge.

## Mục Tiêu

- Tối giản OpenEMS Edge để dễ triển khai, dễ kiểm soát và giảm nhiễu trong Felix Web Console.
- Dùng cấu hình mapping chung trong `outputs/deviceConfig_openems_fields.conf`.
- Thay các driver thiết bị riêng lẻ bằng nhóm thiết bị ZIOT Generic.
- Giữ các controller điều khiển cần thiết cho hệ PV, ESS, meter và sensor.
- Vẫn chạy được qua OSGi/Felix Web Console như OpenEMS Edge chuẩn.

## Thiết Bị ZIOT Generic

Bản ZIOT Edge hiện giữ các factory thiết bị sau:

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

Các model được chọn bằng dropdown trong Felix Web Console, không cần nhập tay model key.

## Controller Được Giữ Lại

Các controller chính vẫn được đóng gói và dùng bình thường:

```text
Controller.PvInverter.FixPowerLimit
Controller.PvInverter.SellToGridLimit
Controller.Hybrid.PvEss
Controller.Symmetric.LimitActivePower
```

Ý nghĩa nhanh:

- `Controller.PvInverter.FixPowerLimit`: đặt cố định giới hạn công suất cho một inverter.
- `Controller.PvInverter.SellToGridLimit`: giới hạn công suất PV theo công suất bán lên lưới.
- `Controller.Hybrid.PvEss`: điều khiển phối hợp PV inverter và ESS.
- `Controller.Symmetric.LimitActivePower`: giới hạn active power theo component được cấu hình.

## Build

Build cả hai bản jar:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
.\gradlew.bat buildEdge buildZiotEdge
```

Kết quả:

```text
build\openems-edge.jar
build\ziot-edge.jar
```

## Chạy Edge

Khuyến nghị chạy bản đặt tên rõ cho ZIOT:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
java -jar .\build\ziot-edge.jar
```

Hoặc chạy theo tên mặc định OpenEMS:

```powershell
cd "D:\visualcode\ZIOT\EMS edeg\EMS_pro\EMS_pro"
java -jar .\build\openems-edge.jar
```

## Khác Nhau Giữa Hai Jar

`build\openems-edge.jar`

- Build từ `io.openems.edge.application/EdgeApp.bndrun`.
- Giữ tên quen thuộc của OpenEMS Edge.
- Phù hợp khi cần thay thế hoặc chạy theo quy trình OpenEMS cũ.

`build\ziot-edge.jar`

- Build từ `io.openems.edge.application/ZiotEdgeApp.bndrun`.
- Đặt tên rõ đây là bản ZIOT Edge.
- Khuyến nghị dùng trong triển khai ZIOT.

Hiện tại hai profile được giữ gần như giống nhau về nội dung runtime.

## Felix Web Console

Sau khi chạy Edge, mở:

```text
http://localhost:8080/system/console/configMgr
```

Tại đây có thể tạo cấu hình:

- Modbus bridge: `Bridge Modbus/TCP` hoặc `Bridge Modbus/RTU Serial`
- Thiết bị: `ZIOT Generic Meter`, `ZIOT Generic PV-Inverter`, `ZIOT Generic ESS`, `ZIOT Generic Sensor`
- Controller: các controller được giữ lại ở trên

Ví dụ controller giới hạn công suất cố định cho inverter:

```text
Factory: Controller PV-Inverter Fix Power Limit
Component-ID: ctrlPvInverterFixPowerLimit0
PV-Inverter-ID: pvInverter0
Power Limit [W]: 5000
```

## Cấu Hình Runtime

OpenEMS/Felix lưu cấu hình runtime tại:

```text
C:\openems\config
```

Dữ liệu runtime mặc định:

```text
C:\openems\data
```

Khi thay đổi source hoặc bundle trong jar, cần build lại và restart Edge để Felix Web Console nhận factory mới.

## Cấu Trúc Quan Trọng

```text
io.openems.edge.ziot.generic
```

Bundle thiết bị ZIOT Generic: meter, PV inverter, ESS, sensor.

```text
io.openems.edge.application/EdgeApp.bndrun
io.openems.edge.application/ZiotEdgeApp.bndrun
```

Profile runtime quyết định bundle nào được đóng vào jar Edge.

```text
outputs/deviceConfig_openems_fields.conf
```

File mapping model/tag/register dùng cho thiết bị ZIOT Generic.

## Ghi Chú Phát Triển

- Source các driver/vendor OpenEMS gốc có thể vẫn còn trong repo để tham khảo hoặc build module, nhưng không mặc định đóng gói vào ZIOT Edge runtime.
- Nếu cần thêm thiết bị mới, ưu tiên thêm model/tag vào mapping và mở rộng `io.openems.edge.ziot.generic`.
- Nếu cần thêm controller mới, thêm bundle controller đó vào cả `EdgeApp.bndrun` và `ZiotEdgeApp.bndrun`.
- Không nên kéo lại toàn bộ nhóm vendor device nếu mục tiêu là giữ Edge tối giản.

## Nguồn Gốc Và License

ZIOT Edge by DoanhGn là bản tùy biến dựa trên OpenEMS.

OpenEMS là Open Source Energy Management System do OpenEMS Association e.V. phát triển, với các thành phần do FENECON GmbH đóng góp.

License gốc trong repo:

- OpenEMS Edge / Backend: [Eclipse Public License 2.0](LICENSE-EPL-2.0)
- OpenEMS UI: [GNU Affero General Public License 3.0](LICENSE-AGPL-3.0)

Vui lòng giữ attribution và tuân thủ license tương ứng khi phân phối hoặc triển khai.
