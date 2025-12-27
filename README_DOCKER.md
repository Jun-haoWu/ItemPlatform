# Item Platform - Docker Deployment Guide

## 项目概述

Item Platform 是一个支持商品详情页和收藏功能的移动应用，采用本地 SQLite 存储和云端同步的双向同步策略。

## Docker 部署方案（推荐）

Docker 部署是最简单、最可靠的部署方式，避免了 CentOS 7 EOL 带来的各种依赖问题。

### 快速开始（10分钟部署）

#### 前提条件
- 一台阿里云 CentOS 7 服务器
- 服务器已配置好安全组规则（开放 80、3000、3306 端口）
- root 或具有 sudo 权限的用户

#### 部署步骤

**1. 安装 Docker**

```bash
# 卸载旧版本（如果有）
sudo yum remove docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine

# 安装 Docker
curl -fsSL https://get.docker.com | sh

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
sudo docker --version
```

**2. 创建项目目录**

```bash
mkdir -p /opt/itemplatform
cd /opt/itemplatform
```

**3. 上传必要文件到服务器**

需要上传以下文件到 `/opt/itemplatform` 目录：

```
/opt/itemplatform/
├── server.js              # Node.js 服务器代码
├── package.json           # Node.js 依赖配置
├── docker-compose.yml     # Docker 编排配置
├── nginx.conf             # Nginx 配置
├── .env.example           # 环境变量示例
└── sql/
    └── init_database.sql  # 数据库初始化脚本
```

**4. 配置环境变量**

```bash
# 复制环境变量示例文件
cp .env.example .env

# 根据需要修改 .env 文件中的配置
# 默认配置已经可以正常工作
```

**5. 启动服务**

```bash
# 构建并启动所有服务
sudo docker-compose up -d

# 查看服务状态
sudo docker-compose ps

# 查看日志
sudo docker-compose logs -f
```

**6. 验证部署**

```bash
# 检查健康状态
curl http://localhost/health

# 测试 API
curl http://localhost/api/health

# 查看所有服务状态
sudo docker-compose ps
```

### 服务说明

Docker Compose 会启动以下三个服务：

1. **MySQL 数据库** (`itemplatform-mysql`)
   - 端口：3306
   - 数据库：item_platform
   - 用户：item_user
   - 密码：ItemPlatform2024!

2. **Node.js API 服务器** (`itemplatform-app`)
   - 端口：3000
   - 提供 RESTful API 接口

3. **Nginx 反向代理** (`itemplatform-nginx`)
   - 端口：80
   - 提供统一的访问入口和静态文件服务

### 常用命令

```bash
# 启动服务
sudo docker-compose up -d

# 停止服务
sudo docker-compose down

# 重启服务
sudo docker-compose restart

# 查看日志
sudo docker-compose logs -f [service_name]

# 进入容器
sudo docker-compose exec app sh
sudo docker-compose exec mysql bash

# 查看服务状态
sudo docker-compose ps

# 重新构建并启动
sudo docker-compose up -d --build
```

### 配置说明

#### 环境变量 (.env)

```env
NODE_ENV=production
PORT=3000

# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_NAME=item_platform
DB_USER=item_user
DB_PASSWORD=ItemPlatform2024!

# JWT 配置
JWT_SECRET=ItemPlatform2024SecretKey
JWT_EXPIRES_IN=24h

# 允许的源
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:80
```

#### 端口映射

- **80**: Nginx（对外访问入口）
- **3000**: Node.js API（内部使用，通过 Nginx 代理）
- **3306**: MySQL（内部使用，如需外部访问请谨慎）

### 数据持久化

MySQL 数据存储在 Docker volume 中，即使容器重启或删除，数据也不会丢失。

```bash
# 查看数据卷
sudo docker volume ls

# 备份数据
sudo docker-compose exec mysql mysqldump -u item_user -pItemPlatform2024! item_platform > backup.sql

# 恢复数据
sudo docker-compose exec -T mysql mysql -u item_user -pItemPlatform2024! item_platform < backup.sql
```

### 故障排查

**1. 服务无法启动**

```bash
# 查看详细日志
sudo docker-compose logs -f

# 检查端口占用
sudo netstat -tlnp | grep -E '80|3000|3306'
```

**2. 数据库连接失败**

```bash
# 检查 MySQL 容器状态
sudo docker-compose ps mysql

# 进入 MySQL 容器
sudo docker-compose exec mysql bash

# 测试数据库连接
mysql -u item_user -pItemPlatform2024! -h localhost item_platform
```

**3. API 无法访问**

```bash
# 检查 Nginx 配置
sudo docker-compose exec nginx nginx -t

# 重启 Nginx
sudo docker-compose restart nginx

# 检查 API 服务
sudo docker-compose exec app wget -O- http://localhost:3000/health
```

**4. 容器内存不足**

```bash
# 查看容器资源使用
sudo docker stats

# 限制容器资源使用（编辑 docker-compose.yml）
services:
  app:
    deploy:
      resources:
        limits:
          memory: 512M
```

### 安全建议

1. **修改默认密码**
   - 修改 `.env` 中的数据库密码
   - 修改 `docker-compose.yml` 中的 MySQL root 密码

2. **配置防火墙**
   ```bash
   # 只开放必要的端口
   sudo firewall-cmd --permanent --add-port=80/tcp
   sudo firewall-cmd --reload
   ```

3. **使用 HTTPS**
   - 配置 SSL 证书
   - 修改 Nginx 配置支持 HTTPS

4. **定期备份数据**
   ```bash
   # 创建定时备份任务
   crontab -e
   
   # 添加以下行（每天凌晨 2 点备份）
   0 2 * * * cd /opt/itemplatform && sudo docker-compose exec -T mysql mysqldump -u item_user -pItemPlatform2024! item_platform > /backup/itemplatform_$(date +\%Y\%m\%d).sql
   ```

### 性能优化

1. **启用 Nginx 缓存**
   - Nginx 配置已包含静态文件缓存
   - 可根据需要调整缓存时间

2. **数据库优化**
   - 定期清理过期数据
   - 添加适当的索引
   - 监控慢查询

3. **日志管理**
   ```bash
   # 配置日志轮转
   sudo docker-compose logs --tail=100 -f
   ```

### 监控和维护

```bash
# 查看容器资源使用
sudo docker stats

# 查看磁盘使用
sudo df -h

# 清理未使用的 Docker 资源
sudo docker system prune -a

# 更新服务
sudo docker-compose pull
sudo docker-compose up -d
```

## API 接口文档

### 基础 URL

```
http://your-server-ip/api
```

### 认证接口

#### 注册
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123",
  "email": "test@example.com",
  "phone": "13800138000"
}
```

#### 登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

### 商品接口

#### 获取商品列表
```
GET /api/products?page=1&limit=20&category=digital&search=iPhone
```

#### 获取商品详情
```
GET /api/products/:id
```

### 收藏接口

#### 获取收藏列表
```
GET /api/favorites
Authorization: Bearer <token>
```

#### 添加收藏
```
POST /api/favorites/:productId
Authorization: Bearer <token>
```

#### 取消收藏
```
DELETE /api/favorites/:productId
Authorization: Bearer <token>
```

#### 同步收藏
```
POST /api/sync/favorites
Authorization: Bearer <token>
Content-Type: application/json

{
  "favorites": [
    {
      "productId": 1,
      "action": "add",
      "timestamp": "2024-01-01T00:00:00Z"
    }
  ]
}
```

## Android 客户端配置

更新 Android 应用中的服务器地址：

```kotlin
// CloudConfig.kt
const val BASE_URL = "http://your-server-ip/api/"
```

## 支持

如遇到问题，请查看：
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - 完整部署指南
- [QUICK_START.md](QUICK_START.md) - 快速开始指南
- [CENTOS_EOL_TROUBLESHOOTING.md](CENTOS_EOL_TROUBLESHOOTING.md) - 故障排查指南
