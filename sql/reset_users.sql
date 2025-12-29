USE item_platform;

-- 先清空users表
TRUNCATE TABLE users;

-- 重新插入所有用户
INSERT INTO users (username, password, email, phone) VALUES
('admin', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'admin@example.com', '13800000001'),
('2', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user2@example.com', '13800000002'),
('testuser', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'test@example.com', '13800000003'),
('user3', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user3@example.com', '13800000004'),
('user4', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user4@example.com', '13800000005'),
('user5', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user5@example.com', '13800000006'),
('user6', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user6@example.com', '13800000007'),
('user7', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user7@example.com', '13800000008'),
('user8', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user8@example.com', '13800000009'),
('user9', '$2a$10$X7w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0O8w0', 'user9@example.com', '13800000010');
