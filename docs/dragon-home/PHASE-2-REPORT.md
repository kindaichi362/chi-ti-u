# Dragon Home — Báo cáo giai đoạn 2 (MVP) · bản 1.7.0

## Đã xong
- **Mô hình rồng Mộc Mộc 3D** (dựng bằng code, `tools/dragon-gen`): 6 giai đoạn, 8 hoạt ảnh (idle, happy, sleep, eat, walk, play, levelup, touch), mỗi file GLB ~280 KB. Có trang xem thử `node serve.mjs` → http://localhost:8787.
- **Hiển thị 3D** bằng Filament 1.75.1 (bản mới nhất chạy được với Kotlin 2.2), nền trong suốt, 30 khung hình/giây, dừng khi rời màn. Nếu máy không chạy được 3D thì tự chuyển sang ảnh tĩnh có nhún nhảy.
- **Dữ liệu** (DB v9, chỉ thêm 2 bảng, không đụng bảng tài chính):
  - `dragon_pet`: tên và 3 chỉ số.
  - `dragon_ledger`: sổ XP/xu chỉ thêm. Khoá chính là khoá chống trùng.
  - Cấp và xu luôn là tổng của sổ này.
- **Cấp độ**: công thức `100 + 20(L−1) + 5(L−1)²`, 50 cấp, 6 giai đoạn (1–5, 6–12, 13–20, 21–30, 31–40, 41–50).
- **Nhiệm vụ (7)**:

  | Nhiệm vụ | Thưởng |
  |---|---|
  | Ghi khoản đầu tiên hôm nay | 20 XP / 5 xu |
  | Chốt sổ hôm nay (bấm nhận) | 30 / 10 |
  | Chuỗi 7 ngày (mỗi mốc 7, 14, 21…) | 100 / 50 |
  | Đặt ngân sách đầu tiên | 50 / 30 |
  | Xem báo cáo tuần | 20 / 10 |
  | Hoàn thành mục tiêu ≥ 500.000 ₫ | 150 / 80 |
  | Tuần trước không vượt ngân sách (cần ghi ≥ 3 ngày) | 60 / 30 |

  - Thưởng theo **thói quen ghi chép**, không theo số tiền.
  - Giao dịch chờ hoặc đã huỷ và điều chỉnh số dư không được tính.
- **Chăm sóc**:
  - Cho ăn, vuốt ve, chơi, ru ngủ; chạm vào rồng cũng tính là vuốt ve.
  - Năng lượng và vui vẻ giảm dần nhưng không xuống dưới 15 / 20. Gắn bó không giảm.
  - Chăm sóc cho tối đa 5 XP mỗi ngày.
- **Giao diện**:
  - Thẻ rồng ở Tổng quan.
  - Màn Thế giới rồng: XP, xu, lời thoại, 3 chỉ số.
  - Tab Nhiệm vụ và Lịch sử thưởng.
  - Hồ sơ: đổi tên, xem các giai đoạn.
  - Thông báo "+XP · +xu" và "lên cấp" hiện ở mọi màn.
- **Sao lưu VPS**: bản sao lưu mới có bảng rồng. Khôi phục bản cũ (v6–v8) chỉ thay dữ liệu tài chính, giữ nguyên tiến trình rồng.

## Chưa làm (giai đoạn 3) — trong app ghi rõ "Sắp mở"
- Nhà 3D, kéo thả nội thất theo lưới, Cửa hàng, Kho đồ, mua bằng xu.
- Xu kiếm được bây giờ được giữ nguyên để dùng sau.

## Đã kiểm tra
- 55 test đều đạt. 11 test riêng cho rồng:
  - chống trùng, kể cả 20 lần cộng cùng lúc
  - chuỗi ngày
  - ngưỡng mục tiêu
  - không vượt ngân sách
  - trần XP chăm sóc
  - lên cấp
  - báo cáo tuần
- Migration v4→v9 và sao lưu/khôi phục.
- Ảnh chụp màn hình: Thế giới rồng (sáng và tối), Nhiệm vụ, thẻ ở Tổng quan.

## Chưa kiểm tra được
- **Phần 3D Filament chưa chạy thử trên điện thoại thật** (máy này không có máy ảo hay thiết bị). Ảnh chụp trong test là ảnh dự phòng.
- Nếu 3D trên máy bạn quá tối hoặc lệch khung, cần chỉnh ánh sáng hoặc camera trong `dragon/render/DragonView.kt`.

## Cách thử
1. Cập nhật lên 1.7.0. App tự báo có bản mới, hoặc vào Cài đặt → Kiểm tra cập nhật.
2. Ghi một khoản chi. Thông báo "+20 XP · +5 xu" sẽ hiện ra.
3. Ở Tổng quan, chạm thẻ Mộc Mộc → vào tab Nhiệm vụ → bấm **Nhận** ở "Chốt sổ hôm nay".
4. Mở tab Báo cáo để nhận thưởng xem báo cáo tuần.
