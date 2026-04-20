# Secondhand Platform Backend

Hệ thống backend cho nền tảng mua bán đồ cũ, sử dụng kiến trúc microservices.

## 📋 Services

| Service | Port | Mô tả |
|---------|------|-------|
| Kong Gateway | 8000, 8001 | API Gateway |
| Auth Service | 8081 | Xác thực & phân quyền |
| PostgreSQL | 5435 | Database chính |
| Redis | 6379 | Cache & Session |
| MongoDB | 27019 | NoSQL Database |
| RabbitMQ | 5672, 15672 | Message Queue |

---

## ⚙️ Development Workflow

### Chế độ Development (Khuyến nghị)

Mục tiêu: chạy backend hoàn toàn bằng Docker và tự restart service khi sửa code.

#### Yêu cầu

- Docker Desktop + Docker Compose v2.22+
- Chạy lệnh tại thư mục `secondhand-platform-backend`

#### Lần đầu sau khi clone

```bash
# Build image + start containers
docker compose -f docker-compose.dev.yml up -d --build

# Bật file watch để auto restart/rebuild theo thay đổi code
docker compose -f docker-compose.dev.yml watch
```

#### Các lần làm việc tiếp theo

```bash
# Start stack
docker compose -f docker-compose.dev.yml up -d

# Bật watch (giữ terminal này mở)
docker compose -f docker-compose.dev.yml watch
```

#### Khi bạn thay đổi code

| Bạn sửa gì? | Hệ thống làm gì? |
|-------------|------------------|
| `backend/*-service/src/**` | ✅ Tự sync + restart đúng service |
| `backend/*-service/pom.xml` | ✅ Tự rebuild image + recreate service |
| `Dockerfile.dev` hoặc `docker-compose.dev.yml` | 🔄 Chạy lại `up -d --build` |

### Chế độ Production/Test

```bash
docker compose up --build -d
```

---

## 📡 API Endpoints

### Auth Service (Direct - Port 8081)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/register/user` | Đăng ký user (tự tạo cart ở order-service) |
| POST | `/api/auth/register/admin` | Đăng ký admin |
| POST | `/api/auth/login/user` | Đăng nhập user |
| POST | `/api/auth/login/admin` | Đăng nhập admin |

### Qua Kong Gateway (Port 8000)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/auth/api/auth/register/user` | Đăng ký user |
| POST | `/auth/api/auth/register/admin` | Đăng ký admin |
| POST | `/auth/api/auth/login/user` | Đăng nhập user |
| POST | `/auth/api/auth/login/admin` | Đăng nhập admin |

### Ví dụ Request

**Register User:**
```json
POST /api/auth/register/user
{
    "fullName": "Nguyễn Văn A",
    "email": "nguyenvana@gmail.com",
    "phoneNumber": "0912345678",
    "password": "123456",
    "confirmPassword": "123456"
}
```

**Login User:**
```json
POST /api/auth/login/user
{
    "email": "nguyenvana@gmail.com",
    "password": "123456"
}
```

**Login Admin:**
```json
POST /api/auth/login/admin
{
    "email": "admin@gmail.com",
    "password": "123456"
}
```

---

## 🚀 Các lệnh Docker thường dùng

```bash

# Build + start dev stack (lần đầu)
docker compose -f docker-compose.dev.yml up -d --build

# Start dev stack
docker compose -f docker-compose.dev.yml up -d

# Auto restart/rebuild khi sửa code (giữ terminal chạy)
docker compose -f docker-compose.dev.yml watch

# Khởi động
docker compose -f docker-compose.dev.yml up -d                    # Start all
docker compose -f docker-compose.dev.yml up -d postgres           # Start specific service

# Dừng
docker compose -f docker-compose.dev.yml stop                     # Stop all (giữ data)
docker compose -f docker-compose.dev.yml down                     # Remove containers (giữ data)
docker compose -f docker-compose.dev.yml down -v                  # Remove all + XÓA DATA

# Xóa toàn bộ service hiện tại (khuyên dùng khi muốn reset stack)
docker compose -f docker-compose.dev.yml down -v --remove-orphans

# Nếu trước đó có chạy file docker-compose.yml thì xóa luôn stack đó
docker compose down -v --remove-orphans

# (Tùy chọn) Xóa luôn image build local để build lại từ đầu sạch 100%
docker compose -f docker-compose.dev.yml down --rmi local --remove-orphans

# Logs
docker compose -f docker-compose.dev.yml logs -f auth-service     # Xem logs realtime

# Trạng thái
docker compose -f docker-compose.dev.yml ps                       # Xem status

# Truy cập database
docker compose -f docker-compose.dev.yml exec postgres psql -U postgres -d secondhand_auth_db
docker compose -f docker-compose.dev.yml exec redis redis-cli
```

---

## 🌐 URLs

| Service | URL |
|---------|-----|
| Kong Gateway | http://localhost:8000 |
| Auth Service | http://localhost:8081 |
| RabbitMQ UI | http://localhost:15672 (admin/admin123) |

---

## ⚠️ Lưu ý

1. **Lần đầu chạy**: Cần thời gian pull images và khởi tạo databases
2. **Xóa data**: `docker compose -f docker-compose.dev.yml down -v` sẽ **XÓA TOÀN BỘ DATA**
3. **Auto restart khi sửa code**: Luôn chạy thêm `docker compose -f docker-compose.dev.yml watch`
4. **Dev compose có Kong**: có thể test qua gateway (`http://localhost:8000`) hoặc gọi trực tiếp service port (`8081`, `8082`, `8083`)

# chạy lại init.sql để khởi tạo lại database:
Get-Content backend/postgres/init.sql | docker exec -i ktpm-postgres psql -U postgres

# Cách chạy Hybrid (IntelliJ + Docker)
# Bước 1 — Khởi động infra trên Docker:
docker compose -f docker-compose.infra.yml up -d


# Lệnh này chỉ chạy: Kong (port 8000), PostgreSQL (5435), Redis (6379), MongoDB (27019), RabbitMQ (5672/15672).

# Bước 2 — Chạy từng service trong IntelliJ bình thường (Run/Debug từng module). Không cần set thêm env var nào vì các application.properties đã có default đúng cho local: