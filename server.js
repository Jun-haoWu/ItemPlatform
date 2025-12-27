const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

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

// 限流中间件
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15分钟
  max: 100, // 限制每个IP每15分钟最多100次请求
  message: '请求过于频繁，请稍后再试'
});

// CORS配置
const corsOptions = {
  origin: function (origin, callback) {
    const allowedOrigins = (process.env.ALLOWED_ORIGINS || 'http://localhost:3000').split(',');
    if (!origin || allowedOrigins.indexOf(origin) !== -1) {
      callback(null, true);
    } else {
      callback(new Error('不允许的源'));
    }
  },
  credentials: true,
  optionsSuccessStatus: 200
};

// 中间件
app.use(cors(corsOptions));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(limiter);

// JWT验证中间件
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: '需要登录' });
  }

  jwt.verify(token, process.env.JWT_SECRET || 'default_secret', (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'token无效' });
    }
    req.user = user;
    next();
  });
}

// 健康检查
app.get('/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    timestamp: new Date().toISOString(),
    database: pool ? 'connected' : 'disconnected'
  });
});

// 错误处理中间件
function errorHandler(err, req, res, next) {
  console.error('错误:', err);
  
  if (err.code === 'ER_DUP_ENTRY') {
    return res.status(409).json({ error: '数据已存在' });
  }
  
  if (err.code === 'ER_NO_REFERENCED_ROW_2') {
    return res.status(400).json({ error: '关联数据不存在' });
  }
  
  if (err.name === 'JsonWebTokenError') {
    return res.status(401).json({ error: 'token验证失败' });
  }
  
  res.status(500).json({ 
    error: '服务器内部错误',
    message: process.env.NODE_ENV === 'development' ? err.message : '未知错误'
  });
}

// 用户认证相关API
app.post('/api/auth/register', async (req, res, next) => {
  try {
    const { username, password, email, phone } = req.body;
    
    if (!username || !password) {
      return res.status(400).json({ error: '用户名和密码是必填项' });
    }
    
    if (password.length < 6) {
      return res.status(400).json({ error: '密码长度至少6位' });
    }
    
    const hashedPassword = await bcrypt.hash(password, 10);
    
    const [result] = await pool.execute(
      'INSERT INTO users (username, password, email, phone) VALUES (?, ?, ?, ?)',
      [username, hashedPassword, email || null, phone || null]
    );
    
    const token = jwt.sign(
      { userId: result.insertId, username },
      process.env.JWT_SECRET || 'default_secret',
      { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
    );
    
    res.status(201).json({
      message: '用户注册成功',
      token,
      user: { id: result.insertId, username, email, phone }
    });
  } catch (error) {
    next(error);
  }
});

app.post('/api/auth/login', async (req, res, next) => {
  try {
    const { username, password } = req.body;
    
    if (!username || !password) {
      return res.status(400).json({ error: '用户名和密码是必填项' });
    }
    
    const [rows] = await pool.execute(
      'SELECT id, username, password, email, phone FROM users WHERE username = ?',
      [username]
    );
    
    if (rows.length === 0) {
      return res.status(401).json({ error: '用户名或密码错误' });
    }
    
    const user = rows[0];
    const isValidPassword = await bcrypt.compare(password, user.password);
    
    if (!isValidPassword) {
      return res.status(401).json({ error: '用户名或密码错误' });
    }
    
    const token = jwt.sign(
      { userId: user.id, username: user.username },
      process.env.JWT_SECRET || 'default_secret',
      { expiresIn: process.env.JWT_EXPIRES_IN || '24h' }
    );
    
    res.json({
      message: '登录成功',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        phone: user.phone
      }
    });
  } catch (error) {
    next(error);
  }
});

// 商品相关API
app.get('/api/products', async (req, res, next) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
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
    
    const [products] = await pool.execute(
      `SELECT id, name, description, price, original_price, images, category, location, like_count, view_count, created_at 
       FROM products ${whereClause} 
       ORDER BY created_at DESC 
       LIMIT ? OFFSET ?`,
      [...params, limit, offset]
    );
    
    const [totalResult] = await pool.execute(
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

app.get('/api/products/:id', async (req, res, next) => {
  try {
    const productId = parseInt(req.params.id);
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    // 更新浏览次数
    await pool.execute(
      'UPDATE products SET view_count = view_count + 1 WHERE id = ?',
      [productId]
    );
    
    const [rows] = await pool.execute(
      `SELECT id, name, description, price, original_price, images, category, location, like_count, view_count, created_at 
       FROM products WHERE id = ? AND status = "active"`,
      [productId]
    );
    
    if (rows.length === 0) {
      return res.status(404).json({ error: '商品不存在' });
    }
    
    res.json({ product: rows[0] });
  } catch (error) {
    next(error);
  }
});

// 收藏相关API
app.get('/api/favorites', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const [favorites] = await pool.execute(
      `SELECT p.id, p.name, p.description, p.price, p.original_price, p.images, p.category, p.location, p.like_count, p.view_count, f.created_at as favorited_at
       FROM favorites f
       JOIN products p ON f.product_id = p.id
       WHERE f.user_id = ? AND p.status = "active"
       ORDER BY f.created_at DESC`,
      [userId]
    );
    
    res.json({ favorites });
  } catch (error) {
    next(error);
  }
});

app.post('/api/favorites/:productId', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const productId = parseInt(req.params.productId);
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    // 检查商品是否存在
    const [productCheck] = await pool.execute(
      'SELECT id FROM products WHERE id = ? AND status = "active"',
      [productId]
    );
    
    if (productCheck.length === 0) {
      return res.status(404).json({ error: '商品不存在' });
    }
    
    // 添加收藏
    await pool.execute(
      'INSERT INTO favorites (user_id, product_id) VALUES (?, ?)',
      [userId, productId]
    );
    
    // 更新商品收藏数
    await pool.execute(
      'UPDATE products SET like_count = like_count + 1 WHERE id = ?',
      [productId]
    );
    
    res.json({ message: '收藏成功' });
  } catch (error) {
    next(error);
  }
});

app.delete('/api/favorites/:productId', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const productId = parseInt(req.params.productId);
    
    if (isNaN(productId)) {
      return res.status(400).json({ error: '无效的商品ID' });
    }
    
    // 删除收藏
    const [result] = await pool.execute(
      'DELETE FROM favorites WHERE user_id = ? AND product_id = ?',
      [userId, productId]
    );
    
    if (result.affectedRows === 0) {
      return res.status(404).json({ error: '收藏不存在' });
    }
    
    // 更新商品收藏数
    await pool.execute(
      'UPDATE products SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?',
      [productId]
    );
    
    res.json({ message: '取消收藏成功' });
  } catch (error) {
    next(error);
  }
});

// 同步API - 用于移动端同步收藏数据
app.post('/api/sync/favorites', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const { favorites } = req.body; // [{ productId, action, timestamp }]
    
    if (!Array.isArray(favorites)) {
      return res.status(400).json({ error: '无效的同步数据格式' });
    }
    
    let successCount = 0;
    let errorCount = 0;
    const results = [];
    
    for (const item of favorites) {
      try {
        const { productId, action } = item;
        
        if (action === 'add') {
          // 添加收藏（如果已存在则跳过）
          await pool.execute(
            'INSERT IGNORE INTO favorites (user_id, product_id) VALUES (?, ?)',
            [userId, productId]
          );
          
          // 更新商品收藏数
          await pool.execute(
            'UPDATE products SET like_count = like_count + 1 WHERE id = ?',
            [productId]
          );
        } else if (action === 'remove') {
          // 删除收藏
          const [result] = await pool.execute(
            'DELETE FROM favorites WHERE user_id = ? AND product_id = ?',
            [userId, productId]
          );
          
          if (result.affectedRows > 0) {
            // 更新商品收藏数
            await pool.execute(
              'UPDATE products SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?',
              [productId]
            );
          }
        }
        
        successCount++;
        results.push({ productId, action, status: 'success' });
      } catch (error) {
        errorCount++;
        results.push({ productId, action: item.action, status: 'error', error: error.message });
      }
    }
    
    res.json({
      message: '同步完成',
      syncResult: {
        total: favorites.length,
        success: successCount,
        errors: errorCount,
        results
      }
    });
  } catch (error) {
    next(error);
  }
});

// 获取用户收藏状态（用于检查多个商品）
app.post('/api/favorites/status', authenticateToken, async (req, res, next) => {
  try {
    const userId = req.user.userId;
    const { productIds } = req.body;
    
    if (!Array.isArray(productIds) || productIds.length === 0) {
      return res.status(400).json({ error: '无效的商品ID列表' });
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

startServer().catch(console.error);