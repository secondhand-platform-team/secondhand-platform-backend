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
INSERT INTO categories (category_id, name, description, parent_id, posting_fee, created_at, updated_at) VALUES
('cg-0001', 'Điện tử', 'Các sản phẩm điện tử', NULL, 0, NOW(), NOW()),
('cg-0002', 'Phương tiện', 'Các loại phương tiện giao thông', NULL, 0, NOW(), NOW()),
('cg-0003', 'Sản phẩm khác', 'Các sản phẩm khác', NULL, 0, NOW(), NOW());

-- ========================================
-- 2. CHILD CATEGORIES (Điện tử)
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, posting_fee, created_at, updated_at) VALUES
('cg-0004', 'Điện thoại', 'Điện thoại thông minh', 'cg-0001', 10000, NOW(), NOW()),
('cg-0005', 'Laptop', 'Máy tính xách tay', 'cg-0001', 15000, NOW(), NOW()),
('cg-0006', 'Tivi', 'Tivi, màn hình', 'cg-0001', 12000, NOW(), NOW()),
('cg-0007', 'Âm thanh', 'Loa, tai nghe, dàn âm thanh', 'cg-0001', 8000, NOW(), NOW()),
('cg-0008', 'Máy tính bảng', 'Tablet', 'cg-0001', 10000, NOW(), NOW()),
('cg-0009', 'Máy tính để bàn', 'PC, máy tính để bàn', 'cg-0001', 12000, NOW(), NOW()),
('cg-0010', 'Tủ lạnh', 'Tủ lạnh, tủ mát', 'cg-0001', 20000, NOW(), NOW()),
('cg-0011', 'Máy lạnh', 'Máy lạnh, điều hòa', 'cg-0001', 20000, NOW(), NOW()),
('cg-0012', 'Điều hòa', 'Điều hòa không khí', 'cg-0001', 20000, NOW(), NOW()),
('cg-0013', 'Máy giặt', 'Máy giặt, máy sấy', 'cg-0001', 20000, NOW(), NOW());

-- ========================================
-- 3. CHILD CATEGORIES (Phương tiện)
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, posting_fee, created_at, updated_at) VALUES
('cg-0014', 'Xe máy', 'Xe máy, xe tay ga', 'cg-0002', 25000, NOW(), NOW()),
('cg-0015', 'Ô tô', 'Ô tô 4 bánh', 'cg-0002', 50000, NOW(), NOW()),
('cg-0016', 'Xe tải', 'Xe tải các loại', 'cg-0002', 30000, NOW(), NOW()),
('cg-0017', 'Xe ben', 'Xe ben, xe chuyên dụng', 'cg-0002', 30000, NOW(), NOW()),
('cg-0018', 'Xe đạp', 'Xe đạp, xe điện', 'cg-0002', 5000, NOW(), NOW()),
('cg-0019', 'Phương tiện khác', 'Các phương tiện khác', 'cg-0002', 10000, NOW(), NOW()),
('cg-0020', 'Phụ tùng xe', 'Phụ tùng xe máy, ô tô', 'cg-0002', 3000, NOW(), NOW());

-- ========================================
-- 4. CHILD CATEGORIES (Sản phẩm khác)
-- ========================================
INSERT INTO categories (category_id, name, description, parent_id, posting_fee, created_at, updated_at) VALUES
('cg-0021', 'Thú cưng', 'Chó, mèo và các thú cưng khác', 'cg-0003', 8000, NOW(), NOW()),
('cg-0022', 'Đồ gia dụng', 'Đồ gia dụng, vệ sinh', 'cg-0003', 3000, NOW(), NOW()),
('cg-0023', 'Nội thất', 'Bàn, ghế, tủ, giường', 'cg-0003', 5000, NOW(), NOW()),
('cg-0024', 'Cây cảnh', 'Cây, hoa, cây cảnh', 'cg-0003', 2000, NOW(), NOW());

-- ========================================
-- 5. CATEGORY ATTRIBUTES FOR SMARTPHONE
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0001', 'cg-0004', 'brand', 'Hãng', 'Hãng sản xuất', 'STRING', NULL, true, true, true, NULL, NULL, '["Apple","Samsung","Xiaomi","OPPO","Vivo"]', 1, NOW(), NOW()),
('at-0002', 'cg-0004', 'storage', 'Dung lượng', 'Bộ nhớ trong (GB)', 'NUMBER', 'GB', true, true, false, 32, 1024, NULL, 2, NOW(), NOW()),
('at-0003', 'cg-0004', 'ram', 'RAM', 'Bộ nhớ RAM (GB)', 'NUMBER', 'GB', true, true, false, 2, 16, NULL, 3, NOW(), NOW()),
('at-0004', 'cg-0004', 'condition', 'Tình trạng', 'Tình trạng sản phẩm', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường"]', 4, NOW(), NOW());

-- ========================================
-- 6. CATEGORY ATTRIBUTES FOR LAPTOP
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0005', 'cg-0005', 'ram', 'RAM', 'Bộ nhớ RAM (GB)', 'NUMBER', 'GB', true, true, true, 2, 64, NULL, 1, NOW(), NOW()),
('at-0006', 'cg-0005', 'cpu', 'CPU', 'Loại CPU', 'STRING', NULL, true, true, true, NULL, NULL, '["Intel Core i3","Intel Core i5","Intel Core i7","AMD Ryzen 5","AMD Ryzen 7"]', 2, NOW(), NOW()),
('at-0007', 'cg-0005', 'storage', 'Ổ cứng', 'Dung lượng ổ cứng (GB)', 'NUMBER', 'GB', true, true, false, 128, 2048, NULL, 3, NOW(), NOW()),
('at-0008', 'cg-0005', 'condition', 'Tình trạng', 'Tình trạng sản phẩm', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường"]', 4, NOW(), NOW());

-- ========================================
-- 7. CATEGORY ATTRIBUTES FOR XE MÁY
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0009', 'cg-0014', 'displacement', 'Dung tích xi lanh', 'Dung tích (cc)', 'NUMBER', 'cc', true, true, true, 50, 600, NULL, 1, NOW(), NOW()),
('at-0010', 'cg-0014', 'brand', 'Hãng xe', 'Hãng sản xuất', 'STRING', NULL, true, true, true, NULL, NULL, '["Honda","Yamaha","Suzuki","Kawasaki","Ducati"]', 2, NOW(), NOW()),
('at-0011', 'cg-0014', 'year', 'Năm sản xuất', 'Năm sản xuất', 'INTEGER', NULL, true, true, false, 2000, 2024, NULL, 3, NOW(), NOW()),
('at-0012', 'cg-0014', 'mileage', 'Số km', 'Quãng đường đã đi (km)', 'NUMBER', 'km', false, true, false, 0, 100000, NULL, 4, NOW(), NOW()),
('at-0013', 'cg-0014', 'condition', 'Tình trạng', 'Tình trạng xe', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường","Cần sửa chữa"]', 5, NOW(), NOW()),
('at-0014', 'cg-0014', 'fuel_type', 'Loại nhiên liệu', 'Loại nhiên liệu', 'ENUM', NULL, true, true, true, NULL, NULL, '["Xăng","Điện","Hybrid"]', 6, NOW(), NOW());

-- ========================================
-- 8. CATEGORY ATTRIBUTES FOR Ô TÔ
-- ========================================
INSERT INTO category_attributes (attribute_id, category_id, code, name, description, data_type, unit, required, filterable, searchable, min_value_number, max_value_number, options_json, sort_order, created_at, updated_at) VALUES
('at-0015', 'cg-0015', 'brand', 'Hãng xe', 'Hãng sản xuất', 'STRING', NULL, true, true, true, NULL, NULL, '["Toyota","Honda","Ford","BMW","Mercedes"]', 1, NOW(), NOW()),
('at-0016', 'cg-0015', 'year', 'Năm sản xuất', 'Năm sản xuất', 'INTEGER', NULL, true, true, false, 2000, 2024, NULL, 2, NOW(), NOW()),
('at-0017', 'cg-0015', 'mileage', 'Số km', 'Quãng đường đã đi (km)', 'NUMBER', 'km', false, true, false, 0, 500000, NULL, 3, NOW(), NOW()),
('at-0018', 'cg-0015', 'fuel_type', 'Loại nhiên liệu', 'Loại nhiên liệu', 'ENUM', NULL, true, true, true, NULL, NULL, '["Xăng","Diesel","Điện","Hybrid"]', 4, NOW(), NOW()),
('at-0019', 'cg-0015', 'condition', 'Tình trạng', 'Tình trạng xe', 'ENUM', NULL, true, true, false, NULL, NULL, '["Như mới","Rất tốt","Tốt","Bình thường","Cần sửa chữa"]', 5, NOW(), NOW());