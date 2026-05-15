-- Seeding secondhand_auth_db
-- Clear existing non-admin data if needed
DELETE FROM user_profiles WHERE user_id != 'admin1';
DELETE FROM users WHERE user_id != 'admin1';

-- Insert users over the last 6 months
INSERT INTO users (user_id, email, password, role, status, created_at, updated_at, free_sell_used) VALUES
('u1', 'user1@gmail.com', '123456', 'USER', true, CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE - INTERVAL '2 days', 2),
('u2', 'user2@gmail.com', '123456', 'USER', true, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE - INTERVAL '5 days', 2),
('u3', 'user3@gmail.com', '123456', 'USER', true, CURRENT_DATE - INTERVAL '15 days', CURRENT_DATE - INTERVAL '15 days', 2),
('u4', 'user4@gmail.com', '123456', 'USER', true, CURRENT_DATE - INTERVAL '45 days', CURRENT_DATE - INTERVAL '45 days', 2),
('u5', 'user5@gmail.com', '123456', 'USER', true, CURRENT_DATE - INTERVAL '90 days', CURRENT_DATE - INTERVAL '90 days', 2),
('u6', 'user6@gmail.com', '123456', 'USER', false, CURRENT_DATE - INTERVAL '120 days', CURRENT_DATE - INTERVAL '120 days', 2);

INSERT INTO user_profiles (user_id, full_name, avatar_url) VALUES
('u1', 'Nguyễn Văn A', 'https://ui-avatars.com/api/?name=Van+A'),
('u2', 'Trần Thị B', 'https://ui-avatars.com/api/?name=Thi+B'),
('u3', 'Lê Văn C', 'https://ui-avatars.com/api/?name=Van+C'),
('u4', 'Phạm Thị D', 'https://ui-avatars.com/api/?name=Thi+D'),
('u5', 'Hoàng Văn E', 'https://ui-avatars.com/api/?name=Van+E'),
('u6', 'Đặng Thị F', 'https://ui-avatars.com/api/?name=Thi+F');
