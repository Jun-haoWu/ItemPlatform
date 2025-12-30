USE item_platform;

DELETE FROM products;

INSERT INTO products (user_id, name, description, price, original_price, images, category, location, status) VALUES
(1, 'iPhone 15 Pro Max', '全新未拆封，256GB，原装正品，支持验机', 8999.00, 9999.00, '["https://example.com/iphone1.jpg", "https://example.com/iphone2.jpg"]', '数码', '北京', 'active'),
(1, 'MacBook Pro 14寸', 'M3芯片，16GB内存，512GB存储，使用3个月，成色99新', 12999.00, 16999.00, '["https://example.com/macbook1.jpg"]', '数码', '上海', 'active'),
(1, 'Nike Air Jordan 1', '限量款，42码，全新未穿过，原盒', 1999.00, 2499.00, '["https://example.com/jordan1.jpg", "https://example.com/jordan2.jpg"]', '鞋服', '广州', 'active'),
(1, 'Sony WH-1000XM5', '降噪耳机，黑色，使用半年，功能完好', 1599.00, 2499.00, '["https://example.com/sony1.jpg"]', '数码', '深圳', 'active'),
(1, 'Switch OLED版', '白色，附带3个游戏，使用1年，无划痕', 1899.00, 2599.00, '["https://example.com/switch1.jpg", "https://example.com/switch2.jpg"]', '数码', '杭州', 'active'),
(1, '小米13 Ultra', '12GB+256GB，黑色，全新未拆封', 4999.00, 5999.00, '["https://example.com/xiaomi1.jpg"]', '数码', '成都', 'active'),
(1, 'iPad Air 5', '64GB WiFi版，蓝色，使用3个月，无磕碰', 3299.00, 4399.00, '["https://example.com/ipad1.jpg"]', '数码', '武汉', 'active'),
(1, 'Dyson V12吸尘器', '全新未使用，原装配件齐全', 2999.00, 3999.00, '["https://example.com/dyson1.jpg"]', '家电', '南京', 'active'),
(1, 'Levi\'s 501牛仔裤', '32码，深蓝色，全新带吊牌', 399.00, 699.00, '["https://example.com/levis1.jpg"]', '鞋服', '西安', 'active'),
(1, 'Canon R6相机', '24-70mm镜头套机，使用半年，快门次数5000', 12599.00, 16999.00, '["https://example.com/canon1.jpg", "https://example.com/canon2.jpg"]', '数码', '重庆', 'active');
