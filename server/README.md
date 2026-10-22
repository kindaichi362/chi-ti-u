# Sao lưu VPS cho Chi tiêu Thành Phát

App Android gửi bản sao của bảy bảng Room tới `https://backup.180-93-125-5.sslip.io/v1/backup` khi người dùng bấm **Sao lưu ngay** trong **Quản lý & cài đặt**. Nút **Khôi phục** lấy bản mới nhất và thay dữ liệu sau khi xác nhận. Trước khi thay, app giữ `before_vps_restore.json` trong bộ nhớ riêng của app.

Trên VPS, `chi-tieu-backup.service` chạy `backup_server.py` với user riêng, chỉ nghe `127.0.0.1:8766`. Caddy cấp HTTPS và chuyển tiếp host trên. Token nằm ở `/etc/chi-tieu-backup/token` (root và service đọc được). Dữ liệu nằm ở `/var/lib/chi-tieu-backup`, quyền `0700`, gồm `latest.json` và tối đa 10 bản có dấu thời gian. Dữ liệu trên VPS là JSON nguyên bản, chưa mã hóa ở trạng thái lưu trữ; chỉ root và service đọc được.

Để nhập mã kết nối vào điện thoại, chủ VPS lấy token qua SSH bằng `sudo cat /etc/chi-tieu-backup/token` rồi dán vào ô **Mã kết nối VPS**. Không đưa token vào Git. Nếu token bị lộ, tạo token mới bằng `sudo openssl rand -hex 32 | sudo tee /etc/chi-tieu-backup/token >/dev/null`, đặt lại quyền `root:chi-tieu-backup 0640`, rồi nhập mã mới trong app.

Sao lưu hiện là thao tác thủ công cho **một điện thoại**. Nó chưa giải quyết xung đột giữa nhiều điện thoại, chưa tự chạy nền. Khi khôi phục, tất cả bảy bảng phải cùng schema Room v6; bản khác phiên bản sẽ bị từ chối trước khi thay dữ liệu.

## Cập nhật Android trong app

Từ bản 1.6.0, app tự kiểm tra `https://download.180-93-125-5.sslip.io/latest.json` khi mở và có nút **Kiểm tra bản mới** trong Cài đặt. Khi `versionCode` trên VPS lớn hơn bản đang cài, app tải APK qua HTTPS, so mã SHA-256 rồi mở trình cài đặt Android. Người dùng cần xác nhận cài đặt theo yêu cầu của Android; bản mới cài đè và giữ dữ liệu nếu cùng chữ ký. Bản 1.5.0 chưa có chức năng kiểm tra nên cần cài đè bản 1.6.0 một lần.

Khi phát hành bản sau: tăng `versionCode`, giữ nguyên khóa ký, xây APK, đưa APK vào `/srv/chi-tieu-download/`, rồi cập nhật `latest.json` ở cùng thư mục bằng URL, mã SHA-256, `versionCode`, `versionName`. Tải APK lên trước, thay `latest.json` cuối cùng để app không thấy phiên bản khi tệp chưa sẵn sàng. File `server/latest.json` là metadata của bản 1.6.0.
