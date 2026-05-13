-- ====================================
-- REPORTS TABLE
-- ====================================
-- Bảng lưu trữ báo cáo vi phạm của người dùng
-- Mỗi báo cáo ghi lại hành vi vi phạm của một bài viết

CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(255) PRIMARY KEY,
    
    -- ID người báo cáo (từ auth-service)
    reporter_id VARCHAR(255) NOT NULL,
    
    -- Mã báo cáo (FRAUD, COUNTERFEIT, FORBIDDEN, WRONG_CAT, SOLD_OUT)
    code VARCHAR(50) NOT NULL,
    
    -- Lý do báo cáo
    reason VARCHAR(255) NOT NULL,
    
    -- Mô tả chi tiết
    description TEXT,
    
    -- Trạng thái báo cáo (PENDING, REVIEWING, RESOLVED, REJECTED)
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- ID bài viết bị báo cáo
    item_id VARCHAR(255) NOT NULL,
    
    -- Thời gian tạo báo cáo
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Thời gian xử lý xong
    resolved_at DATETIME,
    
    -- Nhân viên/admin xử lý báo cáo
    assigned_staff_id VARCHAR(255),
    
    -- Ghi chú của admin khi xử lý báo cáo
    admin_note TEXT,
    
    -- Indexes để tối ưu performance
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_item_id (item_id),
    INDEX idx_status (status),
    INDEX idx_code (code),
    INDEX idx_created_at (created_at DESC),
    
    -- Foreign key constraint
    CONSTRAINT fk_reports_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Báo cáo vi phạm - Reports';


-- ====================================
-- REPORT IMAGES TABLE
-- ====================================
-- Bảng lưu trữ ảnh đi kèm báo cáo
-- Mỗi báo cáo có thể có tối đa 2 ảnh chứng minh

CREATE TABLE IF NOT EXISTS report_images (
    id VARCHAR(255) PRIMARY KEY,
    
    -- ID báo cáo
    report_id VARCHAR(255) NOT NULL,
    
    -- URL ảnh (lưu link Cloudinary hoặc S3)
    image_url TEXT NOT NULL,
    
    -- Index
    INDEX idx_report_id (report_id),
    
    -- Foreign key constraint
    CONSTRAINT fk_report_images_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Ảnh báo cáo - Report Images';
