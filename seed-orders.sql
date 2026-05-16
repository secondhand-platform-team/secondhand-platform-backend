INSERT INTO orders (id, buyer_id, total_price, status, payment_status, receiver_name, receiver_phone, shipping_address, created_at, updated_at) VALUES
('ord1', 'u1', 25000000, 'DELIVERED', 'PAID', 'Nguyen Van A', '0912345678', 'Hanoi', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
('ord2', 'u2', 15000000, 'DELIVERED', 'PAID', 'Tran Thi B', '0987654321', 'HCM', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('ord3', 'u3', 5000000, 'SHIPPING', 'PAID', 'Le Van C', '0901122334', 'Danang', NOW() - INTERVAL '1 days', NOW() - INTERVAL '1 days'),
('ord4', 'u4', 1200000, 'PENDING', 'PENDING', 'Pham Van D', '0933445566', 'Can Tho', NOW(), NOW());

INSERT INTO order_items (id, order_id, item_id, item_name, seller_id, price, quantity) VALUES
('oi1', 'ord1', 'p1', 'iPhone 13', 's1', 25000000, 1),
('oi2', 'ord2', 'p2', 'iPad Air', 's2', 15000000, 1),
('oi3', 'ord3', 'p3', 'AirPods Pro', 's1', 5000000, 1),
('oi4', 'ord4', 'p4', 'Chuột Logitech', 's3', 1200000, 1);
