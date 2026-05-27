# Requirements

## Gioi thieu chung
Trong boi canh tieu dung hien dai, luong hang hoa bi thai loai ngay cang tang cao gay ap luc lon len moi truong. De tai **Secondhand Platform** duoc thuc hien voi muc tieu khuyen khich viec tai su dung (Reuse) va tai che (Recycle) thông qua viec trao doi, mua ban do cu. Viec keo dai vong doi san pham khong chi giup tiet kiem chi phi cho nguoi dung ma con gop phan truc tiep vao viec giam thieu rac thai ra moi truong tu nhien.

Ve mat ky thuat, du an nay duoc xay dung tren kien truc **Microservices**. Day la mot mo hinh hien dai giup giai quyet cac bai toan ve kha nang mo rong, tinh doc lap giua cac dich vu va toi uu hoa quy trinh trien khai (Deployment). Viec ap dung Microservices vao de tai giúp:
- Chia nho he thong thanh cac dich vu chuyen biet (Auth, Post, Order, Chat, AI).
- Tan dung suc manh cua cac cong nghe khac nhau (Java Spring Boot cho nghiep vu chinh, Python FastAPI cho AI).
- Lam quen voi viec van hanh he thong phuc tap, quan ly luu luong qua API Gateway va giao tiep giua cac services.

## Doi tuong su dung va Chuc nang he thong

### 1. USER (Nguoi dung)
La doi tuong chinh su dung he thong de thuc hien cac giao dich mua ban. Mot tai khoan USER co the dong ca hai vai tro: Nguoi mua va Nguoi ban.
- **Chuc nang cho Nguoi mua:**
    - Tim kiem va loc san pham theo danh muc, gia ca, khu vuc.
    - Su dung AI Chatbot de duoc tu van san pham va huong dan su dung.
    - Nhan goi y san pham ca nhan hoa (AI Recommendation).
    - Quan ly gio hang (Them/Xoa/Cap nhat).
    - Dat hang va theo doi trang thai don hang.
    - Chat truc tiep voi nguoi ban de thuong luong.
    - Danh gia va phan hoi ve san pham/nguoi ban.
    - Bao cao (Report) cac bai dang vi pham hoac lua dao.
- **Chuc nang cho Nguoi ban:**
    - Dang tin ban san pham (upload hinh anh qua Cloudinary, mo ta, gia).
    - Quan ly danh sach bai dang (Chinh sua, An, Xoa).
    - Tiep nhan va xac nhan don hang tu nguoi mua.
    - Chat voi khach hang de tu van.
    - Quan ly ho so ca nhan va uy tin ban hang.

### 2. STAFF (Nhan vien he thong)
La nhom nguoi dung ho tro van hanh, dam bao moi truong giao dich an toan.
- **Quan ly noi dung:**
    - Kiem duyet cac bai dang moi de dam bao khong vi pham chinh sach.
    - Khoa hoac go bo cac bai dang vi pham.
- **Ho tro va giai quyet tranh chap:**
    - Tiep nhan va xu ly cac bao cao (Report) tu nguoi dung.
    - Lam trung gian giai quyet tranh chap giua nguoi mua va nguoi ban trong qua trinh giao dich.
    - Khoa tam thoi cac tai khoan co dau hieu gian lan.

### 3. ADMIN (Quan tri vien)
Co quyen han cao nhat, quan ly toan bo logic va du lieu cua he thong.
- **Quan ly nguoi dung:** Quan ly danh sach USER va STAFF (Khoa/Mo khoa tai khoan).
- **Quan ly cau hinh:** Cau hinh danh muc san pham, phi dich vu (neu co), va cac tham so he thong.
- **Thong ke va Bao cao:**
    - Theo doi bieu do tang truong nguoi dung, don hang.
    - Thong ke doanh thu va hieu suat cua he thong.
    - Xem nhat ky hoat dong (Audit logs) de truy vet su co.
- **Quan ly he thong:** Giam sat trang thai cac microservices thông qua gateway hoac dashboard.

## Cong nghe va Ky thuat su dung

### 1. Stack cong nghe chinh (Tech Stack)
He thong duoc phat trien theo mo hinh **Polyglot Microservices** (su dung nhieu ngon ngu lap trinh phu hop voi tung muc tieu):

- **Backend (Main Services):**
    - **Java 21 & Spring Boot 3:** Key framework cho Auth-service, Core-service, Order-service va Chat-service.
    - **Spring Data JPA & Hibernate:** Quan ly tuong tac co so du lieu.
    - **Spring Security & JWT:** Co che xac thuc va phan quyen tap trung.
- **AI Service:**
    - **Python 3.10+ & FastAPI:** Framework hieu suat cao cho cac tac vu AI.
    - **Google Gemini API:** Su dung mô hình ngôn ngữ lớn (LLM) cho Chatbot va phan tich goi y.
- **Co so du lieu & Luu tru:**
    - **PostgreSQL:** Co so du lieu quan he chinh cho toan bo he thong.
    - **Cloudinary:** Luu tru va quan ly hinh anh san pham tren nen tang dam may.
- **Giao tiep & Ket noi:**
    - **RabbitMQ:** Message Broker ho tro giao tiep bat dong bo giua cac services (Event-driven).
    - **Kong API Gateway:** Quan ly luu luong (Traffic management), bao mat va dinh tuyen (Routing) tap trung.
- **DevOps & Trien khai:**
    - **Docker & Docker Compose:** Container hoa toan bo ung dung de dong nhat moi truong phat trien va trien khai.
    - **Flyway:** Quan ly phien ban cau truc co so du lieu (Database Migration).

### 2. Cac ky thuat va Kien truc ap dung
- **Microservices Architecture:** Chia nho he thong thanh cac dich vu doc lap, tang kha nang bao tri va mo rong.
- **API Gateway Pattern:** Su dung Kong lam diem dau tiep nhan duy nhat cho tat ca client, giup an giau cau truc ben trong va tang bao mat.
- **Event-Driven Architecture:** Su dung RabbitMQ de gui thong bao hoac cap nhat trang thai bat dong bo (vi du: cap nhat kho khi co don hang).
- **Interservice Communication:** Su dung **OpenFeign** (Synchronous) va **RabbitMQ** (Asynchronous) de truyen tai du lieu giua cac service Java.
- **Database per Service:** Moi service quan ly co so du lieu rieng (logical) de dam bao tinh doc lap.
- **Stateless Authentication:** Su dung JWT (JSON Web Token) luu trong HTTP-only Cookie de quan ly phien lam viec ma khong can luu trang thai tren server.
- **AI Integration:** Ket hop LLM (Large Language Model) vao luong nghiep vu thuc te de cung cap tinh nang thong minh (Chatbot, Recommendation).

### 3. Cac ky thuat chi tiet (Sub-techniques)
- **Soft Delete:** Su dung cot `deleted_at` de an cac ban ghi thay vi xoa vat ly.
  - *Minh chung:* [backend/postgres/V10__add_soft_delete_to_items.sql](backend/postgres/V10__add_soft_delete_to_items.sql)
- **Audit & History Tracking:** Quan ly lich su xem san pham (`ViewHistory`) va tim kiem (`SearchHistory`).
  - *Minh chung:* Package `com.secondhand.coreservice.model` trong Core-service.
- **Global Exception Handling:** Bo xu ly ngoai le tap trung trong moi microservice.
  - *Minh chung:* [GlobalExceptionHandler.java](backend/auth-service/src/main/java/com/secondhand/authservice/exception/GlobalExceptionHandler.java)
- **Database Migration:** Su dung **Flyway** de tu dong hoa cap nhat cau truc DB.
  - *Minh chung:* Thu muc `db/migration` trong cac service Java.
- **LLM Fallback Mechanism:** Service AI ho tro nhieu model (OpenAI, Gemini, Groq) voi co che fallback.
  - *Minh chung:* Bien `LLM_CLIENTS` trong [chat_service.py](backend/ai-service/app/services/chat_service.py#L21)
- **HTTP-only Cookie Security:** Luu tru JWT trong Cookie voi flag `HttpOnly` de bao mat.
  - *Minh chung:* [AuthCookieUtils.java](backend/auth-service/src/main/java/com/secondhand/authservice/utils/AuthCookieUtils.java#L28)
- **Image Optimization:** Tich hop API cua **Cloudinary** de tu dong resize va toi uu hinh anh.
- **Bean Validation:** Su dung JSR 303/380 để kiem soat tinh hop le cua du lieu (vi du: `@NotBlank`, `@Email`).

### 4. Ky thuat toi uu hieu suat (Performance Optimization)
- **Database Indexing:** Thiet lap Index tren cac cot truy van thuong xuyen.
  - *Minh chung:* `CREATE INDEX idx_items_deleted_at` trong [V10__add_soft_delete_to_items.sql](backend/postgres/V10__add_soft_delete_to_items.sql#L5)
- **Asynchronous Processing:** Su dung **RabbitMQ** de xu ly cac tac vu ton thoi gian bat dong bo nhu gui thong bao hoac cap nhat vi.
  - *Minh chung 1 (Producer):* [NotificationClient.java](backend/order-service/src/main/java/com/secondhand/orderservice/service/NotificationClient.java#L46) su dung `rabbitTemplate.convertAndSend` de gui tin nhan vao queue.
  - *Minh chung 2 (Consumer):* [NotificationEventConsumer.java](backend/core-service/src/main/java/com/secondhand/coreservice/consumer/NotificationEventConsumer.java#L29) su dung `@RabbitListener` de tiep nhan va xu ly tin nhan tu queue.
- **Distributed Caching:** Tich hop **Redis** de luu tru du lieu truy cap cao, giam tai cho Database quan he.
  - *Minh chung:* [CategoryServiceImpl.java](backend/core-service/src/main/java/com/secondhand/coreservice/service/impl/CategoryServiceImpl.java#L92) su dung anotation `@Cacheable` de tu dong luu ket qua truy van danh muc vao Redis Cache.
- **Pagination:** Ap dung **Spring Data Pageable** de han che luong du lieu truyen tai.
  - *Minh chung:* Tham so `Pageable` trong [AdminUserController.java](backend/auth-service/src/main/java/com/secondhand/authservice/controller/AdminUserController.java#L41)
- **Connection Pooling:** Su dung **HikariCP** (mac dinh trong Spring Boot 3) de quan ly va tai su dung cac ket noi co so du lieu, giup toi uu hoa hieu suat va chi phi tai nguyen.
  - *Minh chung:* Thu vien duoc tu dong tich hop qua dependency [spring-boot-starter-data-jpa](backend/core-service/pom.xml#L44). Spring Boot tu dong khoi tao `HikariDataSource` de duy tri pool ket noi toi PostgreSQL ma khong can cau hinh thu cong phức tạp.
- **Static Content Offloading:** Su dung **Cloudinary CDN** de tang toc do tai hinh anh.
- **Asynchronous FastAPI:** Service AI su dung `async/await` de xu ly nhieu ket noi dong thoi.
  - *Minh chung:* [chat.py](backend/ai-service/app/routes/chat.py) va [recommendation.py](backend/ai-service/app/routes/recommendation.py)

### 5. Kien truc va Design Patterns
He thong ap dung cac kien truc va mau thiet ke tieu chuan de dam bao tinh minh bach va de bao tri:

- **CQRS (Command Query Responsibility Segregation):** Tach biet giua luong du lieu thay doi trang thai (Command) va luong du lieu truy van (Query).
  - *Minh chung:* Trong Order-service, cac logic duoc tach thanh `OrderCommandServiceImpl` va `OrderQueryServiceImpl`.
- **Sync & Async Communication:**
    - **Synchronous (OpenFeign):** Su dung khi can ket qua tuc thi giua cac service. (Vi du: Core-service goi Auth-service de kiem tra thng tin user).
    - **Asynchronous (RabbitMQ):** Su dung cho cac tac vu can su on dinh va giam tai he thong. (Vi du: Khi co don hang moi, `Order-service` ban event sang `Notification-queue` de gui thong bao ma khong can cho doi).
- **Dependency Injection (DI):** Su dung Spring IOC container de quan ly dependencies, giup code loose coupling.
- **Data Transfer Object (DTO):** Su dung DTO de trao doi du lieu giua cac lop va giua các service, tranh lo lot thong tin nhay cam cua Entity.
- **Singleton Pattern:** Tat ca các Spring Beans (Service, Repository) deu duoc quan ly duoi dang Singleton de toi uu bo nho.
  - *Minh chung:* Cac lop co gan te `@Service` hoac `@Repository` (vi du: [OrderServiceImpl.java](backend/order-service/src/main/java/com/secondhand/orderservice/service/impl/OrderServiceImpl.java)) deu duoc Spring Boot khoi tao duy nhat mot doi tuong trong suot vong doi ung dung.
- **Factory Pattern:** Su dung de khoi tao doi tuong dua tren cau hinh hoac dieu kien thuc te.
  - *Minh chung 1 (Java):* [RestTemplateConfig.java](backend/order-service/src/main/java/com/secondhand/orderservice/config/RestTemplateConfig.java#L21) su dung `SimpleClientHttpRequestFactory` de thiet lap timeout va tao doi tuong `RestTemplate`.
  - *Minh chung 2 (Python):* Co che khoi tao danh sach `LLM_CLIENTS` trong [chat_service.py](backend/ai-service/app/services/chat_service.py#L21-L36) đóng vai trò như một Factory để cung cấp client AI (OpenAI/Gemini) tùy theo API Key có sẵn.

## Muc tieu he thong

He thong duoc xay dung voi cac muc tieu chien luoc sau:

### 1. Toi uu hoa quy trinh mua ban do cu
- Cung cap mot nen tang tap trung giup nguoi dung de dang dang tin ban va tim kiem san pham do cu moi luc, moi noi.
- Don gian hoa cac bước tu tiep can san pham, thuong luong (Chat) den thanh toan va xac nhan don hang.

### 2. Xay dung moi truong giao dich tin cay va an toan
- Thiet lap co che kiem duyet (Staff) de loai bo cac tin dang vi pham hoac kem chat luong.
- Cung cap tinh nang bao cao va danh gia (Rating/Review) de xay dung uy tin cho nguoi ban va bao ve quyen loi nguoi mua.
- Giam thieu rui ro gian lan thong qua quan ly danh tinh nguoi dung va lich su giao dich minh bach.

### 3. Ca nhan hoa trai nghiem nguoi dung bang AI
- Su dung AI Recommendation de goi y nhung san pham phu hop nhat voi so thich va hanh vi cua tung nguoi dung, tang ty le chot don.
- Trien khai AI Chatbot de ho tro giai dap thac mac 24/7, huong dan nguoi dung su dung nen tang mot cach hieu qua.

### 4. Dam bao tinh san sang va kha nang mo rong (Scalability)
- He thong duoc kien truc theo dang Microservices (Spring Boot, FastAPI) cho phep mo rong tung dich vu rieng le theo nhu cau tai.
- Su dung API Gateway (Kong) de quan ly luu luong va bao mat, dam bao he thong hoat dong on dinh ngay cả khi luong nguoi dung tang cao.

### 5. Cung cap cac công cụ quan tri va ra quyet dinh
- Cung cap he thong thong ke, bao cao chi tiet cho Admin ve doanh thu, so luong giao dich va hanh vi nguoi dung.
- Giup ban quan tri co cai nhin tong quan de dieu chinh chien luoc kinh doanh va cai thien chat luong dich vu lien tuc.
