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

`guestId` là UUID do client tạo để giữ một membership trong cùng tab. `requestId` là UUID idempotency cho yêu cầu tạo phòng. API trả access token một lần; DB chỉ giữ SHA-256 hash. Đây là phiên khách cho MVP, chưa hỗ trợ khôi phục quyền hay xác thực danh tính người thật.
