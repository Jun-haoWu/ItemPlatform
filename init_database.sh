#!/bin/bash
set -e

echo "=== 数据库表结构初始化 ==="

# 检查数据库连接
echo "正在检查数据库连接..."
mysql -u item_user -pItemPlatform2024! item_platform -e "SELECT 1;" > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ 无法连接到数据库，请先运行mysql_setup.sh"
    exit 1
fi

# 检查SQL文件是否存在
if [ ! -f "./sql/init_database.sql" ]; then
    echo "正在创建SQL文件..."
    mkdir -p sql
    cat > sql/init_database.sql << 'EOF'
-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    status ENUM('active', 'sold', 'inactive') DEFAULT 'active',
    like_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 商品图片表
CREATE TABLE IF NOT EXISTS product_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO users (username, email, password_hash) VALUES 
('testuser', 'test@example.com', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'),
('admin', 'admin@example.com', '$2b$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');

INSERT INTO products (user_id, title, description, price, category) VALUES 
(1, 'iPhone 13 Pro', '九成新iPhone，无磕碰', 6999.00, '电子产品'),
(1, 'MacBook Air', 'M1芯片，8GB内存，256GB存储', 7999.00, '电子产品'),
(2, '小米电视', '55寸4K电视，支持HDR', 2999.00, '家电');

INSERT INTO product_images (product_id, image_url, sort_order) VALUES 
(1, 'https://via.placeholder.com/600x400?text=iPhone+13+Pro', 0),
(2, 'https://via.placeholder.com/600x400?text=MacBook+Air', 0),
(3, 'https://via.placeholder.com/600x400?text=Mi+TV', 0);
EOF
fi

# 执行SQL脚本
echo "正在执行数据库初始化脚本..."
mysql -u item_user -pItemPlatform2024! item_platform < sql/init_database.sql

# 验证表创建
echo "正在验证表创建..."
table_count=$(mysql -u item_user -pItemPlatform2024! item_platform -e "SHOW TABLES;" | wc -l)
if [ $table_count -gt 4 ]; then
    echo "✅ 数据库表结构初始化完成"
    echo "- 创建表数量: $((table_count - 1))"  # 减去表头行
    echo "- 测试数据已插入"
else
    echo "❌ 数据库表创建验证失败"
    exit 1
fi