# Realtime Chat

Ứng dụng chat realtime dùng Spring Boot. Maven project nằm tại root repository.

Mã nguồn được chia theo chức năng trong `src/main/java/com/teamchat/realtime_chat`: `auth`, `user`, `conversation`, `message` và `notification`. Cấu hình chung nằm trong `config`; giao diện nằm trong `src/main/resources/static` và migration trong src/main/resources/db/migration`.

Xem [README_DOCKER.md](README_DOCKER.md) để biết trạng thái cấu hình Docker.

## Chạy Swing trong NetBeans

1. Mở dự án Maven ở thư mục gốc. Chạy `com.teamchat.realtime_chat.RealtimeChatApplication` với đối số `--spring.profiles.active=swing-local`. Từ PowerShell, có thể dùng:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=swing-local"
   ```

2. Khi máy chủ đã chạy trên `127.0.0.1:8080`, chạy lớp `com.teamchat.swing.ChatSpaceApp` trong NetBeans. Lớp này cũng có trong dự án Maven `swing-client`.

Đăng ký và đăng nhập bằng email và mật khẩu. Tài khoản và tin nhắn local được lưu riêng trong cơ sở dữ liệu H2 tại `~/.chatspace/`. Profile `swing-local` mở API đăng ký đơn giản chỉ cho ứng dụng trên cùng máy. Bản web dùng profile mặc định và xác thực OTP khi đăng ký.

H2 của Swing có 5 bảng nghiệp vụ và 3 bảng cho phiên đăng nhập và lịch sử migration. Khi nâng cấp database local từ bản 12 bảng, hãy dừng backend và sao lưu `~/.chatspace/chat.mv.db` trước khi chạy Flyway. Migration local bỏ các bảng tùy chọn web, lịch sử thông báo, chống trùng thông báo trình duyệt và dữ liệu nhập từ web cũ.

## Triển khai Vercel

Production: https://realtime-chat-six-sand.vercel.app

Dự án dùng Vercel Container Runtime (beta) để build `Dockerfile` và chạy cả Spring Boot lẫn giao diện. `vercel.json` chọn framework `container`; không xuất riêng thư mục static. Cần Java 21 để build thử trên máy:

```powershell
.\mvnw.cmd -B package
npx vercel@latest deploy --prod --yes --env PORT=8080
```

Đăng nhập bằng `npx vercel@latest login` nếu chưa có phiên Vercel. Khi kết nối qua Git, chọn framework Container và đặt biến môi trường `PORT=8080` trong Vercel. Spring Boot đọc `PORT`, mặc định dùng 8080 khi chạy cục bộ.

Giới hạn hiện tại: chat riêng cần xác thực thật để có `Principal`; danh tính demo mặc định bị tắt và không nên bật trên môi trường công khai. Tin nhắn chưa lưu vào database. Broker nằm trong bộ nhớ một instance; cần broker dùng chung để chat giữa các instance khi Vercel mở rộng. Xem [docs/websocket-demo.md](docs/websocket-demo.md) để thử demo cục bộ.
# Phiên đăng nhập

Ứng dụng lưu phiên trong MySQL bằng Spring Session; Flyway tạo các bảng phiên khi khởi động. Cookie `SESSION` có thời hạn tối đa 30 ngày kể từ khi được cấp; phiên trên server hết hạn sau 30 ngày không hoạt động. Đăng xuất hủy phiên trên server. Việc xóa cookie hoặc các chính sách chặn cookie của trình duyệt cũng có thể yêu cầu đăng nhập lại sớm hơn.

Mặc định cookie dùng `Secure`, `HttpOnly` và `SameSite=None` để frontend và backend ở khác domain có thể dùng chung phiên qua HTTPS. Khi chạy backend qua HTTP trên máy, đặt các biến môi trường `SESSION_COOKIE_SECURE=false` và `SESSION_COOKIE_SAME_SITE=Lax` cho tiến trình ứng dụng. Với frontend và backend khác site, trình duyệt chặn cookie bên thứ ba vẫn có thể ngăn đăng nhập; triển khai cùng site là lựa chọn ổn định hơn.

