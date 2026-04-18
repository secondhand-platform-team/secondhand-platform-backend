# Items API - Repository & Implementation Details

## 📂 Repository Layer

### ItemRepository Interface

```java
package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.Item;
import com.secondhand.coreservice.model.enums.ItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {

    // Lấy tin theo danh mục
    List<Item> findByCategory_CategoryId(String categoryId);

    // Lấy tin theo danh mục slug với status cụ thể
    List<Item> findByCategory_SlugAndStatus(String slug, ItemStatus status);

    // Lấy tin theo danh mục slug với danh sách status
    List<Item> findByCategory_SlugAndStatusIn(String slug, List<ItemStatus> statuses);

    // Lấy tất cả tin của user
    List<Item> findByUserId(String userId);

    // Lấy tin theo ID
    Optional<Item> findByItemId(String itemId);
}
```

### FavoriteItemRepository Interface

```java
package com.secondhand.coreservice.repository;

import com.secondhand.coreservice.model.FavoriteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, String> {

    // Kiểm tra tin đã được yêu thích
    boolean existsByUserIdAndItem_ItemId(String userId, String itemId);

    // Lấy danh sách tin yêu thích của user
    List<FavoriteItem> findByUserId(String userId);

    // Lấy chi tiết tin yêu thích
    Optional<FavoriteItem> findByUserIdAndItem_ItemId(String userId, String itemId);
}
```

---

## 🔧 Service Implementation Details

### ItemServiceImpl - Key Methods

#### 1. createItem() & createItemInternal()

```
Luồng:
1. Lấy userId từ SecurityContext (JWT Token)
2. Xác định loại giao dịch (SELL/GIVE_AWAY)
3. Verify payment (nếu SELL)
4. Validate category tồn tại
5. Validate price (SELL > 0, GIVE_AWAY >= 0)
6. Lập status ban đầu:
   - GIVE_AWAY → ACTIVE (ngay lập tức)
   - SELL → DRAFT (chờ thanh toán)
7. Tạo Item với IdGenerator.generateItemId()
8. Tạo Location (nếu có)
9. Upload images lên Cloudinary
10. Lưu ItemImages
11. Tạo payment URL (cho SELL)
12. Lưu ItemAttributeValues
13. Return ItemResponse

Edge Cases:
- Nếu payment tạo fail → xóa item vừa tạo
- Nếu file trống → skip
- Nếu attribute required mà null → throw error
```

#### 2. processAndUploadImages()

```
Luồng:
1. Validate file type (image/*)
2. Validate file size <= 10MB
3. Upload lên Cloudinary
4. Lưu URL vào ItemImageRequest
5. Đánh dấu ảnh đầu tiên làm primary

Mảng return:
ItemImageRequest {
  imageUrl: "https://res.cloudinary.com/...",
  isPrimary: true/false
}
```

#### 3. updateItem()

```
Luồng:
1. Lấy currentUserId từ Security
2. Tìm item theo ID
3. Xác thực owner (userId == currentUserId)
4. Validate category tồn tại
5. Validate price > 0
6. Cập nhật các field:
   - Title, Description
   - Category, Price
   - Condition, TransactionType, Status
7. Cập nhật Location (tạo mới nếu chưa có)
8. Cập nhật images (xóa cũ + thêm mới)
9. Cập nhật attributes (xóa cũ + thêm mới)
10. updatedAt = now()
11. Return ItemResponse

Quy tắc:
- Cascade delete old images
- Cascade delete old attributes
- Giữ nguyên createdAt
```

#### 4. updateItemStatus()

```
Luồng:
1. Lấy currentUserId từ Security
2. Tìm item theo ID
3. Xác thực owner
4. Parse & validate status enum
5. Cập nhật status
6. updatedAt = now()
7. Return ItemResponse

Valid Status:
- AVAILABLE, RESERVED, SOLD, HIDDEN, ACTIVE
```

#### 5. deleteItem()

```
Luồng:
1. Lấy currentUserId từ Security
2. Tìm item theo ID
3. Xác thực owner
4. deleteById() (cascade delete tất cả children)
5. Return MessageResponse

Cascade Delete:
- ItemImages
- ItemAttributeValues
- Location
- FavoriteItems
- Reviews
- GiveawayRequests
- Reports
- Notifications
```

#### 6. handleVNPayCallback()

```
Luồng:
1. Nhận VNPayCallbackRequest
2. Xác thực secure hash
3. Nếu responseCode == "00":
   - Tìm item theo transactionId
   - Cập nhật status → ACTIVE
   - Gọi PaymentEventService
   - Lưu item
   - Return success message
4. Nếu fail:
   - Log error
   - Xóa draft item hoặc keep it
   - Return error message
```

#### 7. buildAttributeValue()

```
Luồng:
1. Lấy CategoryAttribute từ DB
2. Lấy dataType từ attribute
3. Convert value theo dataType:
   - STRING → valueString
   - NUMBER → valueNumber (BigDecimal)
   - INTEGER → valueInteger (Long)
   - BOOLEAN → valueBoolean
   - DATE → valueDate (LocalDate)
   - ENUM → valueString
   - JSON → valueJson (ObjectMapper.writeValueAsString)
4. Tạo ItemAttributeValue
5. Return ItemAttributeValue
```

#### 8. mapToItemResponse()

```
Luồng:
1. Lấy item từ DB (joined load)
2. Mapping field:
   - Item → ItemResponse (1:1 fields)
   - ItemImages → ItemImageResponse[]
   - Location → LocationResponse
   - Attributes → ItemAttributeResponse[]
3. Include:
   - transactionId
   - paymentUrl
4. Return ItemResponse
```

---

## 🔐 Security Context

### getCurrentUserId()

```java
private String getCurrentUserId() {
    Object principal = SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();

    if (principal instanceof JwtAuthenticatedUser) {
        return ((JwtAuthenticatedUser) principal).getUserId();
    }
    throw new UnauthorizedException("User not authenticated");
}
```

### Authorization Flow

```
1. Request đến controller
2. JWT Filter xác thực token
3. Lấy userId từ token payload
4. Lưu vào SecurityContext
5. Service gọi getCurrentUserId()
6. So sánh userId với item.userId
7. Nếu khác → throw BadRequestException
8. Nếu giống → allow operation
```

---

## 📦 DTOs - Field Mapping

### ItemRequest → Item Entity

```
ItemRequest.title → Item.title
ItemRequest.description → Item.description
ItemRequest.categoryId → Item.category (lookup)
ItemRequest.price → Item.price
ItemRequest.condition → Item.condition (enum)
ItemRequest.transactionType → Item.transactionType (enum)
ItemRequest.status → Item.status (enum)
ItemRequest.location → Location entity (1:1)
ItemRequest.itemImageList → ItemImage[] (1:N)
ItemRequest.attributes → ItemAttributeValue[] (1:N)
ItemRequest.transactionId → Item.transactionId (for payment)
```

### Item Entity → ItemResponse

```
Item.itemId → ItemResponse.itemId
Item.title → ItemResponse.title
Item.description → ItemResponse.description
Item.category.categoryId → ItemResponse.categoryId
Item.price → ItemResponse.price
Item.condition → ItemResponse.condition
Item.transactionType → ItemResponse.transactionType
Item.status → ItemResponse.status
Item.itemLocation → LocationResponse
Item.userId → ItemResponse.userId
Item.createdAt → ItemResponse.createdAt
Item.updatedAt → ItemResponse.updatedAt
Item.itemImageList → ItemImageResponse[]
Item.attributeValues → ItemAttributeResponse[]
Item.transactionId → ItemResponse.transactionId
Item.paymentUrl → ItemResponse.paymentUrl
```

---

## 🗂️ Related Entities

### Item Model Fields

```java
@Entity
@Table(name = "items")
public class Item {
    @Id
    private String itemId;                    // ITM + 14 digits

    @Column(columnDefinition = "TEXT")
    private String title;                     // Bắt buộc

    @Column(columnDefinition = "TEXT")
    private String description;               // Optional

    private BigDecimal price;                 // Bắt buộc

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;  // SELL/GIVE_AWAY

    @Enumerated(EnumType.STRING)
    private ItemCondition condition;          // NEW/LIKE_NEW/USED/FOR_PARTS

    @Enumerated(EnumType.STRING)
    private ItemStatus status;                // ACTIVE/DRAFT/RESERVED/SOLD/etc

    private Integer view;                     // Số lượt xem

    @Column(columnDefinition = "TEXT")
    private String location;                  // Deprecated - dùng Location entity

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;          // Auto-set

    private LocalDateTime updatedAt;          // Auto-update

    private String userId;                    // Foreign key to User

    private String transactionId;             // VNPay transaction ID

    @Column(columnDefinition = "TEXT")
    private String paymentUrl;                // VNPay payment URL

    private LocalDateTime paymentInitiatedAt; // Payment start time

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;                // Foreign key

    @OneToOne(mappedBy = "item", cascade = CascadeType.ALL)
    private Location itemLocation;            // 1:1

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemAttributeValue> attributeValues;  // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> itemImageList;   // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FavoriteItem> favoriteItems; // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviewList;         // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GiveawayRequest> giveawayRequestList; // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports;            // 1:N

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications; // 1:N
}

@PrePersist
protected void onPrePersist() {
    if (this.itemId == null) {
        this.itemId = IdGenerator.generateItemId();
    }
    if (this.createdAt == null) {
        this.createdAt = LocalDateTime.now();
    }
}
```

### ItemImage Model

```java
@Entity
@Table(name = "item_images")
public class ItemImage {
    @Id
    private String imageId;                   // ITM_IMG + digits

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private String url;                       // Cloudinary URL

    private Boolean isPrimary;                // Main image

    private LocalDateTime createdAt;
}
```

### Location Model

```java
@Entity
@Table(name = "locations")
public class Location {
    @Id
    private String locationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private String address;                   // VD: "123 Nguyen Hue"
    private String ward;                      // VD: "Ben Thanh"
    private String district;                  // VD: "District 1"
    private String city;                      // VD: "Ho Chi Minh City"
}
```

### FavoriteItem Model

```java
@Entity
@Table(name = "favorite_items")
public class FavoriteItem {
    @Id
    private String favoriteId;

    private String userId;                    // Foreign key to User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    private LocalDateTime createdAt;
}
```

### ItemAttributeValue Model

```java
@Entity
@Table(name = "item_attribute_values")
public class ItemAttributeValue {
    @Id
    private String attributeValueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_attribute_id")
    private CategoryAttribute attribute;

    private String valueString;               // STRING, ENUM
    private BigDecimal valueNumber;           // NUMBER
    private Long valueInteger;                // INTEGER
    private Boolean valueBoolean;             // BOOLEAN
    private LocalDate valueDate;              // DATE
    private String valueJson;                 // JSON

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 🔄 Transaction Management

### @Transactional Usage

```java
// Read-only
@Transactional(readOnly = true)
public List<ItemResponse> getAllItems() { ... }

// Read-write (default)
@Transactional
public ItemResponse createItem(ItemRequest request) { ... }

// Nested transaction
private void processAndUploadImages(ItemRequest request, MultipartFile[] images) { ... }
```

---

## 🌐 External Integration

### CloudinaryService

```java
public interface CloudinaryService {
    String uploadImage(MultipartFile file) throws IOException;
}

// Trả về URL: https://res.cloudinary.com/account/image/upload/v123456789/filename.jpg
```

### PaymentEventService

```java
public interface PaymentEventService {
    CreatePaymentResponse createVnPayPayment(
        long amount,           // VND, tối thiểu 10k
        String bankCode,       // "NCB", "AGRIBANK"
        String language,       // "vn", "en"
        String userId
    );
}

// Returns:
// {
//   "code": "00",  // Success
//   "message": "Success",
//   "transactionId": "VNP123456789",
//   "paymentUrl": "https://sandbox.vnpayment.vn/paygate/pay?..."
// }
```

### UserServiceClient

```java
// Gọi auth-service để verify user
@FeignClient(name = "auth-service")
public interface UserServiceClient {
    // Methods...
}
```

---

## 📊 Payment Status Flow

```
SELL Item Lifecycle:
1. POST /api/items
   ↓
2. ItemStatus = DRAFT (chờ thanh toán)
   paymentUrl = VNPay URL
   paymentInitiatedAt = now()
   ↓
3. User redirect → paymentUrl
   ↓
4. User thanh toán trên VNPay
   ↓
5. VNPay callback → GET /api/items/payment-callback?vnp_*
   ↓
6. If responseCode == "00":
   - ItemStatus = ACTIVE
   - Redirect → /payment-success
   Else:
   - ItemStatus = DRAFT (hoặc xóa)
   - Redirect → /payment-failed

GIVE_AWAY Item Lifecycle:
1. POST /api/items (transactionType = GIVE_AWAY)
   ↓
2. ItemStatus = ACTIVE (ngay lập tức)
   paymentUrl = null
   paymentInitiatedAt = null
   ↓
3. Item sẵn sàng show trên platform
```

---

## 🔗 IdGenerator Pattern

```java
public class IdGenerator {
    public static String generateItemId() {
        // Format: ITM + 14 digits
        // VD: ITM00000000000001
        return "ITM" + generateRandomNumber(14);
    }
}
```

---

## ⚙️ Configuration

### ItemController Configuration

```java
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ItemController {
    // CORS: Accept all origins
    // Max age: 1 hour cache
}
```

### Bean Injection

```java
@RequiredArgsConstructor                    // Constructor injection
private final ItemService itemService;
private final Validator validator;
private final ObjectMapper objectMapper;   // JSON mapping (Jackson)
```

---

## 📝 Logging

```java
@Slf4j
public class ItemServiceImpl {
    log.info("Creating item: {} with {} images",
             request.getTitle(),
             images.length);

    log.debug("Starting internal item creation process");

    log.error("Failed to upload image at index {}", i, e);

    log.warn("Skipping empty file at index {}", i);
}
```

---

## 🚀 Performance Considerations

1. **Lazy Loading**: Category, ItemLocation sử dụng FetchType.LAZY
2. **N+1 Query Problem**: Có thể tối ưu bằng join fetch
3. **Indexing**: Cần index trên userId, categoryId, status
4. **Pagination**: Hiện tại chưa implement, cần thêm Spring Data Pageable
5. **Caching**: Có thể cache category attributes, static data
6. **Batch Operations**: Image upload có thể batch lên Cloudinary

---

**Last Updated**: January 2024
