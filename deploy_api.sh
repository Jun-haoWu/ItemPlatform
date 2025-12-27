#!/bin/bash
set -e

echo "=== API服务部署 ==="

# 1. 检查依赖
echo "正在检查依赖..."
if ! command -v node &> /dev/null; then
    echo "❌ Node.js未安装，请先运行nodejs_setup.sh"
    exit 1
fi

if ! command -v pm2 &> /dev/null; then
    echo "❌ PM2未安装，正在安装..."
    npm install -g pm2
fi

# 2. 进入应用目录
echo "正在进入应用目录..."
cd /opt/itemplatform

# 3. 检查server.js文件
if [ ! -f "server.js" ]; then
    echo "❌ server.js文件不存在，请上传server.js文件到/opt/itemplatform/目录"
    echo "你可以使用以下命令上传:"
    echo "scp server.js root@your-server:/opt/itemplatform/"
    exit 1
fi

# 4. 创建package.json（如果不存在）
if [ ! -f "package.json" ]; then
    echo "正在创建package.json..."
    cat > package.json << 'EOF'
{
  "name": "itemplatform-api",
  "version": "1.0.0",
  "description": "Item Platform Backend API",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "mysql2": "^3.6.5",
    "cors": "^2.8.5",
    "bcryptjs": "^2.4.3",
    "jsonwebtoken": "^9.0.2",
    "dotenv": "^16.3.1",
    "express-rate-limit": "^7.1.5"
  },
  "devDependencies": {
    "nodemon": "^3.0.2"
  }
}
EOF
fi

# 5. 安装依赖
echo "正在安装依赖包..."
npm install

# 6. 创建PM2配置文件
echo "正在创建PM2配置文件..."
cat > ecosystem.config.js << 'EOF'
module.exports = {
  apps: [{
    name: 'itemplatform-api',
    script: './server.js',
    instances: 2,
    exec_mode: 'cluster',
    env: {
      NODE_ENV: 'production',
      PORT: 3000,
      DB_HOST: 'localhost',
      DB_USER: 'item_user',
      DB_PASSWORD: 'ItemPlatform2024!',
      DB_NAME: 'item_platform',
      JWT_SECRET: 'your-jwt-secret-key-change-this-in-production'
    },
    error_file: '/opt/itemplatform/logs/err.log',
    out_file: '/opt/itemplatform/logs/out.log',
    log_file: '/opt/itemplatform/logs/combined.log',
    time: true,
    max_memory_restart: '500M',
    min_uptime: '10s',
    max_restarts: 5
  }]
};
EOF

# 7. 创建环境变量文件（如果不存在）
if [ ! -f ".env" ]; then
    echo "正在创建环境变量文件..."
    cat > .env << 'EOF'
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=item_platform
DB_USER=item_user
DB_PASSWORD=ItemPlatform2024!

# JWT配置
JWT_SECRET=your_jwt_secret_key_here_change_this_in_production
JWT_EXPIRES_IN=24h

# 服务器配置
PORT=3000
NODE_ENV=production

# CORS配置
ALLOWED_ORIGINS=*

# 文件上传配置
MAX_FILE_SIZE=10485760
UPLOAD_DIR=/opt/itemplatform/static/
EOF
fi

# 8. 停止现有服务（如果存在）
echo "正在停止现有服务..."
pm2 stop itemplatform-api 2>/dev/null || true
pm2 delete itemplatform-api 2>/dev/null || true

# 9. 启动服务
echo "正在启动API服务..."
pm2 start ecosystem.config.js

# 10. 保存PM2配置
echo "正在保存PM2配置..."
pm2 save

# 11. 等待服务启动
echo "等待服务启动..."
sleep 5

# 12. 验证服务
echo "正在验证服务..."
if pm2 list | grep -q "itemplatform-api.*online"; then
    echo "✅ API服务部署完成"
    echo "- 服务名称: itemplatform-api"
    echo "- 运行模式: cluster (2个实例)"
    echo "- 监听端口: 3000"
    echo "- 日志文件: /opt/itemplatform/logs/"
    echo ""
    echo "PM2管理命令:"
    echo "- 查看状态: pm2 list"
    echo "- 查看日志: pm2 logs itemplatform-api"
    echo "- 重启服务: pm2 restart itemplatform-api"
    echo "- 停止服务: pm2 stop itemplatform-api"
else
    echo "❌ API服务启动失败"
    echo "请查看日志:"
    echo "- PM2日志: pm2 logs itemplatform-api"
    echo "- 错误日志: tail -f /opt/itemplatform/logs/err.log"
    exit 1
fi

# 13. 测试API端点
echo ""
echo "正在测试API端点..."
sleep 3

# 测试健康检查
echo -n "测试健康检查端点... "
if curl -s http://localhost:3000/health > /dev/null; then
    echo "✅ 正常"
else
    echo "❌ 失败"
fi

# 测试数据库连接
echo -n "测试数据库连接... "
if curl -s http://localhost:3000/api/health > /dev/null; then
    echo "✅ 正常"
else
    echo "❌ 失败"
fi

echo ""
echo "🎉 API服务部署成功！"
echo "下一步可以配置Nginx反向代理（运行setup_nginx.sh）"