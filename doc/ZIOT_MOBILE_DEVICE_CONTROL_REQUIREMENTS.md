# ZIOT EMS - Mobile Device Control Settings Requirement

## 1. Muc Tieu

Tai lieu nay mo ta yeu cau cho giao dien cai dat dieu khien thiet bi tren ung dung mobile ZIOT EMS.

Man hinh nay phuc vu viec cau hinh dieu khien o cap plan, gom 2 nhom chinh:

- PV Plan
- PV+BESS Plan

Moi plan gom 2 muc chinh:

- Restart
- Power Control

UI can giup nguoi dung chon thiet bi, tao nhom cluster va cau hinh mode dieu khien phu hop. Cac thiet bi nam ngoai cluster phai co hanh vi mac dinh ro rang de tranh dieu khien nham.

## 2. Nguyen Tac Chung Cho UI

- UI phai tach ro `PV Plan` va `PV+BESS Plan`.
- Moi plan phai co 2 khu vuc: `Restart` va `Power Control`.
- Cac thao tac restart phai co confirm truoc khi gui len he thong.
- Cac gia tri cong suat hien thi va nhap theo don vi `W` hoac `kW` tuy chuan app hien tai.
- Neu UI dung `kW`, app phai convert ve don vi backend/Edge yeu cau truoc khi gui.
- Moi mode chi hien thi field lien quan den mode do.
- UI phai doc lai cau hinh hien tai sau khi save de dam bao trang thai tren app khop voi Edge.
- UI phai hien thi ro thiet bi nao dang nam trong cluster, thiet bi nao dang nam ngoai cluster.
- Khi thiet bi khong ho tro mot chuc nang, UI phai disable hoac an chuc nang do.

## 3. PV Plan

PV Plan dung de cau hinh dieu khien cho he PV inverter.

PV Plan gom 2 muc:

- Restart
- Power Control

### 3.1 Restart

Muc Restart cho phep nguoi dung gui lenh khoi dong lai PV inverter.

#### Yeu Cau UI

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| Target | Select | Co | `All inverters` hoac `Single inverter` |
| Inverter | Select | Khi chon single | Chon 1 bien tan cu the |
| Confirm restart | Dialog | Co | Can confirm truoc khi gui lenh |

#### Rule

- Neu chon `All inverters`, app gui lenh restart den tat ca inverter co ho tro restart.
- Neu chon `Single inverter`, app chi gui lenh restart den inverter duoc chon.
- Inverter nao khong ho tro restart thi khong hien trong danh sach hoac hien disabled.
- Sau khi gui lenh, UI phai hien thi trang thai thanh cong/that bai theo tung inverter neu co.

### 3.2 Power Control

Muc Power Control cho phep cau hinh cach dieu khien cong suat PV.

Power Control co 2 option:

- Load-following / Bam tai
- Fixed value / Gia tri cung

#### Yeu Cau UI Chung

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| Control enable | Toggle | Co | Bat/tat dieu khien PV Plan |
| Control mode | Select | Co | `Load-following` hoac `Fixed value` |
| PV cluster | Multi-select | Co | Chon nhom inverter tham gia cluster |
| Ungrouped inverter list | List | Co | Hien thi cac inverter khong nam trong cluster |

#### Mode Load-following / Bam Tai

Mode bam tai dieu khien nhom inverter trong cluster de bam theo tai hien tai, han che phat nguoc len luoi.

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| PV cluster members | Multi-select | Co | Cac inverter tham gia bam tai |
| Sell-to-grid limit | Number | Khuyen nghi | Mac dinh `0` neu dung zero export |
| Ramp limit | Number | Tuy chon | Gioi han toc do thay doi cong suat neu backend ho tro |

#### Mode Fixed Value / Gia Tri Cung

Mode fixed value dat cong suat phat co dinh cho cluster hoac cho inverter nam ngoai cluster.

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| PV cluster members | Multi-select | Co | Cac inverter duoc dieu khien theo cluster |
| Cluster fixed power | Number | Co | Cong suat co dinh cho cluster |
| Ungrouped inverter fixed power | Number per inverter | Co neu co inverter ngoai cluster | Moi inverter ngoai cluster phai co gia tri fixed power rieng |

#### Rule Cho PV Cluster Va Inverter Ngoai Cluster

- Nguoi dung duoc phep chon mot nhom inverter vao `PV cluster`.
- Cac inverter nam trong cluster se duoc dieu khien theo mode cua PV Plan.
- Cac inverter khong duoc dua vao cluster se mac dinh chay o mode `Fixed value`.
- Voi moi inverter khong nam trong cluster, UI bat buoc yeu cau nhap gia tri fixed power.
- Khong duoc save cau hinh neu con inverter ngoai cluster ma chua co fixed power.
- UI phai hien thi ro:
  - Inverter trong cluster
  - Inverter ngoai cluster
  - Fixed power cua tung inverter ngoai cluster

## 4. PV+BESS Plan

PV+BESS Plan dung de cau hinh dieu khien he thong co BESS/ESS tham gia dieu phoi nang luong.

PV+BESS Plan gom 2 muc:

- Restart
- Power Control

### 4.1 Restart

Muc Restart cho phep nguoi dung gui lenh khoi dong lai ESS/BESS.

#### Yeu Cau UI

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| Target | Select | Co | `All ESS` hoac `Single ESS` |
| ESS | Select | Khi chon single | Chon 1 ESS cu the |
| Confirm restart | Dialog | Co | Can confirm truoc khi gui lenh |

#### Rule

- Neu chon `All ESS`, app gui lenh restart den tat ca ESS co ho tro restart.
- Neu chon `Single ESS`, app chi gui lenh restart den ESS duoc chon.
- ESS nao khong ho tro restart thi khong hien trong danh sach hoac hien disabled.
- Sau khi gui lenh, UI phai hien thi trang thai thanh cong/that bai theo tung ESS neu co.

### 4.2 Power Control

Muc Power Control cho phep cau hinh cach dieu khien PV+BESS.

Power Control co 3 mode:

- Load-following / Bam tai
- Peak shaving
- TOU

O moi mode, UI phai cho phep add ESS vao cluster. Cac ESS khong nam trong cluster se mac dinh `khong dieu khien`.

#### Yeu Cau UI Chung

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| Control enable | Toggle | Co | Bat/tat dieu khien PV+BESS Plan |
| Control mode | Select | Co | `Load-following`, `Peak shaving`, `TOU` |
| ESS cluster | Multi-select | Co | Chon cac ESS tham gia dieu khien |
| Uncontrolled ESS list | List | Co | Hien thi ESS nam ngoai cluster |

#### Rule Cho ESS Cluster Va ESS Ngoai Cluster

- Nguoi dung duoc phep add ESS vao cluster o tung mode.
- Cac ESS nam trong cluster se duoc dieu khien theo mode dang chon.
- Cac ESS nam ngoai cluster se mac dinh `khong dieu khien`.
- UI phai hien thi ro ESS nao dang:
  - In cluster
  - Out of cluster / Not controlled
- Khong duoc gui lenh dieu khien den ESS nam ngoai cluster.
- ESS nam ngoai cluster chi duoc monitor, khong tham gia control.

### 4.3 Mode Load-following / Bam Tai

Mode bam tai dung de dieu khien ESS cluster theo tai hien tai, co the ket hop voi PV de giam phat nguoc hoac giam lay dien tu luoi tuy logic backend.

#### Field UI

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| ESS cluster members | Multi-select | Co | ESS tham gia bam tai |
| Target grid power | Number | Khuyen nghi | Mac dinh `0` neu muc tieu zero export |
| Min SOC | Number | Khuyen nghi | SOC toi thieu de cho phep xa |
| Max SOC | Number | Khuyen nghi | SOC toi da de cho phep sac |
| Max charge power | Number | Tuy chon | Gioi han cong suat sac |
| Max discharge power | Number | Tuy chon | Gioi han cong suat xa |

#### Rule

- Neu dung zero export, `Target grid power` mac dinh bang `0`.
- Neu SOC nho hon `Min SOC`, ESS khong duoc xa tiep de bam tai.
- Neu SOC lon hon `Max SOC`, ESS khong duoc sac tiep.

### 4.4 Mode Peak Shaving

Mode Peak shaving dung de cat dinh cong suat tai. Khi tai vuot nguong, ESS cluster xa de giam cong suat lay tu luoi.

#### Field UI

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| ESS cluster members | Multi-select | Co | ESS tham gia peak shaving |
| Peak limit | Number | Co | Nguong cat dinh |
| Recovery limit | Number | Khuyen nghi | Nguong cho phep sac lai |
| Min SOC | Number | Khuyen nghi | Bao ve pin khi xa |
| Max discharge power | Number | Khuyen nghi | Gioi han cong suat xa |

#### Rule

- `Peak limit` phai lon hon 0.
- `Recovery limit` neu co phai nho hon hoac bang `Peak limit`.
- SOC phai nam trong khoang 0-100%.
- Neu SOC thap hon `Min SOC`, ESS khong duoc xa de cat dinh.

### 4.5 Mode TOU - Time Of Use

Mode TOU dung de cau hinh lich sac/xa theo khung gio, tuong tu logic ENEZ.

UI can ho tro toi da 10 time slots.

#### Field UI Chung

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| ESS cluster members | Multi-select | Co | ESS tham gia TOU |
| TOU slots | List | Co | Toi da 10 slot |

#### Field UI Cho Moi TOU Slot

| Field | Kieu du lieu | Bat buoc | Ghi chu |
| --- | --- | --- | --- |
| Enable slot | Toggle | Co | Bat/tat khung gio |
| Start time | Time | Co | Gio bat dau |
| End time | Time | Co | Gio ket thuc |
| Mode | Select | Co | `Charge`, `Discharge`, `Idle` |
| Power | Number | Co voi Charge/Discharge | Cong suat sac/xa |
| Min SOC | Number | Khuyen nghi | Ap dung khi xa |
| Max SOC | Number | Khuyen nghi | Ap dung khi sac |
| Priority | Number/Select | Khuyen nghi | Thu tu uu tien neu trung khung gio |

#### Rule

- Toi da 10 slot.
- Khong cho phep save neu co slot enable nhung thieu `Start time`, `End time` hoac `Mode`.
- `Start time` phai khac `End time`.
- Neu slot qua ngay, UI phai hien thi ro vi du `22:00 - 06:00 next day`.
- Power phai lon hon hoac bang 0.
- SOC phai nam trong khoang 0-100%.
- Neu backend khong ho tro overlap, UI khong duoc cho phep cac slot trung thoi gian.

## 5. De Xuat Cau Truc Man Hinh Mobile

### 5.1 Screen: Control Settings

Man hinh chinh nen co 2 tab:

- PV Plan
- PV+BESS Plan

### 5.2 Tab: PV Plan

Trong tab PV Plan co 2 section:

- Restart
- Power Control

Section Restart:

- Radio/select: `All inverters` hoac `Single inverter`
- Neu single: dropdown chon inverter
- Button: `Restart`
- Confirm dialog truoc khi gui lenh

Section Power Control:

- Toggle: enable/disable control
- Select mode: `Load-following` hoac `Fixed value`
- Multi-select: chon inverter vao PV cluster
- Danh sach inverter ngoai cluster
- Field fixed power cho tung inverter ngoai cluster
- Button: `Save`

### 5.3 Tab: PV+BESS Plan

Trong tab PV+BESS Plan co 2 section:

- Restart
- Power Control

Section Restart:

- Radio/select: `All ESS` hoac `Single ESS`
- Neu single: dropdown chon ESS
- Button: `Restart`
- Confirm dialog truoc khi gui lenh

Section Power Control:

- Toggle: enable/disable control
- Select mode: `Load-following`, `Peak shaving` hoac `TOU`
- Multi-select: add ESS vao cluster
- Danh sach ESS ngoai cluster voi status `Not controlled`
- Field cau hinh tuong ung theo mode
- Button: `Save`

## 6. Validation Bat Buoc

| Gia tri | Rule |
| --- | --- |
| Power | Phai la so, >= 0 |
| SOC | Tu 0 den 100 |
| Time slot | Start time va End time khong duoc rong neu slot enable |
| Mode | Phai chon 1 mode hop le |
| PV cluster | Bat buoc neu bat PV Power Control |
| ESS cluster | Bat buoc neu bat PV+BESS Power Control |
| PV ungrouped fixed power | Bat buoc voi moi inverter ngoai cluster |
| Peak limit | Phai lon hon 0 |
| Recovery limit | Nho hon hoac bang Peak limit neu co |

## 7. Trang Thai Va Phan Hoi UI

UI can hien thi cac trang thai:

- Dang doc cau hinh
- Dang luu cau hinh
- Dang gui lenh restart
- Luu thanh cong
- Luu that bai
- Restart thanh cong
- Restart that bai
- Thiet bi offline
- Chuc nang khong duoc ho tro
- Controller dang active
- Controller dang inactive

Khi thao tac that bai, UI can hien thi loi tra ve tu backend/Edge neu co.

## 8. Ghi Chu Cho Backend / Edge Integration

- PV Power Control can ho tro tao/cap nhat PV-Inverter Cluster.
- PV mode `Load-following` dieu khien cac inverter trong cluster.
- PV mode `Fixed value` dieu khien cluster theo fixed power.
- PV inverter nam ngoai cluster phai duoc set fixed power rieng.
- PV+BESS Power Control can ho tro tao/cap nhat ESS Cluster.
- ESS nam trong cluster se tham gia mode dang chon.
- ESS nam ngoai cluster khong nhan lenh dieu khien.
- Peak shaving can co controller hoac plan logic nhan cac tham so cat dinh.
- TOU can ho tro toi da 10 slot cau hinh sac/xa.
- Restart chi hien thi khi thiet bi driver co channel/command ho tro restart.
- App mobile can doc lai cau hinh hien tai sau khi save de dam bao UI khop voi Edge.

## 9. Acceptance Criteria

- UI co 2 tab/chuyen muc chinh: `PV Plan` va `PV+BESS Plan`.
- PV Plan co section `Restart` cho phep chon tat ca inverter hoac 1 inverter cu the.
- PV Plan co section `Power Control` voi 2 mode: `Load-following` va `Fixed value`.
- PV Plan cho phep chon nhom inverter vao cluster.
- PV inverter khong nam trong cluster bat buoc co fixed power rieng.
- PV+BESS Plan co section `Restart` cho phep chon tat ca ESS hoac 1 ESS cu the.
- PV+BESS Plan co section `Power Control` voi 3 mode: `Load-following`, `Peak shaving`, `TOU`.
- Moi mode cua PV+BESS cho phep add ESS vao cluster.
- ESS nam ngoai cluster mac dinh `Not controlled` va khong nhan lenh dieu khien.
- TOU ho tro toi da 10 time slots.
- UI validate day du truoc khi save.
- UI hien thi trang thai thanh cong/that bai sau khi save hoac restart.
