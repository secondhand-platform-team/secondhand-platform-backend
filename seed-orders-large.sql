-- Seeding secondhand_order_db
TRUNCATE order_items, payments, shipments, transactions, orders CASCADE;

-- Insert orders over the last 6 months
-- TODAY
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_today_1', 'u1', 5000000, 'DELIVERED', 'PAID', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_t1', 'ord_today_1', 'item1', 'Tai nghe Sony', 's1', 5000000, 1);

-- YESTERDAY
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_yest_1', 'u2', 12000000, 'DELIVERED', 'PAID', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day');
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_y1', 'ord_yest_1', 'item2', 'iPad Air 4', 's2', 12000000, 1);

-- 3 DAYS AGO
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_3d_1', 'u1', 25000000, 'DELIVERED', 'PAID', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days');
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_3d1', 'ord_3d_1', 'item3', 'iPhone 13 Pro', 's1', 25000000, 1);

-- 10 DAYS AGO (Should show in 'month' but not in 'week')
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_10d_1', 'u3', 8000000, 'DELIVERED', 'PAID', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days');
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_10d1', 'ord_10d_1', 'item4', 'Bàn phím cơ', 's3', 8000000, 1);

-- 40 DAYS AGO (Should show in 'year' but not in 'month')
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_40d_1', 'u4', 45000000, 'DELIVERED', 'PAID', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '40 days');
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_40d1', 'ord_40d_1', 'item5', 'MacBook Pro M1', 's1', 45000000, 1);

-- CANCELLED (Should not count in revenue)
INSERT INTO orders (id, buyer_id, total_price, status, payment_status, created_at, updated_at) VALUES
('ord_cancel_1', 'u2', 10000000, 'CANCELLED', 'REFUNDED', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days');
INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi_c1', 'ord_cancel_1', 'item6', 'Apple Watch', 's2', 10000000, 1);
