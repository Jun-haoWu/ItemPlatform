#!/bin/bash

echo "开始配置MySQL数据库..."

# 1. 首先尝试重新安装MySQL
echo "重新安装MySQL..."
sudo yum remove -y mysql-community-server
sudo yum install -y mysql-community-server --nogpgcheck

# 2. 启动服务
echo "启动MySQL服务..."
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 3. 获取临时密码
echo "获取临时密码..."
temp_password=$(sudo grep 'temporary password' /var/log/mysqld.log 2>/dev/null | awk '{print $NF}')
if [ -z "$temp_password" ]; then
    echo "无法获取临时密码，尝试空密码登录..."
    temp_password=""
fi

echo "临时密码: $temp_password"

# 4. 修改root密码
echo "修改root密码..."
if [ -n "$temp_password" ]; then
    mysql -u root -p"$temp_password" --connect-expired-password << EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY 'ItemPlatform2024!';
EOF
else
    mysql -u root << EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY 'ItemPlatform2024!';
EOF
fi

# 5. 配置远程访问和创建数据库
echo "配置远程访问和创建数据库..."
mysql -u root -p'ItemPlatform2024!' << EOF
-- 创建数据库
CREATE DATABASE IF NOT EXISTS item_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'item_user'@'%' IDENTIFIED BY 'ItemPlatform2024!';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'%';
GRANT ALL PRIVILEGES ON item_platform.* TO 'root'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 使用数据库
USE item_platform;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建商品表
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    images JSON,
    category VARCHAR(50),
    location VARCHAR(100),
    like_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 插入测试数据
INSERT INTO users (username, password, email, phone) VALUES 
('testuser', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'test@example.com', '13800138000'),
('admin', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@example.com', '13900139000');

INSERT INTO products (name, description, price, original_price, images, category, location) VALUES 
('iPhone 15 Pro', '全新iPhone 15 Pro，钛金属材质', 7999.00, 8999.00, '["https://example.com/iphone1.jpg", "https://example.com/iphone2.jpg"]', '电子产品', '北京'),
('MacBook Air M2', 'M2芯片MacBook Air，轻薄便携', 8999.00, 9999.00, '["https://example.com/macbook1.jpg", "https://example.com/macbook2.jpg"]', '电子产品', '上海'),
('AirPods Pro', '主动降噪无线耳机', 1999.00, 2499.00, '["https://example.com/airpods1.jpg"]', '电子产品', '深圳');

INSERT INTO favorites (user_id, product_id) VALUES 
(1, 1),
(1, 2),
(2, 3);

SHOW DATABASES;
SHOW TABLES;
SELECT COUNT(*) as total_users FROM users;
SELECT COUNT(*) as total_products FROM products;
SELECT COUNT(*) as total_favorites FROM favorites;
EOF

# 6. 配置MySQL远程访问
echo "配置MySQL远程访问..."
sudo sed -i 's/^bind-address.*/bind-address = 0.0.0.0/' /etc/my.cnf 2>/dev/null || echo "bind-address = 0.0.0.0" >> /etc/my.cnf

# 7. 重启MySQL服务
echo "重启MySQL服务..."
sudo systemctl restart mysqld

# 8. 检查状态
echo "检查服务状态..."
sudo systemctl status mysqld

# 9. 防火墙配置（如果启用）
echo "配置防火墙..."
sudo firewall-cmd --permanent --add-port=3306/tcp 2>/dev/null || echo "防火墙未启用"
sudo firewall-cmd --reload 2>/dev/null || echo "防火墙未启用"

# 10. 测试连接
echo "测试数据库连接..."
mysql -u root -p'ItemPlatform2024!' -e "SELECT 1;" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "数据库连接测试成功！"
else
    echo "数据库连接测试失败，请检查配置"
fi

echo "数据库配置完成！"
echo "数据库信息："
echo "- 数据库: item_platform"
echo "- root密码: ItemPlatform2024!"
echo "- 应用用户: item_user"
echo "- 应用用户密码: ItemPlatform2024!"
echo "- 远程访问: 已开启"
echo ""
echo "测试命令："
echo "mysql -u root -pItemPlatform2024!"
echo "mysql -u item_user -pItemPlatform2024! -h 你的服务器IP item_platform"