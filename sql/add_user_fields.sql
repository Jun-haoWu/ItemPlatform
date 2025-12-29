-- 添加用户表缺失的字段
ALTER TABLE users ADD COLUMN real_name VARCHAR(50) AFTER phone;
ALTER TABLE users ADD COLUMN student_id VARCHAR(20) AFTER real_name;
ALTER TABLE users ADD COLUMN department VARCHAR(100) AFTER student_id;
