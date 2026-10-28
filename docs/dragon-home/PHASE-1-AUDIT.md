# Dragon Home — Phase 1: Audit và thiết kế

Ngày: 28/10/2026 · Trạng thái: chờ nghiệm thu. Chưa có dòng code Dragon Home nào được viết.

## 1. Kiến trúc hiện tại (đã kiểm tra trên code, không đoán)

| Hạng mục | Thực tế |
|---|---|
| Nền tảng | **Android native**: Kotlin 2.2, Jetpack Compose (Material 3), minSdk 24, targetSdk 36. **Không có iOS.** |
| Kiến trúc | MVVM: một `FinanceViewModel` (StateFlow) → `FinanceRepository` → Room DAO. ~14.300 dòng Kotlin. |
| Cơ sở dữ liệu | Room **v8**, 8 bảng: accounts, categories, transactions, budgets, debts, goals, recurring_bills, card_installments. Có migration đầy đủ 1→8, schema export ở `app/schemas/`, không có destructive fallback. |
| Ghi số dư | Mọi thao tác tiền chạy trong `database.withTransaction` (cộng/hoàn số dư theo giao dịch). |
| Người dùng | **Không có đăng nhập**, một máy một người. Không có API server. |
| Đồng bộ | Chỉ có **sao lưu snapshot thủ công** lên VPS (`VpsBackup`, danh sách bảng cứng, kiểm tra `databaseVersion`). Không có đồng bộ nhiều máy. |
| Phát hành | APK debug ký khoá máy dev, kênh tự cập nhật qua VPS (`latest.json` + SHA-256). |
| Giao diện | Token màu/khoảng cách (`ui/theme`), component chung (`ui/components/Ui.kt`), 4 tab + nút ➕, nút micro nổi. |
| Kiểm thử | 41 test (Robolectric, migration, parser, chụp màn hình) — đều đạt trên code hiện tại. |
| Công việc đang dở | ChatGPT đang làm "trả góp thẻ" (DB v8, app 1.6.5) **chưa commit**. Dragon Home phải đi sau, bắt đầu từ DB **v9**. |

## 2. Đánh giá khả năng tích hợp 3D

| Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|
| **SceneView (trên Filament của Google)** | Có Compose, đọc GLB/glTF, skeletal animation + blend, PBR, IBL; native nên nhẹ pin hơn WebView | +~5–7 MB APK (thư viện native arm64) | **Chọn** |
| Filament thuần | Kiểm soát tối đa | Tự viết camera/animation/chạm — tốn công | Dự phòng nếu SceneView vướng |
| Unity as a Library | Công cụ game đầy đủ | APK +40–60 MB, khởi động chậm, hai hệ build | Không hợp app tài chính nhỏ |
| WebView + three.js | Viết nhanh | Chậm và hao pin trên máy phổ thông | Không chọn |

Nguyên tắc hiệu năng:
- 3D **chỉ chạy trên màn Dragon World**. Thẻ rồng ở Tổng quan dùng **ảnh render từ chính model** (đóng gói sẵn theo từng hình thái), không mở GPU, không tốn pin.
- Tạm dừng render khi rời màn / app xuống nền. Có chế độ 30 FPS (giới hạn frame) và tắt bóng đổ cho máy yếu.
- Mục tiêu asset: rồng ≤ 15k tam giác, phòng ≤ 40k, texture KTX2 ≤ 1024px, nén mesh meshopt/Draco. Tổng asset MVP ≤ 8 MB.

## 3. Thành phần tái sử dụng

- **Room + quy trình migration + schema export**: thêm bảng game bằng migration v8→v9, không đụng 8 bảng tài chính.
- **`withTransaction`** của repository: dùng cho cộng thưởng và mua đồ (cùng kiểu an toàn như ghi số dư).
- **Luồng dữ liệu tài chính** (transactions, budgets, goals) để Mission Engine đánh giá — chỉ đọc.
- **FinancialAdvisorEngine**: lấy câu nói của rồng từ số liệu thật ("Còn 324k hôm nay").
- **Token + component UI**: thẻ, nút, tab, thanh tiến độ — phần game dùng chung để không lạc tông.
- **VpsBackup**: mở rộng danh sách bảng để rồng, xu, kho đồ được sao lưu cùng.
- **Kênh cập nhật VPS**: phát hành bản có asset mới như bình thường.

## 4. Cấu trúc module đề xuất

```
com.example.dragon
├─ data/        entity + DAO các bảng game, DragonRepository (withTransaction)
├─ domain/
│   ├─ LevelCurve        công thức XP + bảng 6 giai đoạn (cấu hình được)
│   ├─ MissionEngine     đánh giá nhiệm vụ từ dữ liệu tài chính (thuần, test được)
│   ├─ RewardService     cộng XP/xu có idempotency key
│   ├─ CareService       năng lượng/vui/gắn bó, hồi phục theo thời gian, trần thưởng
│   └─ ShopService       mua đồ an toàn (kiểm tra số dư + trùng + giao dịch DB)
├─ render/      DragonScene (SceneView), AnimationController, RoomScene, PlacementGrid
└─ ui/          DragonWorld, Care, Missions, House(edit), Shop, Inventory, Profile, thẻ ở Tổng quan
assets/dragon/  mochi_stageN.glb, room_lv1.glb, furniture/*.glb, thumbs/*.webp
```

Liên kết tài chính → rồng **một chiều**: sau khi giao dịch lưu xong (đã commit), app phát sự kiện; Mission Engine chạy riêng. Nếu thưởng lỗi, giao dịch không bị ảnh hưởng; lần mở app sau sẽ đối soát lại.

## 5. Thiết kế dữ liệu (Room v9, chỉ thêm bảng)

Mọi bảng game có `ownerId` (hiện là `"local"`, sẵn cho đăng nhập/đồng bộ sau) và `id` dạng UUID để gộp dữ liệu nhiều máy không đụng nhau.

| Bảng | Cột chính | Ghi chú |
|---|---|---|
| `pets` | id, ownerId, name="Mộc Mộc", level, xp, energy, happiness, bond, outfitJson, lastCareAt, createdAt | Một rồng/người ở MVP |
| `pet_levels` | level, xpToNext, stage, unlockJson | Sinh từ `LevelCurve`, đổi được không cần sửa code |
| `xp_events` | id, ownerId, petId, amount, source, **idempotencyKey UNIQUE**, createdAt | Chỉ thêm, không sửa/xoá |
| `coin_ledger` | id, ownerId, delta, reason, refId, **idempotencyKey UNIQUE**, createdAt | Số dư = tổng `delta`, luôn ≥ 0 |
| `missions` | id, type(daily/weekly/once), ruleKey, xp, coin, active | Cấu hình nhiệm vụ |
| `mission_progress` | id, ownerId, missionId, **periodKey**, progress, completedAt, rewardedAt; UNIQUE(missionId, periodKey) | Khoá chống thưởng lại |
| `shop_items` | id, name, category, rarity, price, unique, unlockLevel, previewAsset, modelAsset | Danh mục đóng gói theo app |
| `pet_inventory` | id, ownerId, itemId, qty, acquiredAt, purchaseKey UNIQUE | |
| `houses` / `rooms` | houseLevel, roomId, gridW, gridH, wallSides | MVP: 1 nhà, 1 phòng |
| `furniture_placements` | id, roomId, inventoryId, gridX, gridY, rotation, updatedAt | Kiểm va chạm trên lưới trước khi lưu |
| `pet_animations` | key, assetClip, unlockLevel | Thêm animation mới không đổi schema |
| `achievements` | id, ownerId, key, unlockedAt | |

**Chống thưởng trùng (yêu cầu mục 5):**
- Khoá thưởng = `missionId + periodKey` (ví dụ `daily_first_tx:2026-10-28`). Xoá rồi nhập lại giao dịch trong ngày **không** tạo khoá mới → không thưởng lại.
- Ghi liên tục nhiều giao dịch: nhiệm vụ ngày chỉ có một khoá/ngày → không cộng vô hạn.
- Đồng bộ/khôi phục: sự kiện mang `idempotencyKey` UNIQUE → nhập lại cùng sự kiện bị bỏ qua.
- Bỏ app lâu: không có cơ chế trừ XP/cấp; năng lượng chỉ giảm tới mức "buồn ngủ", không bao giờ về 0.

**Mua đồ:** một `withTransaction`: đọc số dư từ ledger → kiểm tra đủ xu, vật phẩm độc nhất chưa có → ghi ledger `−price` với `purchaseKey` (UUID tạo lúc bấm, nên bấm hai lần chỉ trừ một lần) → thêm vào kho. Lỗi giữa chừng thì rollback toàn bộ.

**Rollback migration:** migration v9 chỉ `CREATE TABLE`. Nếu phải gỡ: bản sau tạo migration v10 `DROP` các bảng game; dữ liệu tài chính không liên quan. Sao lưu snapshot hiện tại cũng cần nhận `databaseVersion = 9`.

**Thử công thức XP** `100 + 20(L−1) + 5(L−1)²`: L1→L2 = 100 XP, L10→L11 = 685, L49→L50 = 12.580. Tổng tới L50 ≈ 218.540 XP. Với ~110 XP/ngày nếu ghi đều: tới L20 khoảng 5 tháng, tới L50 khoảng 5,5 năm. Đây là con số để chỉnh; bảng `pet_levels` cho phép đổi mà không ra bản app mới về schema.

## 6. Giao diện từng màn

- **Điều hướng:** đề xuất **giữ thanh dưới hiện tại** (Tổng quan · Giao dịch · ➕ · Báo cáo · Tài khoản) để phần tài chính không đổi; Dragon World mở từ **thẻ rồng trên Tổng quan** (toàn màn hình, có nút quay lại). Phương án theo đặc tả (5 tab Tổng quan · Thu chi · Ghi nhanh · Rồng · Báo cáo) làm Tài khoản lùi vào trong — chờ bạn chọn.
- **Thẻ rồng ở Tổng quan:** ảnh render, tên, cấp, thanh XP, chuỗi ngày, một câu nói; chạm để mở.
- **Dragon World:** phòng 3D chiếm ~70% màn; thanh trên (tên · cấp · XP · xu), thanh dưới 5 nút (Chăm sóc · Nhiệm vụ · Nhà · Cửa hàng · Kho) nằm dưới vùng an toàn, không che rồng.
- **Nhiệm vụ / Cửa hàng / Kho / Hồ sơ:** dùng component thẻ, chip lọc, lưới hiện có; độ hiếm dùng 4 màu cố định (xám/xanh/tím/vàng).
- **Nhà – chỉnh sửa:** lưới ẩn hiện mờ khi kéo đồ, ô xanh = đặt được, đỏ = va chạm; nút xoay 90°, thu hồi, lưu.
- **Thông báo thưởng:** toast nhỏ 2 giây "+40 XP · +20 xu", không chặn thao tác ghi giao dịch.

## 7. Công việc theo thứ tự ưu tiên

**Làm được ngay, không phụ thuộc mô hình 3D (Phase 2a):**
1. Commit phần "trả góp thẻ" của ChatGPT trước (tránh trộn lẫn) — cần bạn xác nhận.
2. Migration v9 + entity/DAO 13 bảng + test migration và rollback.
3. `LevelCurve`, `RewardService`, `MissionEngine` (7 nhiệm vụ mẫu), `CareService`, `ShopService` + test chống trùng, số dư âm, xoá-nhập lại, mua hai lần.
4. Kết nối sự kiện sau khi lưu giao dịch; đối soát khi mở app.
5. Màn Nhiệm vụ, Hồ sơ rồng (cấp, XP, giai đoạn), toast thưởng, thẻ ở Tổng quan (tạm dùng ảnh minh hoạ, ghi rõ là tạm).
6. Mở rộng `VpsBackup` cho bảng game.

**Cần mô hình 3D (Phase 2b, 3):**
7. Asset pipeline (mục 8), tích hợp SceneView, 8 animation, thẻ dùng ảnh render thật.
8. Phòng 3D, lưới đặt đồ, va chạm, chế độ sửa, cửa hàng có model.

**Phase 4:** tối ưu, thử máy yếu, chế độ 30 FPS, kiểm thử trải nghiệm.

## 8. Rủi ro và hướng xử lý

| Rủi ro | Mức | Hướng xử lý |
|---|---|---|
| **Mô hình rồng 3D chất lượng cao** — máy này chưa có Blender; mô hình dựng bằng code chỉ ra dạng khối đơn giản, đặc tả đã nói rõ không chấp nhận làm bản hoàn thiện | Cao | Nguồn model: (a) thuê họa sĩ 3D, hoặc (b) công cụ AI tạo 3D có auto-rig (Tripo, Meshy) rồi chỉnh trong Blender. Pipeline: model → retopology ≤15k tris → rig → 8 action → bake → xuất GLB (meshopt + KTX2) → kiểm tra trong viewer → đưa vào `assets/dragon/`. Cần bạn chọn nguồn và có tài khoản/chi phí |
| **iOS** — app là Android native, không có code iOS | Cao | Ngoài phạm vi hiện tại. Muốn iOS phải chuyển sang Compose Multiplatform (phần UI/logic Kotlin dùng lại được) — một dự án riêng |
| **Đồng bộ nhiều máy** — chưa có đăng nhập và API | Trung bình | Dữ liệu game đã thiết kế sẵn (UUID, idempotency, ledger chỉ thêm) để gộp được. Đồng bộ thật cần API + đăng nhập trên VPS — dự án riêng sau MVP |
| Dung lượng APK tăng | Trung bình | ~+6 MB thư viện + ≤8 MB asset; chỉ đóng gói arm64 cho bản phát hành |
| Máy yếu giật | Trung bình | Chế độ 30 FPS, tắt bóng, LOD đơn giản; 3D chỉ chạy khi mở Dragon World |
| Gamification lệch hướng (khuyến khích chi tiêu, cày XP) | Trung bình | Không thưởng theo số tiền; nhiệm vụ thưởng theo **thói quen ghi chép**; trần thưởng chăm sóc/ngày |
| Hai trợ lý AI sửa cùng repo | Trung bình | Commit/đồng bộ trước mỗi phase; Dragon Home nằm trong package riêng, chỉ chạm `AppDatabase`, `MainActivity`, `ScreenDashboard`, `VpsBackup` |
