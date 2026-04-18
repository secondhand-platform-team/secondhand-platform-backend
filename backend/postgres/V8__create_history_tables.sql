-- ====================================
-- SEARCH HISTORY TABLE
-- ====================================
-- Lịch sử tìm kiếm của người dùng
-- Lưu các từ khóa tìm kiếm để gợi ý, analytics, UX improvement

CREATE TABLE IF NOT EXISTS search_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- ID người dùng thực hiện tìm kiếm
    user_id VARCHAR(255) NOT NULL,
    
    -- Từ khóa tìm kiếm
    search_query TEXT NOT NULL,
    
    -- Loại danh mục (optional)
    category_id VARCHAR(255),
    
    -- Số kết quả trả về
    result_count INT,
    
    -- Thời gian tạo record
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes để tối ưu performance
    INDEX idx_user_id_created_at (user_id DESC, created_at DESC),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Lịch sử tìm kiếm - Search History';


-- ====================================
-- VIEW HISTORY TABLE
-- ====================================
-- Lịch sử xem tin của người dùng
-- Lưu tất cả các lần user xem items để gợi ý, analytics, track behavior

CREATE TABLE IF NOT EXISTS view_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    -- ID người dùng xem tin
    user_id VARCHAR(255) NOT NULL,
    
    -- ID tin được xem
    item_id VARCHAR(255) NOT NULL,
    
    -- Thời gian xem
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Session ID (optional, để track xem liên tiếp)
    session_id VARCHAR(255),
    
    -- Thời gian tạo record (đồng bộ với viewed_at)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to items table
    CONSTRAINT fk_view_history_item 
        FOREIGN KEY (item_id) REFERENCES items(item_id) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    
    -- Indexes để tối ưu performance
    INDEX idx_user_id_item_id_created_at (user_id DESC, item_id, created_at DESC),
    INDEX idx_user_id_created_at (user_id DESC, created_at DESC),
    INDEX idx_item_id_created_at (item_id, created_at DESC),
    INDEX idx_user_id (user_id),
    INDEX idx_item_id (item_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Lịch sử xem tin - View History';


-- ====================================
-- IMPORTANT NOTES
-- ====================================
/*
Performance Optimization:
- Composite indexes được tạo để tối ưu các queries thường xuyên
- user_id DESC được sử dụng vì query thường lấy dữ liệu theo user + created_at DESC
- created_at DESC để query lấy dữ liệu mới nhất nhanh hơn

Data Cleanup:
- Records cũ hơn 90 ngày sẽ được xóa tự động bằng HistoryCleanupScheduler
- Chạy hàng đêm lúc 2 AM

Estimated Storage:
- Nếu 10,000 users, mỗi user 100 view/tháng = 12 triệu records/năm -> ~5-10 GB/năm
- Cascade delete sẽ xóa view_history khi item bị xóa

Foreign Keys:
- View History có FK đến Items table (ON DELETE CASCADE)
- Search History không có FK (chỉ lưu category_id, không bắt buộc)
*/

-- ====================================
-- STEP 1: Run this SQL để tạo tables
-- ====================================
-- Sau đó, chạy: mvn spring-boot:run
-- Spring Boot sẽ tự động thêm @Column, @Entity mapping

-- ====================================
-- STEP 2: Verify tables
-- ====================================
-- SELECT * FROM search_history LIMIT 5;
-- SELECT * FROM view_history LIMIT 5;
