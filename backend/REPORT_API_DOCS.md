# Report Management API Documentation

## Tổng Quan

Module báo cáo vi phạm cho phép người dùng báo cáo các bài viết vi phạm nội quy, và admin quản lý các báo cáo này.

## Các Mã Báo Cáo (Report Codes)

- **FRAUD** - Gian lận
- **COUNTERFEIT** - Hàng giả
- **FORBIDDEN** - Mục bị cấm
- **WRONG_CAT** - Danh mục sai
- **SOLD_OUT** - Đã bán hết

## Trạng Thái Báo Cáo (Report Status)

- **PENDING** - Chờ xử lý
- **REVIEWING** - Đang xem xét
- **RESOLVED** - Đã xử lý
- **REJECTED** - Bị từ chối

## API Endpoints

### 1. Tạo Báo Cáo

**POST** `/api/reports`

**Content-Type:** `multipart/form-data`

**Request:**

| Field  | Type          | Required | Description                                    |
| ------ | ------------- | -------- | ---------------------------------------------- |
| report | JSON (String) | Yes      | Dữ liệu báo cáo dạng JSON string               |
| images | File[]        | No       | Ảnh báo cáo (tối đa 2 file, max 10MB mỗi file) |

**Report JSON Schema:**

```json
{
  "code": "FRAUD",
  "reason": "Sản phẩm không đúng như mô tả",
  "description": "Sản phẩm được quảng cáo là iPhone 15 nhưng thực tế là iPhone 14",
  "itemId": "item123"
}
```

**Hỗ trợ định dạng ảnh:** jpg, jpeg, png, gif, webp

**cURL Example:**

```bash
curl -X POST http://localhost:8080/api/reports \
  -H "Authorization: Bearer <access_token>" \
  -F 'report={"code":"FRAUD","reason":"Sản phẩm không đúng như mô tả","description":"Chi tiết","itemId":"item123"}' \
  -F "images=@/path/to/image1.jpg" \
  -F "images=@/path/to/image2.jpg"
```

**Response:** `201 Created`

```json
{
  "id": "report001",
  "reporterId": "user123",
  "code": "FRAUD",
  "reason": "Sản phẩm không đúng như mô tả",
  "description": "Sản phẩm được quảng cáo là iPhone 15 nhưng thực tế là iPhone 14",
  "status": "PENDING",
  "itemId": "item123",
  "reportImages": [
    {
      "id": "img001",
      "imageUrl": "https://res.cloudinary.com/..."
    },
    {
      "id": "img002",
      "imageUrl": "https://res.cloudinary.com/..."
    }
  ],
  "createdAt": "2026-05-11T10:30:00",
  "resolvedAt": null,
  "assignedStaffId": null,
  "adminNote": null
}
```

---

### 2. Lấy Thông Tin Chi Tiết Báo Cáo

**GET** `/api/reports/{reportId}`

**Response:** `200 OK`

```json
{
  "id": "report001",
  "reporterId": "user123",
  "code": "FRAUD",
  "reason": "Sản phẩm không đúng như mô tả",
  "status": "PENDING",
  "itemId": "item123",
  "reportImages": [...],
  "createdAt": "2026-05-11T10:30:00",
  "resolvedAt": null,
  "adminNote": null
}
```

---

### 3. Lấy Báo Cáo Theo Bài Viết

**GET** `/api/reports/item/{itemId}?page=0&size=10`

**Response:** `200 OK`

```json
{
  "content": [
    {...},
    {...}
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### 4. Lấy Báo Cáo Của Người Dùng Hiện Tại

**GET** `/api/reports/reporter/my-reports?page=0&size=10`

**Response:** `200 OK` - Trả về các báo cáo mà người dùng hiện tại đã gửi

---

### 5. Lấy Báo Cáo Theo Trạng Thái (Admin)

**GET** `/api/reports/status/{status}?page=0&size=10`

**Parameters:**

- `status`: PENDING, REVIEWING, RESOLVED, REJECTED

**Response:** `200 OK`

---

### 6. Cập Nhật Trạng Thái Báo Cáo (Admin)

**PATCH** `/api/reports/{reportId}/status?status=REVIEWING&adminNote=Đang kiểm tra`

**Response:** `200 OK`

```json
{
  "id": "report001",
  "reporterId": "user123",
  "code": "FRAUD",
  "status": "REVIEWING",
  "itemId": "item123",
  "createdAt": "2026-05-11T10:30:00",
  "resolvedAt": null,
  "adminNote": "Đang kiểm tra"
}
```

---

### 7. Xóa Báo Cáo (Admin)

**DELETE** `/api/reports/{reportId}`

**Response:** `200 OK`

```json
{
  "message": "Báo cáo đã được xóa thành công"
}
```

---

### 8. Lấy Số Báo Cáo Chưa Xử Lý (Admin Dashboard)

**GET** `/api/reports/stats/pending-count`

**Response:** `200 OK`

```json
5
```

---

## Database Schema

### reports table

```sql
CREATE TABLE reports (
    id VARCHAR(255) PRIMARY KEY,
    reporter_id VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    item_id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME,
    assigned_staff_id VARCHAR(255),
    admin_note TEXT,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);
```

### report_images table

```sql
CREATE TABLE report_images (
    id VARCHAR(255) PRIMARY KEY,
    report_id VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);
```

---

## Các Ràng Buộc

- Mỗi báo cáo có tối đa **2 ảnh**
- Chỉ có admin mới có thể cập nhật trạng thái báo cáo
- Khi xóa bài viết, tất cả báo cáo liên quan sẽ bị xóa theo (CASCADE)
- Báo cáo không thể bị người dùng khác xóa (chỉ admin)

---

## Classes & Components

### Models

- `Report` - Entity báo cáo
- `ReportImage` - Entity ảnh báo cáo

### Enums

- `ReportCode` - Mã báo cáo
- `ReportStatus` - Trạng thái báo cáo

### DTOs

- `ReportRequest` - Request tạo báo cáo (không có images field, upload qua multipart)
- `ReportResponse` - Response báo cáo
- `ReportImageResponse` - Response ảnh báo cáo (auto generated từ uploaded images)

### Services

- `ReportService` - Interface service
- `ReportServiceImpl` - Implementation

### Controllers

- `ReportController` - REST controller

### Repositories

- `ReportRepository` - JPA repository
- `ReportImageRepository` - JPA repository

---

## Validation & Upload

### File Validation

- **Định dạng hỗ trợ:** jpg, jpeg, png, gif, webp
- **Kích thước tối đa:** 10MB per file
- **Số lượng:** Tối đa 2 ảnh
- **Processing:** Upload lên Cloudinary, lưu URL vào database

### Error Handling

- File rỗng → Skip và tiếp tục
- Định dạng không hợp lệ → BadRequestException
- File quá lớn → BadRequestException
- Upload lỗi → BadRequestException

---

## Ghi Chú Phát Triển

- Backend tự động upload ảnh lên Cloudinary (tương tự Item upload)
- Ảnh được validate về type, size trước upload
- Tối đa 2 ảnh, các ảnh vượt quá sẽ bị bỏ qua
- Sử dụng CloudinaryService cho image upload
- Cân nhắc thêm notification khi admin xử lý báo cáo
- Có thể thêm queue job để xử lý báo cáo tự động
- Cân nhắc thêm analytics để theo dõi tần suất báo cáo
