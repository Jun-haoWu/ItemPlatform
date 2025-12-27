#!/bin/bash

# CentOS 7 MySQL 8.0 安装脚本

echo "开始安装MySQL 8.0..."

# 1. 卸载已存在的MariaDB（如果有）
echo "检查并卸载MariaDB..."
yum remove -y mariadb* mysql*

# 2. 下载并安装MySQL官方YUM仓库
echo "添加MySQL官方仓库..."
cd /tmp
wget https://dev.mysql.com/get/mysql80-community-release-el7-5.noarch.rpm
rpm -ivh mysql80-community-release-el7-5.noarch.rpm

# 3. 安装MySQL服务器
echo "安装MySQL服务器..."
yum install -y mysql-community-server

# 4. 启动MySQL服务
echo "启动MySQL服务..."
systemctl start mysqld
systemctl enable mysqld

# 5. 获取临时密码
echo "获取临时密码..."
TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
echo "临时密码: $TEMP_PASSWORD"

# 6. 等待MySQL完全启动
echo "等待MySQL服务启动..."
sleep 10

# 7. 修改root密码（自动设置新密码）
echo "修改root密码..."
NEW_PASSWORD="ItemPlatform2024!"
mysql -u root -p$TEMP_PASSWORD --connect-expired-password -e "
ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASSWORD';
UNINSTALL PLUGIN validate_password;
SET GLOBAL validate_password.policy=LOW;
ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASSWORD';
"

# 8. 配置远程访问
echo "配置远程访问..."
mysql -u root -p$NEW_PASSWORD -e "
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '$NEW_PASSWORD';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
"

# 9. 修改配置文件，允许远程连接
echo "修改MySQL配置..."
cat >> /etc/my.cnf << EOF

[mysqld]
bind-address = 0.0.0.0
EOF

# 10. 重启MySQL服务
echo "重启MySQL服务..."
systemctl restart mysqld

# 11. 创建应用数据库
echo "创建应用数据库..."
mysql -u root -p$NEW_PASSWORD -e "
CREATE DATABASE IF NOT EXISTS item_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE item_platform;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
);

-- 创建商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    images JSON,
    seller_id BIGINT,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    INDEX idx_seller (seller_id),
    INDEX idx_created (created_at)
);

-- 创建收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_product (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_product (product_id)
);

-- 插入测试数据
INSERT INTO users (username, email, password_hash) VALUES 
('testuser', 'test@example.com', '\$2b\$10\$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi')
ON DUPLICATE KEY UPDATE email=email;

INSERT INTO products (title, description, price, images, seller_id) VALUES 
('iPhone 15 Pro', '全新iPhone 15 Pro，256GB，深空黑色', 8999.00, '[\"https://example.com/iphone1.jpg\", \"https://example.com/iphone2.jpg\"]', 1),
('MacBook Air M2', 'MacBook Air M2芯片，8GB内存，256GB存储', 8999.00, '[\"https://example.com/macbook1.jpg\"]', 1),
('AirPods Pro', 'AirPods Pro第二代，主动降噪', 1899.00, '[\"https://example.com/airpods1.jpg\"]', 1)
ON DUPLICATE KEY UPDATE title=title;
"

echo "MySQL安装完成！"
echo "数据库: item_platform"
echo "用户名: root"
echo "密码: $NEW_PASSWORD"
echo "远程访问: 已开启"
echo ""
echo "测试连接命令:"
echo "mysql -u root -p$NEW_PASSWORD -h localhost item_platform"
echo ""
echo "查看状态: systemctl status mysqld"
echo "启动服务: systemctl start mysqld"
echo "停止服务: systemctl stop mysqld"
echo "重启服务: systemctl restart mysqld"