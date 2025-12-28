const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');
require('dotenv').config();

async function createTestUser() {
  const connection = await mysql.createConnection({
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 3306,
    user: process.env.DB_USER || 'item_user',
    password: process.env.DB_PASSWORD || 'ItemPlatform2024!',
    database: process.env.DB_NAME || 'item_platform'
  });

  try {
    const username = 'testuser';
    const password = 'password123';
    const email = 'test@example.com';

    const hashedPassword = await bcrypt.hash(password, 10);

    await connection.execute(
      'INSERT INTO users (username, password, email) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE password = ?',
      [username, hashedPassword, email, hashedPassword]
    );

    console.log('测试用户创建成功！');
    console.log('用户名:', username);
    console.log('密码:', password);
    console.log('邮箱:', email);
  } catch (error) {
    console.error('创建测试用户失败:', error);
  } finally {
    await connection.end();
  }
}

createTestUser();
