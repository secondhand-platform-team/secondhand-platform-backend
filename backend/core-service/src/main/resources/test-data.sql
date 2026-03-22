-- ========================================
-- TEST DATA FOR SECONDHAND PLATFORM (PostgreSQL)
-- ========================================

-- Clean up old data first
SET CONSTRAINTS ALL DEFERRED;

TRUNCATE TABLE item_attribute_values CASCADE;
TRUNCATE TABLE items CASCADE;
TRUNCATE TABLE category_attributes CASCADE;
TRUNCATE TABLE categories CASCADE;

-- ========================================
-- 1. PARENT CATEGORIES
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, created_at, updated_at) VALUES
('cg-0001', 'Điện tử', 'Các sản phẩm điện tử', NULL, NOW(), NOW()),
('cg-0002', 'Phương tiện', 'Các loại phương tiện giao thông', NULL, NOW(), NOW()),
('cg-0003', 'Thời trang', 'Quần áo, giày dép, phụ kiện', NULL, NOW(), NOW());

-- ========================================
-- 2. CHILD CATEGORIES (Điện tử)
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, created_at, updated_at) VALUES
('cg-0004', 'Laptop', 'Máy tính xách tay', 'cg-0001', NOW(), NOW()),
('cg-0005', 'Smartphone', 'Điện thoại thông minh', 'cg-0001', NOW(), NOW()),
('cg-0006', 'Tablet', 'Máy tính bảng', 'cg-0001', NOW(), NOW());

-- ========================================
-- 3. CHILD CATEGORIES (Phương tiện)
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, created_at, updated_at) VALUES
('cg-0007', 'Xe máy', 'Xe máy, xe tay ga', 'cg-0002', NOW(), NOW()),
('cg-0008', 'Ô tô', 'Ô tô 4 bánh', 'cg-0002', NOW(), NOW()),
('cg-0009', 'Xe đạp', 'Xe đạp, xe điện', 'cg-0002', NOW(), NOW());

-- ========================================
-- 4. CATEGORY ATTRIBUTES FOR LAPTOP
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0001', 'cg-0004', 'ram', 'RAM', 'Bộ nhớ RAM (GB)', 'NUMBER', 'GB', true, true, true, 2, 64, NULL, 1, NOW(), NOW()),
('at-0002', 'cg-0004', 'cpu', 'CPU', 'Loại CPU', 'STRING', NULL, true, true, true, NULL, NULL, '["Intel Core i3","Intel Core i5","Intel Core i7","AMD Ryzen 5","AMD Ryzen 7"]', 2, NOW(), NOW()),
('at-0003', 'cg-0004', 'storage', 'Ổ cứng', 'Dung lượng ổ cứng (GB)', 'NUMBER', 'GB', true, true, false, 128, 2048, NULL, 3, NOW(), NOW()),
('at-0004', 'cg-0004', 'screen_size', 'Kích thước màn hình', 'Đường chéo màn hình (inch)', 'NUMBER', 'inch', false, true, false, 13, 17, NULL, 4, NOW(), NOW()),
('at-0005', 'cg-0004', 'condition', 'Tình trạng', 'Tình trạng sản phẩm', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường"]', 5, NOW(), NOW()),
('at-0006', 'cg-0004', 'year', 'Năm sản xuất', 'Năm sản xuất', 'INTEGER', NULL, false, true, false, 2015, 2024, NULL, 6, NOW(), NOW());

-- ========================================
-- 5. CATEGORY ATTRIBUTES FOR SMARTPHONE
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0007', 'cg-0005', 'brand', 'Hãng', 'Hãng sản xuất', 'STRING', NULL, true, true, true, NULL, NULL, '["Apple","Samsung","Xiaomi","OPPO","Vivo"]', 1, NOW(), NOW()),
('at-0008', 'cg-0005', 'storage', 'Dung lượng', 'Bộ nhớ trong (GB)', 'NUMBER', 'GB', true, true, false, 32, 1024, NULL, 2, NOW(), NOW()),
('at-0009', 'cg-0005', 'ram', 'RAM', 'Bộ nhớ RAM (GB)', 'NUMBER', 'GB', true, true, false, 2, 16, NULL, 3, NOW(), NOW()),
('at-0010', 'cg-0005', 'screen_size', 'Kích thước màn hình', 'Đường chéo màn hình (inch)', 'NUMBER', 'inch', false, true, false, 4, 7, NULL, 4, NOW(), NOW()),
('at-0011', 'cg-0005', 'battery', 'Pin', 'Dung lượng pin (mAh)', 'NUMBER', 'mAh', false, false, false, 1000, 7000, NULL, 5, NOW(), NOW()),
('at-0012', 'cg-0005', 'condition', 'Tình trạng', 'Tình trạng sản phẩm', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường"]', 6, NOW(), NOW());

-- ========================================
-- 6. CATEGORY ATTRIBUTES FOR XE MÁY
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0013', 'cg-0007', 'displacement', 'Dung tích xi lanh', 'Dung tích (cc)', 'NUMBER', 'cc', true, true, true, 50, 600, NULL, 1, NOW(), NOW()),
('at-0014', 'cg-0007', 'brand', 'Hãng xe', 'Hãng sản xuất', 'STRING', NULL, true, true, true, NULL, NULL, '["Honda","Yamaha","Suzuki","Kawasaki","Ducati"]', 2, NOW(), NOW()),
('at-0015', 'cg-0007', 'year', 'Năm sản xuất', 'Năm sản xuất', 'INTEGER', NULL, true, true, false, 2000, 2024, NULL, 3, NOW(), NOW()),
('at-0016', 'cg-0007', 'mileage', 'Số km', 'Quãng đường đã đi (km)', 'NUMBER', 'km', false, true, false, 0, 100000, NULL, 4, NOW(), NOW()), -- min_value_number sửa từ false thành 0
('at-0017', 'cg-0007', 'condition', 'Tình trạng', 'Tình trạng xe', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường","Cần sửa chữa"]', 5, NOW(), NOW()),
('at-0018', 'cg-0007', 'fuel_type', 'Loại nhiên liệu', 'Loại nhiên liệu', 'ENUM', NULL, true, true, true, NULL, NULL, '["Xăng","Điện","Hybrid"]', 6, NOW(), NOW());

-- [Các lệnh INSERT cho bảng Items và ItemAttributeValues giữ nguyên vì không bị lỗi kiểu dữ liệu]