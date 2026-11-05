# Let-sPlay-BE

Backend Spring Boot, Java 17, Spring Data JPA, Flyway và PostgreSQL.

## Chạy local

Tạo database PostgreSQL `letsplay`. Từ thư mục `source/LetsPlay-BE`, copy `.env.example` thành `.env`, điền mật khẩu PostgreSQL local rồi chạy:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
# Điền DB_PASSWORD trong .env
mvn spring-boot:run
```

Spring nạp `.env` khi ứng dụng chạy từ thư mục BE. Mặc định API chạy ở cổng `8082`; có thể đổi bằng `SERVER_PORT`. Flyway chạy migration tự động khi ứng dụng khởi động; Hibernate chỉ validate schema. CORS mặc định cho phép `http://localhost:5175`, có thể đổi bằng `CORS_ALLOWED_ORIGINS`.

## API hiện có

| Method | Path | Mục đích |
| --- | --- | --- |
| `POST` | `/api/rooms` | Tạo phòng và cấp membership chủ phòng |
| `POST` | `/api/rooms/preview` | Kiểm tra mã mời và xem thông tin phòng trước khi nhập tên |
| `POST` | `/api/rooms/join` | Tham gia bằng mã mời |
| `GET` | `/api/rooms/{roomId}` | Đọc phòng với header `Authorization: Bearer <accessToken>` |
| `GET` | `/api/rooms/{roomId}/events` | SSE bảo vệ bằng membership; client gửi `Last-Event-ID` là revision gần nhất |
| `GET` | `/api/system/health` | Kiểm tra ứng dụng và DB; dùng cho health check của Render |
| `GET` | `/api/system/cron-ping` | Endpoint công khai cho cron bên ngoài; ghi heartbeat và tăng counter trong DB |
| `GET` | `/api/system/self-ping` | Ping nội bộ được BE gọi tự động mỗi 5 phút; yêu cầu khóa ngẫu nhiên nội bộ |

Cron bên ngoài gọi `GET https://<render-service>.onrender.com/api/system/cron-ping`; không cần token.

`guestId` là UUID do client tạo để giữ một membership trong cùng tab. `requestId` là UUID idempotency cho yêu cầu tạo phòng. API trả access token một lần; DB chỉ giữ SHA-256 hash. Đây là phiên khách cho MVP, chưa hỗ trợ khôi phục quyền hay xác thực danh tính người thật.

## Swagger UI và triển khai

- Swagger UI: `/swagger-ui.html`; OpenAPI JSON: `/v3/api-docs`.
- API docs được chia nhóm `01 - Rooms` và `02 - System and Cron`.
- Realtime dùng SSE cho push và HTTP cho lệnh ghi. Mỗi thay đổi phòng tăng revision PostgreSQL; mỗi BE poll các phòng đang có subscriber mỗi giây để đồng bộ giữa nhiều instance. Stream gửi comment keep-alive mỗi 15 giây.
- CORS đọc `CORS_ALLOWED_ORIGIN_PATTERNS`, mặc định `*` theo cấu hình hiện tại. Không bật credential cookies.
- Render có thể build từ `Dockerfile`; Render tự cấp `PORT` và `RENDER_EXTERNAL_URL`. Chỉ cần cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` cho Neon. Health check path là `/api/system/health`.
- Không commit file `.env`. Dùng `.env.example` làm mẫu cấu hình local.
- FE trên Vercel cần `VITE_API_URL` trỏ đến URL BE công khai.

## Ẩn danh và thông báo

API phòng chỉ trả tên hiển thị của chính người đang xem; các thành viên khác nhận nhãn `Thành viên N`. Đề xuất có cờ `createdByMe` thay cho membership ID của người tạo. Phiếu vẫn có thể nhận biết ở phía cá nhân qua nhãn `Bạn`; các phiếu còn lại là `Ẩn danh`.

Khi thêm đề xuất, backend gửi sự kiện SSE `room.proposal-created` và gửi Web Push đến các subscription đã đăng ký trong phòng. Web Push cần khai báo `PUSH_VAPID_PUBLIC_KEY`, `PUSH_VAPID_PRIVATE_KEY` và `PUSH_VAPID_SUBJECT` trên Render hoặc `.env`. Tạo cặp khóa bằng `npx web-push generate-vapid-keys`; có thể để trống hai khóa nếu chỉ dùng thông báo trong phòng. FE đọc public key từ `/api/system/public-config`, không cần thêm biến môi trường FE.

Người dùng cần bấm **Bật thông báo đẩy** trong từng phòng để cấp quyền và đăng ký thiết bị. Push notification hỗ trợ rung ở các trình duyệt/thiết bị có hỗ trợ; âm báo và rung khi đang mở phòng cũng tùy quyền của trình duyệt và phần cứng.

## Poll chọn giờ tham gia

Chủ phòng có thể tạo tối đa 30 mốc giờ qua `PUT /api/rooms/{roomId}/availability/options`. Thành viên cập nhật lựa chọn của mình qua `PUT /api/rooms/{roomId}/availability`; API chỉ trả số lượng tổng hợp và cờ lựa chọn của viewer. Chủ phòng dùng `POST /api/rooms/{roomId}/availability/{slotId}/select` để cập nhật ngày giờ dự kiến. Flyway migration V9 tạo bảng mốc giờ và lựa chọn ẩn danh.
