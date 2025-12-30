第一步：进入MySQL容器
docker exec -it itemplatform-mysql mysql -u root -pItemPlatform2024! item_platform
第二步：在MySQL提示符下执行这些SQL语句 （逐行复制粘贴）
-- 先禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 删除所有用户（这会级联删除相关产品）
DELETE FROM users;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 插入所有10个用户
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

-- 验证用户数量
SELECT COUNT(*) as total_users FROM users;

-- 查看所有用户
SELECT id, username, email FROM users ORDER BY id;

-- 退出MySQL
exit