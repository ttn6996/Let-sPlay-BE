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
| `GET` | `/api/system/health` | Kiểm tra ứng dụng và DB; dùng cho health check của Render |
| `GET` | `/api/system/cron-ping` | Endpoint công khai cho cron bên ngoài; ghi heartbeat và tăng counter trong DB |
| `GET` | `/api/system/self-ping` | Ping nội bộ được BE gọi tự động mỗi 5 phút; yêu cầu khóa ngẫu nhiên nội bộ |

Cron bên ngoài gọi `GET https://<render-service>.onrender.com/api/system/cron-ping`; không cần token.

`guestId` là UUID do client tạo để giữ một membership trong cùng tab. `requestId` là UUID idempotency cho yêu cầu tạo phòng. API trả access token một lần; DB chỉ giữ SHA-256 hash. Đây là phiên khách cho MVP, chưa hỗ trợ khôi phục quyền hay xác thực danh tính người thật.

## Swagger UI và triển khai

- Swagger UI: `/swagger-ui.html`; OpenAPI JSON: `/v3/api-docs`.
- API docs được chia nhóm `01 - Rooms` và `02 - System and Cron`.
- CORS đọc `CORS_ALLOWED_ORIGIN_PATTERNS`, mặc định `*` theo cấu hình hiện tại. Không bật credential cookies.
- Render có thể build từ `Dockerfile`; Render tự cấp `PORT` và `RENDER_EXTERNAL_URL`. Chỉ cần cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` cho Neon. Health check path là `/api/system/health`.
- Không commit file `.env`. Dùng `.env.example` làm mẫu cấu hình local.
- FE trên Vercel cần `VITE_API_URL` trỏ đến URL BE công khai.
