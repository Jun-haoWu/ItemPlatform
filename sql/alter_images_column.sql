-- 修改 products 表的 images 字段类型从 JSON 改为 TEXT
USE item_platform;

-- 备份现有数据（如果需要）
-- CREATE TABLE products_backup AS SELECT * FROM products;

-- 修改 images 字段类型
ALTER TABLE products MODIFY COLUMN images TEXT;

-- 验证修改
DESCRIBE products;