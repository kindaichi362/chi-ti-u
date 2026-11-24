# Sao lưu VPS cho Chi tiêu Thành Phát

App Android gửi bản sao của tám bảng Room tới `https://backup.180-93-125-5.sslip.io/v1/backup` khi người dùng bấm **Sao lưu ngay** trong **Quản lý & cài đặt**. Nút **Khôi phục** lấy bản mới nhất và thay dữ liệu sau khi xác nhận. Trước khi thay, app giữ `before_vps_restore.json` trong bộ nhớ riêng của app.

Trên VPS, `chi-tieu-backup.service` chạy `backup_server.py` với user riêng, chỉ nghe `127.0.0.1:8766`. Caddy cấp HTTPS và chuyển tiếp host trên. Token nằm ở `/etc/chi-tieu-backup/token` (root và service đọc được). Dữ liệu nằm ở `/var/lib/chi-tieu-backup`, quyền `0700`, gồm `latest.json` và tối đa 10 bản có dấu thời gian. Dữ liệu trên VPS là JSON nguyên bản, chưa mã hóa ở trạng thái lưu trữ; chỉ root và service đọc được.

Để nhập mã kết nối vào điện thoại, chủ VPS lấy token qua SSH bằng `sudo cat /etc/chi-tieu-backup/token` rồi dán vào ô **Mã kết nối VPS**. Không đưa token vào Git. Nếu token bị lộ, tạo token mới bằng `sudo openssl rand -hex 32 | sudo tee /etc/chi-tieu-backup/token >/dev/null`, đặt lại quyền `root:chi-tieu-backup 0640`, rồi nhập mã mới trong app.

Sao lưu hiện là thao tác thủ công cho **một điện thoại**. Nó chưa giải quyết xung đột giữa nhiều điện thoại, chưa tự chạy nền. Bản mới dùng schema Room v8 và gồm lịch trả góp thẻ với tiền trả mỗi tháng. App vẫn khôi phục được bản sao lưu v6 và v7 cũ; v6 chưa có lịch trả góp, còn v7 sẽ giữ các khoản cũ ở chế độ chia tiền gốc đều.

## Cấp quyền đọc chỉ xem cho Thư ký (/v1/secretary/finance)

Endpoint `GET /v1/secretary/finance` cung cấp các khoản sắp đến hạn (hóa đơn định kỳ, trả góp thẻ, công nợ có hạn) cho Bot Thư ký nhắc việc mỗi sáng. Endpoint này chỉ đọc từ `latest.json`, tuyệt đối không trả số tiền, thông tin cá nhân và không cấp quyền sao lưu/ghi đè.

1. **Chủ VPS tự tạo khóa:**
   ```bash
   sudo openssl rand -hex 32 | sudo tee /etc/chi-tieu-backup/secretary.key >/dev/null
   sudo chown root:chi-tieu-backup /etc/chi-tieu-backup/secretary.key
   sudo chmod 0640 /etc/chi-tieu-backup/secretary.key
   ```
2. **Khởi động lại dịch vụ:**
   ```bash
   sudo systemctl restart chi-tieu-backup.service
   ```
3. **Kiểm tra bằng curl (đọc khóa từ file, không in khóa):**
   ```bash
   curl -s -H "X-Secretary-Key: $(sudo cat /etc/chi-tieu-backup/secretary.key)" https://backup.180-93-125-5.sslip.io/v1/secretary/finance
   ```
4. **Cách gỡ (tắt endpoint):**
   Khi thiếu file hoặc file rỗng, endpoint tự động trả về `503 Service Unavailable` và tắt, không mở công khai:
   ```bash
   sudo rm -f /etc/chi-tieu-backup/secretary.key
   sudo systemctl restart chi-tieu-backup.service
   ```

## Cập nhật Android trong app

Từ bản 1.6.0, app tự kiểm tra `https://download.180-93-125-5.sslip.io/latest.json` khi mở và có nút **Kiểm tra bản mới** trong Cài đặt. Khi `versionCode` trên VPS lớn hơn bản đang cài, app tải APK qua HTTPS, so mã SHA-256 rồi mở trình cài đặt Android. Người dùng cần xác nhận cài đặt theo yêu cầu của Android; bản mới cài đè và giữ dữ liệu nếu cùng chữ ký. Bản 1.5.0 chưa có chức năng kiểm tra nên cần cài đè bản 1.6.0 một lần.

Khi phát hành bản sau: tăng `versionCode`, giữ nguyên khóa ký, xây APK, đưa APK vào `/srv/chi-tieu-download/`, rồi cập nhật `latest.json` ở cùng thư mục bằng URL, mã SHA-256, `versionCode`, `versionName`. Tải APK lên trước, thay `latest.json` cuối cùng để app không thấy phiên bản khi tệp chưa sẵn sàng. File `server/latest.json` là metadata của bản đang phát hành.
