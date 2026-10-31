# Docker

`Dockerfile` nằm tại root của repository và đóng gói ứng dụng Spring Boot bằng Java 21.

`compose.yaml` hiện là tệp khung, chưa cấu hình MySQL hoặc profile `full`. Nhóm cần hoàn thiện cấu hình trước khi chạy Docker Compose theo kế hoạch. Sao chép `.env.example` thành `.env` và điền giá trị riêng khi cấu hình môi trường; không commit `.env`.
