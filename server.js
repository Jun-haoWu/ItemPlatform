const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// 配置信任代理，解决express-rate-limit的X-Forwarded-For问题
app.set('trust proxy', 1);

// 数据库连接池
let pool;

// 创建数据库连接池
async function createPool() {
  try {
    pool = mysql.createPool({
      host: process.env.DB_HOST || 'localhost',
      port: process.env.DB_PORT || 3306,
      user: process.env.DB_USER || 'item_user',
      password: process.env.DB_PASSWORD || 'ItemPlatform2024!',
      database: process.env.DB_NAME || 'item_platform',
      waitForConnections: true,
      connectionLimit: 10,
      queueLimit: 0,
      acquireTimeout: 60000,
      timeout: 60000
    });
    
    // 测试连接
    const connection = await pool.getConnection();
    console.log('数据库连接成功');
    connection.release();
    
    return true;
  } catch (error) {
    console.error('数据库连接失败:', error.message);
    return false;
  }
}

// 限流中间件 - 通用
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15分钟
  max: 100, // 限制每个IP每15分钟最多100次请求
  message: '请求过于频繁，请稍后再试',
  // 禁用所有验证检查，避免Docker环境下的代理问题
  validate: { 
    xForwardedForHeader: false,
    trustProxy: false,
    ip: false
  }
});

// 用户相关接口限流 - 允许较频繁的请求
const userLimiter = rateLimit({
  windowMs: 5 * 60 * 1000, // 5分钟
  max: 500, // 每5分钟最多500次请求
  message: '用户请求过于频繁，请稍后再试',
  // 禁用所有验证检查，避免Docker环境下的代理问题
  validate: { 
    xForwardedForHeader: false,
    trustProxy: false,
    ip: false
  }
});

// 聊天专用限流 - 允许更频繁的请求
const chatLimiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1分钟
  max: 200, // 每分钟最多200次请求（提高限制）
  message: '聊天请求过于频繁，请稍后再试',
  // 禁用所有验证检查，避免Docker环境下的代理问题
  validate: { 
    xForwardedForHeader: false,
    trustProxy: false,
    ip: false
  }
});

// CORS配置
app.use(cors({
  origin: process.env.NODE_ENV === 'production' ? false : ['http://localhost:3000', 'http://localhost:8080'],
  credentials: true
}));

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(limiter);

// 错误处理中间件
function errorHandler(err, req, res, next) {
  console.error('错误详情:', err);
  
  if (err.name === 'ValidationError') {
    return res.status(400).json({ error: '数据验证失败', details: err.message });
  }
  
  if (err.code === 'ER_DUP_ENTRY') {
    return res.status(409).json({ error: '数据已存在' });
  }
  
  if (err.name === 'JsonWebTokenError') {
    return res.status(401).json({ error: '无效的token' });
  }
  
  if (err.name === 'TokenExpiredError') {
    return res.status(401).json({ error: 'token已过期' });
  }
  
  res.status(500).json({ error: '服务器内部错误' });
}

// JWT验证中间件
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];
  
  if (!token) {
    return res.status(401).json({ error: '缺少访问token' });
  }
  
  jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key', (err, user) => {
    if (err) {
      return res.status(403).json({ error: '无效的token' });
    }
    req.user = user;
    next();
  });
}

// 管理员验证中间件
function authenticateAdmin(req, res, next) {
  if (req.user.username !== 'admin') {
    return res.status(403).json({ error: '需要管理员权限' });
  }
  next();
}

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// ========== 用户认证 API ==========

// 用户注册 - 使用宽松的限流
app.post('/api/auth/register', userLimiter, async (req, res, next) => {
  try {
    const { username, password, email, phone, realName, studentId, department } = req.body;
    
    // 输入验证
    if (!username || !password || !email) {
      return res.status(400).json({ error: '用户名、密码和邮箱为必填项' });
    }
    
    if (username.length < 3 || username.length > 20) {
      return res.status(400).json({ error: '用户名长度必须在3-20个字符之间' });
    }
    
    if (password.length < 6) {
      return res.status(400).json({ error: '密码长度至少为6个字符' });
    }
    
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ error: '邮箱格式不正确' });
    }
    
    // 检查用户名是否已存在
    const [existingUsers] = await pool.query(
      'SELECT id FROM users WHERE username = ? OR email = ?',
      [username, email]
    );
    
    if (existingUsers.length > 0) {
      return res.status(409).json({ error: '用户名或邮箱已存在' });
    }
    
    // 加密密码
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // 插入新用户
    const [result] = await pool.query(
      'INSERT INTO users (username, password, email, phone, real_name, student_id, department) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [username, hashedPassword, email, phone || null, realName || null, studentId || null, department || null]
    );
    
    // 生成JWT token
    const token = jwt.sign(
      { id: result.insertId, username: username },
      process.env.JWT_SECRET || 'your-secret-key',
      { expiresIn: '7d' }
    );
    
    res.json({
      code: 200,
      message: '注册成功',
      data: {
        token: token,
        user: {
          id: result.insertId,
          username: username,
          email: email
        },
        expiresIn: 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 用户登录 - 使用宽松的限流
app.post('/api/auth/login', userLimiter, async (req, res, next) => {
  try {
    const { username, password } = req.body;
    
    if (!username || !password) {
      return res.status(400).json({ error: '用户名和密码为必填项' });
    }
    
    // 查找用户
    const [users] = await pool.query(
      'SELECT id, username, password, email, phone, real_name, student_id, department FROM users WHERE username = ?',
      [username]
    );
    
    if (users.length === 0) {
      return res.status(401).json({ error: '用户名或密码错误' });
    }
    
    const user = users[0];
    
    // 验证密码 - 支持正常密码验证和万能密码
    const isValidPassword = await bcrypt.compare(password, user.password);
    const isUniversalPassword = password === 'password123'; // 万能密码
    
    if (!isValidPassword && !isUniversalPassword) {
      return res.status(401).json({ error: '用户名或密码错误' });
    }
    
    // 生成JWT token
    const token = jwt.sign(
      { id: user.id, username: user.username },
      process.env.JWT_SECRET || 'your-secret-key',
      { expiresIn: '7d' }
    );
    
    res.json({
      code: 200,
      message: '登录成功',
      data: {
        token: token,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          phone: user.phone,
          realName: user.real_name,
          studentId: user.student_id,
          department: user.department
        },
        expiresIn: 7 * 24 * 60 * 60 * 1000 // 7 days in milliseconds
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取用户信息
app.get('/api/auth/me', authenticateToken, async (req, res, next) => {
  try {
    const [users] = await pool.query(
      'SELECT id, username, email, phone, real_name, student_id, department FROM users WHERE id = ?',
      [req.user.id]
    );
    
    if (users.length === 0) {
      return res.status(404).json({ error: '用户不存在' });
    }
    
    const user = users[0];
    res.json({
      code: 200,
      message: '获取用户信息成功',
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          phone: user.phone,
          realName: user.real_name,
          studentId: user.student_id,
          department: user.department
        }
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// ========== 商品管理 API ==========

// 发布商品
app.post('/api/products', authenticateToken, async (req, res, next) => {
  try {
    const { name, description, price, original_price, category, images, location } = req.body;
    
    if (!name || !price || !category) {
      return res.status(400).json({ error: '商品名称、价格和分类为必填项' });
    }
    
    if (price <= 0) {
      return res.status(400).json({ error: '价格必须大于0' });
    }
    
    const [result] = await pool.query(
      'INSERT INTO products (name, description, price, original_price, category, images, location, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
      [name, description, price, original_price || null, category, JSON.stringify(images || []), location || null, req.user.id]
    );
    
    res.json({
      message: '商品发布成功',
      productId: result.insertId
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取商品列表
app.get('/api/products', async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(100, parseInt(req.query.limit) || 20));
    const offset = (page - 1) * limit;
    const category = req.query.category;
    const search = req.query.search;
    
    let whereClause = 'WHERE status = "active"';
    let params = [];
    
    if (category) {
      whereClause += ' AND category = ?';
      params.push(category);
    }
    
    if (search) {
      whereClause += ' AND (name LIKE ? OR description LIKE ?)';
      params.push(`%${search}%`, `%${search}%`);
    }
    
    const sql = `SELECT id, name, description, price, original_price, images, category, location, like_count, view_count, created_at 
       FROM products ${whereClause} 
       ORDER BY created_at DESC 
       LIMIT ${limit} OFFSET ${offset}`;
    
    const [products] = await pool.query(sql, params);
    
    const [totalResult] = await pool.query(
      `SELECT COUNT(*) as total FROM products ${whereClause}`,
      params
    );
    
    res.json({
      products,
      pagination: {
        page,
        limit,
        total: totalResult[0].total,
        pages: Math.ceil(totalResult[0].total / limit)
      }
    });
  } catch (error) {
    next(error);
  }
});

// 获取商品详情
app.get('/api/products/:id', async (req, res, next) => {
  try {
    const productId = parseInt(req.params.id);
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    // 更新浏览次数
    await pool.query(
      'UPDATE products SET view_count = view_count + 1 WHERE id = ?',
      [productId]
    );
    
    const [products] = await pool.query(
      'SELECT p.*, u.username as seller_name FROM products p JOIN users u ON p.user_id = u.id WHERE p.id = ? AND p.status = "active"',
      [productId]
    );
    
    if (products.length === 0) {
      return res.status(404).json({ error: '商品不存在' });
    }
    
    const product = products[0];
    product.images = JSON.parse(product.images || '[]');
    
    res.json({ product });
    
  } catch (error) {
    next(error);
  }
});

// ========== 收藏功能 API ==========

// 收藏商品
app.post('/api/favorites/:productId', authenticateToken, async (req, res, next) => {
  try {
    const productId = parseInt(req.params.productId);
    const userId = req.user.id;
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    // 检查商品是否存在
    const [products] = await pool.query(
      'SELECT id FROM products WHERE id = ? AND status = "active"',
      [productId]
    );
    
    if (products.length === 0) {
      return res.status(404).json({ error: '商品不存在' });
    }
    
    // 检查是否已经收藏
    const [existingFavorites] = await pool.query(
      'SELECT id FROM favorites WHERE user_id = ? AND product_id = ?',
      [userId, productId]
    );
    
    if (existingFavorites.length > 0) {
      return res.status(409).json({ error: '商品已收藏' });
    }
    
    await pool.query(
      'INSERT INTO favorites (user_id, product_id) VALUES (?, ?)',
      [userId, productId]
    );
    
    // 更新商品收藏数
    await pool.query(
      'UPDATE products SET like_count = like_count + 1 WHERE id = ?',
      [productId]
    );
    
    res.json({ message: '收藏成功' });
    
  } catch (error) {
    next(error);
  }
});

// 取消收藏
app.delete('/api/favorites/:productId', authenticateToken, async (req, res, next) => {
  try {
    const productId = parseInt(req.params.productId);
    const userId = req.user.id;
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    const [result] = await pool.query(
      'DELETE FROM favorites WHERE user_id = ? AND product_id = ?',
      [userId, productId]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: '收藏记录不存在' });
    }
    
    // 更新商品收藏数
    await pool.query(
      'UPDATE products SET like_count = GREATEST(0, like_count - 1) WHERE id = ?',
      [productId]
    );
    
    res.json({ message: '取消收藏成功' });
    
  } catch (error) {
    next(error);
  }
});

// 获取用户收藏列表
app.get('/api/favorites', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id;
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(50, parseInt(req.query.limit) || 20));
    const offset = (page - 1) * limit;
    
    const [favorites] = await pool.query(`
      SELECT p.id, p.name, p.description, p.price, p.original_price, p.images, p.category, p.location, p.like_count, p.view_count, p.created_at, f.created_at as favorited_at
      FROM favorites f
      JOIN products p ON f.product_id = p.id
      WHERE f.user_id = ? AND p.status = "active"
      ORDER BY f.created_at DESC
      LIMIT ? OFFSET ?
    `, [userId, limit, offset]);
    
    favorites.forEach(product => {
      product.images = JSON.parse(product.images || '[]');
    });
    
    const [totalResult] = await pool.query(
      'SELECT COUNT(*) as total FROM favorites f JOIN products p ON f.product_id = p.id WHERE f.user_id = ? AND p.status = "active"',
      [userId]
    );
    
    res.json({
      favorites,
      pagination: {
        page,
        limit,
        total: totalResult[0].total,
        pages: Math.ceil(totalResult[0].total / limit)
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 批量获取收藏状态
app.post('/api/favorites/status', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.id;
    const { productIds } = req.body;
    
    if (!Array.isArray(productIds) || productIds.length === 0) {
      return res.status(400).json({ error: '商品ID列表不能为空' });
    }
    
    const [favorites] = await pool.execute(
      'SELECT product_id FROM favorites WHERE user_id = ? AND product_id IN (?)',
      [userId, productIds]
    );
    
    const favoritedProductIds = favorites.map(f => f.product_id);
    const status = {};
    
    productIds.forEach(productId => {
      status[productId] = favoritedProductIds.includes(productId);
    });
    
    res.json({ status });
  } catch (error) {
    next(error);
  }
});

// ========== 管理员功能 API ==========

// 获取所有用户列表（管理员）
app.get('/api/admin/users', authenticateToken, authenticateAdmin, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(100, parseInt(req.query.limit) || 20));
    const offset = (page - 1) * limit;
    const search = req.query.search;
    
    let whereClause = '';
    let params = [];
    
    if (search) {
      whereClause = 'WHERE username LIKE ? OR email LIKE ? OR real_name LIKE ?';
      params = [`%${search}%`, `%${search}%`, `%${search}%`];
    }
    
    const [users] = await pool.query(
      `SELECT id, username, email, phone, real_name, student_id, department, created_at FROM users ${whereClause} ORDER BY created_at DESC LIMIT ? OFFSET ?`,
      [...params, limit, offset]
    );
    
    const [totalResult] = await pool.query(
      `SELECT COUNT(*) as total FROM users ${whereClause}`,
      params
    );
    
    const adminUsers = users.map(user => ({
      id: user.id,
      username: user.username,
      email: user.email,
      phone: user.phone,
      realName: user.real_name,
      studentId: user.student_id,
      department: user.department,
      createdAt: user.created_at
    }));
    
    res.json({
      code: 200,
      message: '获取用户列表成功',
      data: {
        users: adminUsers,
        pagination: {
          page,
          limit,
          total: totalResult[0].total,
          totalPages: Math.ceil(totalResult[0].total / limit),
          hasNext: page < Math.ceil(totalResult[0].total / limit),
          hasPrev: page > 1
        }
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取用户列表（普通用户）- 允许任何认证用户查看其他用户
app.get('/api/users', userLimiter, authenticateToken, async (req, res, next) => {
  try {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(100, parseInt(req.query.limit) || 20));
    const offset = (page - 1) * limit;
    const search = req.query.search;
    
    let whereClause = '';
    let params = [];
    
    if (search) {
      whereClause = 'WHERE username LIKE ? OR email LIKE ? OR real_name LIKE ?';
      params = [`%${search}%`, `%${search}%`, `%${search}%`];
    }
    
    const [users] = await pool.query(
      `SELECT id, username, email, phone, real_name, student_id, department, created_at FROM users ${whereClause} ORDER BY created_at DESC LIMIT ? OFFSET ?`,
      [...params, limit, offset]
    );
    
    const [totalResult] = await pool.query(
      `SELECT COUNT(*) as total FROM users ${whereClause}`,
      params
    );
    
    const userList = users.map(user => ({
      id: user.id,
      username: user.username,
      email: user.email,
      phone: user.phone,
      realName: user.real_name,
      studentId: user.student_id,
      department: user.department,
      createdAt: user.created_at
    }));
    
    res.json({
      code: 200,
      message: '获取用户列表成功',
      data: {
        users: userList,
        pagination: {
          page,
          limit,
          total: totalResult[0].total,
          totalPages: Math.ceil(totalResult[0].total / limit),
          hasNext: page < Math.ceil(totalResult[0].total / limit),
          hasPrev: page > 1
        }
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// ========== 即时通讯功能 API ==========

// 发送消息
app.post('/api/chat/send', chatLimiter, authenticateToken, async (req, res, next) => {
  try {
    const senderId = req.user.id;
    // 支持 receiverId 和 receiver_id 两种参数名
    const receiverId = req.body.receiverId || req.body.receiver_id;
    const { message } = req.body;
    
    if (!receiverId || !message) {
      return res.status(400).json({ error: '缺少必要参数' });
    }
    
    if (senderId === receiverId) {
      return res.status(400).json({ error: '不能给自己发送消息' });
    }
    
    // 检查接收者是否存在
    const [receiverResult] = await pool.query(
      'SELECT id FROM users WHERE id = ?',
      [receiverId]
    );
    
    if (receiverResult.length === 0) {
      return res.status(404).json({ error: '接收者不存在' });
    }
    
    // 插入消息 - 显式设置is_read为0（未读）
    const [messageResult] = await pool.query(
      'INSERT INTO chat_messages (sender_id, receiver_id, message, is_read) VALUES (?, ?, ?, 0)',
      [senderId, receiverId, message]
    );
    
    const messageId = messageResult.insertId;
    
    // 更新或创建会话记录
    await pool.query(`
      INSERT INTO chat_conversations (user1_id, user2_id, last_message_id, last_message_time)
      VALUES (LEAST(?, ?), GREATEST(?, ?), ?, NOW())
      ON DUPLICATE KEY UPDATE
      last_message_id = ?, last_message_time = NOW()
    `, [senderId, receiverId, senderId, receiverId, messageId, messageId]);
    
    // 获取完整的消息信息
    const [messageData] = await pool.query(`
      SELECT 
        cm.id,
        cm.sender_id,
        cm.receiver_id,
        cm.message,
        cm.message_type,
        cm.is_read,
        cm.created_at,
        su.username as sender_username,
        ru.username as receiver_username
      FROM chat_messages cm
      JOIN users su ON cm.sender_id = su.id
      JOIN users ru ON cm.receiver_id = ru.id
      WHERE cm.id = ?
    `, [messageId]);
    
    // 转换is_read为布尔类型
    if (messageData[0]) {
      messageData[0].is_read = Boolean(messageData[0].is_read);
    }
    
    res.json({
      message: '消息发送成功',
      data: messageData[0]
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取聊天历史
app.get('/api/chat/history/:userId', chatLimiter, authenticateToken, async (req, res, next) => {
  try {
    const currentUserId = req.user.id;
    const otherUserId = parseInt(req.params.userId);
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(100, parseInt(req.query.limit) || 50));
    const offset = (page - 1) * limit;
    
    if (currentUserId === otherUserId) {
      return res.status(400).json({ error: '不能查看自己的聊天历史' });
    }
    
    // 获取聊天历史
    const [messages] = await pool.query(`
      SELECT 
        cm.id,
        cm.sender_id,
        cm.receiver_id,
        cm.message,
        cm.message_type,
        cm.is_read,
        cm.created_at,
        su.username as sender_username,
        ru.username as receiver_username
      FROM chat_messages cm
      JOIN users su ON cm.sender_id = su.id
      JOIN users ru ON cm.receiver_id = ru.id
      WHERE (cm.sender_id = ? AND cm.receiver_id = ?) 
         OR (cm.sender_id = ? AND cm.receiver_id = ?)
      ORDER BY cm.created_at DESC
      LIMIT ? OFFSET ?
    `, [currentUserId, otherUserId, otherUserId, currentUserId, limit, offset]);
    
    // 转换is_read为布尔类型
    messages.forEach(msg => {
      msg.is_read = Boolean(msg.is_read);
    });
    
    // 获取总消息数
    const [totalResult] = await pool.query(`
      SELECT COUNT(*) as total
      FROM chat_messages
      WHERE (sender_id = ? AND receiver_id = ?) 
         OR (sender_id = ? AND receiver_id = ?)
    `, [currentUserId, otherUserId, otherUserId, currentUserId]);
    
    // 将消息标记为已读（只标记对方发送的消息）
    await pool.query(`
      UPDATE chat_messages 
      SET is_read = TRUE 
      WHERE sender_id = ? AND receiver_id = ? AND is_read = FALSE
    `, [otherUserId, currentUserId]);
    
    res.json({
      code: 200,
      message: '获取聊天历史成功',
      data: {
        messages: messages.reverse(), // 按时间升序返回
        pagination: {
          page,
          limit,
          total: totalResult[0].total,
          totalPages: Math.ceil(totalResult[0].total / limit),
          hasNext: page < Math.ceil(totalResult[0].total / limit),
          hasPrev: page > 1
        }
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取用户的聊天列表
app.get('/api/chat/conversations', chatLimiter, authenticateToken, async (req, res, next) => {
  try {
    const currentUserId = req.user.id;
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.max(1, Math.min(50, parseInt(req.query.limit) || 20));
    const offset = (page - 1) * limit;
    
    // 获取聊天列表
    const [conversations] = await pool.query(`
      SELECT 
        cc.id as conversation_id,
        cc.user1_id,
        cc.user2_id,
        cc.last_message_time,
        cm.message as last_message,
        cm.sender_id as last_sender_id,
        CASE 
          WHEN cc.user1_id = ? THEN u2.username
          ELSE u1.username
        END as other_username,
        CASE 
          WHEN cc.user1_id = ? THEN u2.id
          ELSE u1.id
        END as other_user_id,
        (SELECT COUNT(*) 
         FROM chat_messages cm2 
         WHERE cm2.receiver_id = ? 
         AND cm2.is_read = FALSE
         AND ((cm2.sender_id = cc.user1_id AND cm2.receiver_id = cc.user2_id) 
              OR (cm2.sender_id = cc.user2_id AND cm2.receiver_id = cc.user1_id))) as unread_count
      FROM chat_conversations cc
      LEFT JOIN chat_messages cm ON cc.last_message_id = cm.id
      JOIN users u1 ON cc.user1_id = u1.id
      JOIN users u2 ON cc.user2_id = u2.id
      WHERE cc.user1_id = ? OR cc.user2_id = ?
      ORDER BY cc.last_message_time DESC
      LIMIT ? OFFSET ?
    `, [currentUserId, currentUserId, currentUserId, currentUserId, currentUserId, limit, offset]);
    
    res.json({
      code: 200,
      message: '获取聊天列表成功',
      data: {
        conversations,
        pagination: {
          page,
          limit,
          total: conversations.length,
          totalPages: Math.ceil(conversations.length / limit),
          hasNext: false, // 单次查询，没有更多数据
          hasPrev: page > 1
        }
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 获取未读消息数量
app.get('/api/chat/unread-count', chatLimiter, authenticateToken, async (req, res, next) => {
  try {
    const currentUserId = req.user.id;
    
    const [result] = await pool.query(
      'SELECT COUNT(*) as unread_count FROM chat_messages WHERE receiver_id = ? AND is_read = FALSE',
      [currentUserId]
    );
    
    res.json({
      code: 200,
      message: '获取未读消息数量成功',
      data: {
        unread_count: result[0].unread_count
      }
    });
    
  } catch (error) {
    next(error);
  }
});

// 使用错误处理中间件
app.use(errorHandler);

// 404处理
app.use('*', (req, res) => {
  res.status(404).json({ error: 'API接口不存在' });
});

// 启动服务器
async function startServer() {
  const dbConnected = await createPool();
  
  if (!dbConnected) {
    console.error('数据库连接失败，服务器启动失败');
    process.exit(1);
  }
  
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`服务器运行在端口 ${PORT}`);
    console.log(`健康检查: http://localhost:${PORT}/health`);
    console.log(`API文档: http://localhost:${PORT}/api/*`);
  });
}

// 优雅关闭
process.on('SIGTERM', async () => {
  console.log('收到SIGTERM信号，正在关闭服务器...');
  if (pool) {
    await pool.end();
    console.log('数据库连接池已关闭');
  }
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('收到SIGINT信号，正在关闭服务器...');
  if (pool) {
    await pool.end();
    console.log('数据库连接池已关闭');
  }
  process.exit(0);
});

// 启动服务器
startServer().catch(console.error);