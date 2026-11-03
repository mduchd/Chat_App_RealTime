# Chạy dự án bằng Docker

Cần Docker Desktop đang chạy với Linux containers và Docker Compose. Tại thư mục gốc dự án, chạy:

```sh
docker compose up --build -d
```

Mở http://localhost:8080 hoặc http://localhost:8080/ws-test.html. Xem trạng thái và log bằng `docker compose ps` và `docker compose logs -f`. Dừng bằng `docker compose down`; dữ liệu MySQL vẫn được giữ trong volume `mysql_data`.

Compose dùng thông số phát triển mặc định để có thể chạy ngay. Nếu cần thay đổi mật khẩu hoặc cổng, sao chép `.env.example` thành `.env` rồi sửa các giá trị trước khi chạy. `.env` không được commit. Các mật khẩu mặc định chỉ dành cho phát triển trên máy cá nhân. MySQL chỉ mở trên `127.0.0.1:3307`; ứng dụng mặc định dùng cổng `8080`.

MySQL chỉ áp dụng các biến tạo database, user và mật khẩu khi khởi tạo volume lần đầu. Khi thay đổi thông tin này cho database đã tồn tại, cần cập nhật trực tiếp trong MySQL hoặc chủ động tạo lại volume sau khi sao lưu dữ liệu cần giữ. `docker compose down -v` sẽ xóa dữ liệu database.

Hiện ứng dụng chưa có dependency và cấu hình kết nối MySQL; database được khởi động để nhóm phát triển tiếp, chưa lưu dữ liệu chat. Khi tích hợp, dùng hostname `mysql` và cổng `3306` từ container ứng dụng; từ máy host dùng `localhost` và cổng `MYSQL_PORT`.
