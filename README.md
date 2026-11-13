# Realtime Chat

Ứng dụng chat realtime dùng Spring Boot. Maven project nằm tại root repository.

Mã nguồn được chia theo chức năng trong `src/main/java/com/teamchat/realtime_chat`: `auth`, `user`, `conversation`, `message` và `notification`. Cấu hình chung nằm trong `config`; giao diện nằm trong `src/main/resources/static` và migration trong src/main/resources/db/migration`.

Xem [README_DOCKER.md](README_DOCKER.md) để biết trạng thái cấu hình Docker.

## Triển khai Vercel

Production: https://realtime-chat-six-sand.vercel.app

Dự án dùng Vercel Container Runtime (beta) để build `Dockerfile` và chạy cả Spring Boot lẫn giao diện. `vercel.json` chọn framework `container`; không xuất riêng thư mục static. Cần Java 21 để build thử trên máy:

```powershell
.\mvnw.cmd -B package
npx vercel@latest deploy --prod --yes --env PORT=8080
```

Đăng nhập bằng `npx vercel@latest login` nếu chưa có phiên Vercel. Khi kết nối qua Git, chọn framework Container và đặt biến môi trường `PORT=8080` trong Vercel. Spring Boot đọc `PORT`, mặc định dùng 8080 khi chạy cục bộ.

Giới hạn hiện tại: chat riêng cần xác thực thật để có `Principal`; danh tính demo mặc định bị tắt và không nên bật trên môi trường công khai. Tin nhắn chưa lưu vào database. Broker nằm trong bộ nhớ một instance; cần broker dùng chung để chat giữa các instance khi Vercel mở rộng. Xem [docs/websocket-demo.md](docs/websocket-demo.md) để thử demo cục bộ.
