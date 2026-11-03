# Demo chat riêng qua Spring WebSocket

Yêu cầu Java 21. Chạy tại thư mục gốc dự án bằng Maven Wrapper (lần đầu cần kết nối mạng để tải Maven):

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--chat.demo-identities=true'
```

Mở `http://localhost:8080/ws-test.html` trong hai phiên trình duyệt riêng. Kết nối với tên `alice` và `bob`; nhập tên phiên còn lại vào ô người nhận và gửi tin hai chiều. Tên chỉ gồm chữ không dấu, chữ số, `_` hoặc `-`, tối đa 32 ký tự. Trang sẽ tự thử kết nối lại nếu bị mất kết nối.

Giao thức: WebSocket thuần `/ws`, SockJS `/ws-sockjs`, gửi STOMP tới `/app/chat.private` với JSON `{"recipient":"bob","content":"Xin chào"}`, nhận tại `/user/queue/messages`. Sự kiện trả về có `sender`, `recipient`, `content`, `sentAt`.

Danh tính demo lấy từ tham số `user` trên URL kết nối, có thể bị giả mạo và chỉ được bật bằng tùy chọn trên. Không bật tùy chọn này trên môi trường công khai. Khi không bật, controller yêu cầu `Principal` do cơ chế xác thực thật cung cấp. Tin nhắn hiện chỉ được phát tới các phiên đang kết nối, chưa lưu vào cơ sở dữ liệu.

Để kiểm tra giao diện chính, mở `http://localhost:8080/` ở hai tab. Tab đầu dùng tài khoản Nguyễn Minh Đức; tại tab còn lại bấm tên tài khoản ở góc trên, chọn An Nguyễn. Mở cuộc hội thoại giữa hai tài khoản và gửi tin nhắn chữ hai chiều. Trạng thái kết nối hiển thị ở thanh trên cùng. Danh bạ và hội thoại mẫu nằm trên trình duyệt; mã người dùng mẫu được gửi làm danh tính demo. Tin nhắn chỉ được lưu cục bộ trên từng trình duyệt, chưa phải lịch sử từ server. Chat nhóm, hình ảnh và tệp chưa được tích hợp với backend chat riêng.
