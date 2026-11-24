# Tự sao lưu hằng đêm

Khi có **Mã kết nối VPS**, app đặt một JobScheduler job cần kết nối mạng trong khung 01:00–05:00 theo giờ điện thoại. Job gọi cùng `VpsBackup.upload()` như nút **Sao lưu ngay**. Sau mỗi lần chạy, app đặt lịch cho đêm sau. Lỗi mạng được JobScheduler thử lại với backoff tăng dần. Lịch được đặt khi mở app, lưu mã và khởi động lại điện thoại. Xóa mã sẽ hủy lịch.

Trạng thái lần chạy tự động gần nhất nằm trong SharedPreferences `vps_backup`: `last_nightly_backup_at` (epoch milliseconds) và `last_nightly_backup_result` (thành công hoặc lỗi ngắn). App chưa hiển thị trạng thái này trên màn hình vì `ScreenSettings.kt` đang có thay đổi chưa commit và không được sửa trong đợt này.

## Kiểm tra

Sau khi nhập mã kết nối, xem job bằng `adb shell dumpsys jobscheduler`. Chạy thử:

```sh
adb shell cmd jobscheduler run -f com.aistudio.chitieuthanhphat.qzkjmv 4101
```

Thay package bằng `applicationId` của bản APK. Job luân phiên ID `4101` và `4102`; nếu `4101` không còn trong danh sách, chạy `4102`. Lệnh ép chạy sẽ gửi bản sao lưu thật lên VPS, nên chỉ dùng khi muốn kiểm tra thực tế.

JobScheduler không bảo đảm chạy đúng 01:00 hoặc trước 05:00. Doze, mạng mất và giới hạn chạy nền của nhà sản xuất có thể làm chậm job, kể cả sau giờ bot báo cáo 07:00. Trên điện thoại, mở Cài đặt > Ứng dụng > Chi tiêu Thành Phát > Pin và cho phép chạy nền hoặc tắt tối ưu pin cho app; tên mục khác nhau theo hãng.

Khi phát hành bản mới, chủ app tự tăng `versionCode` và `versionName` trong `app/build.gradle.kts` trước khi xây APK.
