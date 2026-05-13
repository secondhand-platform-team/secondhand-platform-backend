# Report API Documentation - FrontEnd Guide

**Base URL:** `http://localhost:8082/api`  
**Last Updated:** May 12, 2026

---

## 📌 Overview

Báo cáo vi phạm (Report) cho phép user báo cáo bài viết vi phạm nội quy. Admin/Staff quản lý và xử lý báo cáo.

**Report Codes:** FRAUD (Gian lận), COUNTERFEIT (Hàng giả), FORBIDDEN (Hàng cấm), WRONG_CAT (Sai danh mục), SOLD_OUT (Đã bán)

**Report Status:** PENDING (Chờ) → REVIEWING (Đang xem) → RESOLVED (Xong) hoặc REJECTED (Từ chối)

---

## 🔑 Authentication

Tất cả API cần JWT token trong Cookie. Login trước:

```
POST /login/client          (User)
POST /login/admin           (Admin/Staff)
```

---

## 📡 API Endpoints

### 1. Tạo Báo Cáo (User)

```http
POST /reports
Content-Type: multipart/form-data
```

**Request:**

```json
{
  "report": "{\"code\":\"FRAUD\",\"reason\":\"Sản phẩm không đúng như mô tả\",\"description\":\"Quảng cáo iPhone 15 nhưng thực tế là iPhone 14\",\"itemId\":\"item-123\"}",
  "images": [file1.jpg, file2.jpg]  // Optional, max 2 files, 10MB each
}
```

**Response: 201 Created**

```json
{
  "id": "report-001",
  "reporterId": "user-123",
  "code": "FRAUD",
  "reason": "Sản phẩm không đúng như mô tả",
  "description": "Quảng cáo iPhone 15 nhưng thực tế là iPhone 14",
  "status": "PENDING",
  "itemId": "item-123",
  "reportImages": [
    {
      "id": "img-001",
      "imageUrl": "https://res.cloudinary.com/..."
    },
    {
      "id": "img-002",
      "imageUrl": "https://res.cloudinary.com/..."
    }
  ],
  "createdAt": "2026-05-12T10:30:00",
  "resolvedAt": null,
  "assignedStaffId": null,
  "adminNote": null
}
```

**cURL:**

```bash
curl -X POST http://localhost:8082/api/reports \
  -F 'report={"code":"FRAUD","reason":"...","description":"...","itemId":"item-123"}' \
  -F "images=@photo1.jpg" \
  -F "images=@photo2.jpg"
```

---

### 2. Lấy Danh Sách Báo Cáo Của User

```http
GET /reports/reporter/my-reports?page=0&size=10
```

**Response: 200 OK**

```json
{
  "content": [
    {
      "id": "report-001",
      "code": "FRAUD",
      "reason": "Sản phẩm không đúng",
      "status": "PENDING",
      "createdAt": "2026-05-12T10:30:00",
      "reportImages": [
        {
          "id": "img-001",
          "imageUrl": "https://res.cloudinary.com/..."
        }
      ]
    },
    {
      "id": "report-002",
      "code": "COUNTERFEIT",
      "reason": "Hàng giả",
      "status": "RESOLVED",
      "createdAt": "2026-05-11T14:20:00",
      "reportImages": []
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### 3. Lấy Chi Tiết Báo Cáo

```http
GET /reports/{reportId}
```

**Response: 200 OK**

```json
{
  "id": "report-001",
  "reporterId": "user-123",
  "code": "FRAUD",
  "reason": "Sản phẩm không đúng như mô tả",
  "description": "Quảng cáo iPhone 15 nhưng thực tế là iPhone 14",
  "status": "REVIEWING",
  "itemId": "item-123",
  "reportImages": [
    {
      "id": "img-001",
      "imageUrl": "https://res.cloudinary.com/..."
    }
  ],
  "createdAt": "2026-05-12T10:30:00",
  "resolvedAt": null,
  "assignedStaffId": "staff-456",
  "adminNote": "Đang kiểm tra lại bài viết"
}
```

---

### 4. Lấy Báo Cáo Theo Bài Viết (Admin/Staff)

```http
GET /reports/item/{itemId}?page=0&size=10
```

**Response: 200 OK**

```json
{
  "content": [
    {
      "id": "report-001",
      "code": "FRAUD",
      "reason": "Sản phẩm không đúng",
      "status": "PENDING",
      "reporterId": "user-123",
      "createdAt": "2026-05-12T10:30:00"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### 5. Lấy Báo Cáo Chưa Xử Lý (Admin/Staff Dashboard)

```http
GET /reports/status/PENDING?page=0&size=10
```

**Response: 200 OK**

```json
{
  "content": [
    {
      "id": "report-001",
      "code": "FRAUD",
      "reason": "Sản phẩm không đúng",
      "status": "PENDING",
      "itemId": "item-123",
      "reporterId": "user-123",
      "createdAt": "2026-05-12T10:30:00",
      "assignedStaffId": null,
      "reportImages": [
        {
          "id": "img-001",
          "imageUrl": "https://res.cloudinary.com/..."
        }
      ]
    }
  ],
  "totalElements": 8,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### 6. Cập Nhật Trạng Thái Báo Cáo (Admin/Staff)

```http
PATCH /reports/{reportId}/status?status=REVIEWING&adminNote=Đang kiểm tra lại
```

**Response: 200 OK**

```json
{
  "id": "report-001",
  "status": "REVIEWING",
  "adminNote": "Đang kiểm tra lại",
  "assignedStaffId": "staff-456"
}
```

**Allowed Status:**

- `REVIEWING` - Đang xem xét
- `RESOLVED` - Đã xử lý (bài viết bị xóa)
- `REJECTED` - Bị từ chối

---

### 7. Assign Nhân Viên Xử Lý (Admin Only)

```http
PATCH /reports/{reportId}/assign-staff?staffId=staff-456
```

**Response: 200 OK**

```json
{
  "id": "report-001",
  "assignedStaffId": "staff-456",
  "status": "REVIEWING"
}
```

---

### 8. Xóa Báo Cáo (Admin Only)

```http
DELETE /reports/{reportId}
```

**Response: 200 OK**

```json
{
  "message": "Báo cáo đã được xóa thành công"
}
```

---

### 9. Lấy Số Báo Cáo Chưa Xử Lý (Admin Dashboard)

```http
GET /reports/stats/pending-count
```

**Response: 200 OK**

```
8
```

---

## 💻 Frontend Implementation Examples

### React - Create Report

```javascript
async function createReport(
  itemId,
  reportCode,
  reason,
  description,
  imageFiles = [],
) {
  const formData = new FormData();

  // Add report JSON
  formData.append(
    "report",
    JSON.stringify({
      code: reportCode,
      reason: reason,
      description: description,
      itemId: itemId,
    }),
  );

  // Add images (max 2)
  imageFiles.slice(0, 2).forEach((file) => {
    formData.append("images", file);
  });

  const res = await fetch("http://localhost:8082/api/reports", {
    method: "POST",
    body: formData,
    credentials: "include",
  });

  if (!res.ok) throw new Error("Failed to create report");
  return await res.json();
}

// Usage
createReport("item-123", "FRAUD", "Sản phẩm không đúng", "Chi tiết...", [
  file1,
  file2,
])
  .then((report) => console.log("Report created:", report.id))
  .catch((err) => console.error(err));
```

### React - Get My Reports

```javascript
async function getMyReports(page = 0, size = 10) {
  const res = await fetch(
    `http://localhost:8082/api/reports/reporter/my-reports?page=${page}&size=${size}`,
    { credentials: "include" },
  );

  if (!res.ok) throw new Error("Failed to fetch reports");
  return await res.json();
}

// Usage
getMyReports(0, 10)
  .then((data) => console.log("Reports:", data.content))
  .catch((err) => console.error(err));
```

### React - Get Pending Reports (Admin)

```javascript
async function getPendingReports(page = 0, size = 10) {
  const res = await fetch(
    `http://localhost:8082/api/reports/status/PENDING?page=${page}&size=${size}`,
    { credentials: "include" },
  );

  if (!res.ok) throw new Error("Failed to fetch reports");
  return await res.json();
}
```

### React - Update Report Status

```javascript
async function updateReportStatus(reportId, newStatus, adminNote = "") {
  const res = await fetch(
    `http://localhost:8082/api/reports/${reportId}/status?status=${newStatus}&adminNote=${encodeURIComponent(adminNote)}`,
    {
      method: "PATCH",
      credentials: "include",
    },
  );

  if (!res.ok) throw new Error("Failed to update status");
  return await res.json();
}

// Usage
updateReportStatus("report-001", "REVIEWING", "Đang kiểm tra")
  .then((data) => console.log("Updated:", data))
  .catch((err) => console.error(err));
```

### React - Assign Staff

```javascript
async function assignStaff(reportId, staffId) {
  const res = await fetch(
    `http://localhost:8082/api/reports/${reportId}/assign-staff?staffId=${staffId}`,
    {
      method: "PATCH",
      credentials: "include",
    },
  );

  if (!res.ok) throw new Error("Failed to assign staff");
  return await res.json();
}
```

---

## 🎯 Typical User Flows

### Flow 1: User Reports an Item

```
1. User opens item detail page
2. Clicks "Report" button
3. Opens modal with form:
   - Report Code (dropdown)
   - Reason (text)
   - Description (textarea)
   - Image upload (max 2)
4. Submits → POST /reports
5. Show success message + report ID
6. Redirect to /my-reports or close modal
```

### Flow 2: Admin Manages Reports

```
1. Admin opens Admin Portal
2. Dashboard shows: GET /reports/stats/pending-count → "8 pending"
3. Clicks "View Pending Reports"
4. Lists: GET /reports/status/PENDING
5. Clicks on report → GET /reports/{id}
6. Options:
   - PATCH /assign-staff (assign to staff)
   - PATCH /status (change to REVIEWING)
   - Can add admin note
7. When resolved:
   - PATCH /status?status=RESOLVED (delete item)
   - Or PATCH /status?status=REJECTED (false report)
```

### Flow 3: Staff Reviews Reports

```
1. Staff logs in
2. Dashboard shows assigned reports
3. GET /reports/reporter/my-reports (personal reports)
   OR GET /reports/status/REVIEWING (assigned to me)
4. Reviews report details + images
5. Updates status:
   - REVIEWING → RESOLVED (if valid, delete item)
   - REVIEWING → REJECTED (if invalid)
6. Adds admin note explaining decision
```

---

## 🖼️ Report Codes Explanation

| Code            | Meaning                 | Example                             |
| --------------- | ----------------------- | ----------------------------------- |
| **FRAUD**       | Gian lận, quảng cáo sai | Nói iPhone 15 nhưng là iPhone 14    |
| **COUNTERFEIT** | Hàng giả, nhập lậu      | Đồ hiệu fake, không chính hãng      |
| **FORBIDDEN**   | Hàng cấm, bất hợp pháp  | Bán vũ khí, chất cấm                |
| **WRONG_CAT**   | Danh mục sai            | Đẩy điện thoại sang danh mục laptop |
| **SOLD_OUT**    | Đã bán hết              | Bài viết không còn hàng             |

---

## ✅ Validation Rules

### File Upload

- **Format:** jpg, jpeg, png, gif, webp
- **Size:** Max 10MB per file
- **Quantity:** Max 2 files
- **Extra files:** Silently skipped

### Form Validation

- `code`: Required, one of 5 codes
- `reason`: Required, text
- `description`: Required, detailed text
- `itemId`: Required, valid item ID
- Images: Optional, max 2

---

## ❌ Error Responses

### 400 Bad Request

```json
{
  "error": "Bad Request",
  "message": "Invalid report JSON",
  "status": 400
}
```

### 401 Unauthorized

```json
{
  "error": "Unauthorized",
  "message": "User not authenticated",
  "status": 401
}
```

### 404 Not Found

```json
{
  "error": "Not Found",
  "message": "Report not found",
  "status": 404
}
```

### 403 Forbidden

```json
{
  "error": "Forbidden",
  "message": "Only ADMIN can delete reports",
  "status": 403
}
```

---

## 📋 Quick Reference

| Endpoint                       | Method | Auth        | Purpose              |
| ------------------------------ | ------ | ----------- | -------------------- |
| `/reports`                     | POST   | USER        | Create report        |
| `/reports/{id}`                | GET    | ANY         | Get report detail    |
| `/reports/reporter/my-reports` | GET    | USER        | Get my reports       |
| `/reports/item/{itemId}`       | GET    | ANY         | Get reports for item |
| `/reports/status/PENDING`      | GET    | ADMIN/STAFF | Get pending reports  |
| `/reports/{id}/status`         | PATCH  | ADMIN/STAFF | Update status        |
| `/reports/{id}/assign-staff`   | PATCH  | ADMIN       | Assign staff         |
| `/reports/{id}`                | DELETE | ADMIN       | Delete report        |
| `/reports/stats/pending-count` | GET    | ADMIN/STAFF | Pending count        |

---

## 🚀 Getting Started

1. **Login** → Get JWT token
2. **Create Report** → POST /reports
3. **View My Reports** → GET /reports/reporter/my-reports
4. **Admin View** → GET /reports/status/PENDING
5. **Update Status** → PATCH /reports/{id}/status

That's it! Start building! 💪
