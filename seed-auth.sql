INSERT INTO users (user_id, email, password, role, status, created_at, updated_at, free_sell_used) VALUES
('admin1', 'admin@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8q2OuVGkqBKkj3Yv77yYkByYq.4f.I.G5v6', 'ADMIN', true, CURRENT_DATE, CURRENT_DATE, 2);

INSERT INTO user_profiles (user_id, full_name, avatar_url) VALUES
('admin1', 'Hệ Thống Admin', 'https://ui-avatars.com/api/?name=Admin&background=0D8ABC&color=fff');
