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

## �️ Development Workflow

### Chế độ Development (Khuyến nghị)

Chạy databases bằng Docker, services chạy local với DevTools:

```bash
# Bước 1: Chạy databases
docker-compose -f docker-compose.dev.yml up -d

# Bước 2: Chạy auth-service từ IDE hoặc terminal
cd backend/auth-service
./mvnw spring-boot:run
```

| Thay đổi | Cần làm gì? |
|----------|-------------|
| Sửa code (.java) | ❌ Không - DevTools tự restart |
| Sửa application.properties | ❌ Không - DevTools tự restart |
| Thêm dependencies (pom.xml) | 🔄 Restart service |

### Chế độ Production/Test

```bash
docker-compose up --build -d
```

| Thay đổi | Cần `--build`? |
|----------|---------------|
| Sửa source code | ✅ Có |
| Sửa pom.xml | ✅ Có |
| Sửa Dockerfile | ✅ Có |
| Sửa docker-compose.yml | ❌ Không |

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
# Khởi động
docker-compose up -d                    # Start all
docker-compose up -d postgres           # Start specific service

# Dừng
docker-compose stop                     # Stop all (giữ data)
docker-compose down                     # Remove containers (giữ data)
docker-compose down -v                  # Remove all + XÓA DATA

# Logs
docker-compose logs -f auth-service     # Xem logs realtime

# Trạng thái
docker-compose ps                       # Xem status

# Truy cập database
docker-compose exec postgres psql -U postgres -d secondhand_auth_db
docker-compose exec redis redis-cli
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
2. **Xóa data**: `docker-compose down -v` sẽ **XÓA TOÀN BỘ DATA**
3. **Dev mode**: Dùng `docker-compose.dev.yml` + chạy service local để code nhanh hơn

# Để tự động rebuild docker khi sửa code thì luôn chạy:
# Lần đầu (build image + download deps):
docker compose -f docker-compose.dev.yml up --build

# Các lần sau:
docker compose -f docker-compose.dev.yml up

