# Docker 部署文件清单

## 已创建的文件

### 核心配置文件
- [x] `package.json` - Node.js 依赖配置
- [x] `docker-compose.yml` - Docker 编排配置
- [x] `nginx.conf` - Nginx 反向代理配置
- [x] `.env.example` - 环境变量示例

### 数据库文件
- [x] `sql/init_database.sql` - 数据库初始化脚本（包含表结构和示例数据）

### 文档文件
- [x] `README_DOCKER.md` - Docker 部署完整指南
- [x] `DEPLOYMENT_GUIDE.md` - 完整部署指南（包含多种方案）
- [x] `QUICK_START.md` - 快速开始指南
- [x] `CENTOS_EOL_TROUBLESHOOTING.md` - CentOS 7 EOL 故障排查指南

### 服务器代码
- [x] `server.js` - Node.js API 服务器（已存在）

## 部署前检查清单

### 服务器准备
- [ ] 已购买阿里云 ECS 服务器
- [ ] 已配置安全组规则（开放 80、3000、3306 端口）
- [ ] 已获得服务器公网 IP 地址
- [ ] 已通过 SSH 连接到服务器

### 文件上传
需要上传以下文件到服务器的 `/opt/itemplatform` 目录：

```
/opt/itemplatform/
├── server.js              # 服务器代码
├── package.json           # 依赖配置
├── docker-compose.yml     # Docker 编排
├── nginx.conf             # Nginx 配置
├── .env.example           # 环境变量示例
└── sql/
    └── init_database.sql  # 数据库脚本
```

上传方法：

**方法 1：使用 scp 命令**
```bash
# 在本地 Windows PowerShell 中执行
scp server.js root@your-server-ip:/opt/itemplatform/
scp package.json root@your-server-ip:/opt/itemplatform/
scp docker-compose.yml root@your-server-ip:/opt/itemplatform/
scp nginx.conf root@your-server-ip:/opt/itemplatform/
scp .env.example root@your-server-ip:/opt/itemplatform/
scp -r sql root@your-server-ip:/opt/itemplatform/
```

**方法 2：使用 FTP/SFTP 工具**
- 使用 FileZilla、WinSCP 等工具
- 连接到服务器
- 上传文件到 `/opt/itemplatform` 目录

**方法 3：在服务器上直接创建**
```bash
# SSH 连接到服务器后
mkdir -p /opt/itemplatform/sql
cd /opt/itemplatform

# 使用 vi 或 nano 编辑器创建文件
vi server.js
vi package.json
vi docker-compose.yml
vi nginx.conf
vi .env.example
vi sql/init_database.sql
```

### Docker 安装检查
- [ ] Docker 已安装
- [ ] Docker 服务已启动
- [ ] Docker 服务已设置为开机自启

验证命令：
```bash
sudo docker --version
sudo systemctl status docker
```

### 环境配置
- [ ] 已复制 `.env.example` 为 `.env`
- [ ] 已根据需要修改 `.env` 中的配置（可选）

```bash
cp .env.example .env
```

### 服务启动
- [ ] 已执行 `docker-compose up -d`
- [ ] 所有容器状态为 "Up"
- [ ] 健康检查通过

验证命令：
```bash
sudo docker-compose ps
curl http://localhost/health
curl http://localhost/api/health
```

### 功能测试
- [ ] API 接口可访问
- [ ] 数据库连接正常
- [ ] 用户注册功能正常
- [ ] 用户登录功能正常
- [ ] 商品列表可获取
- [ ] 收藏功能正常

测试命令：
```bash
# 测试健康检查
curl http://localhost/health

# 测试 API 健康检查
curl http://localhost/api/health

# 测试用户注册
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# 测试获取商品列表
curl http://localhost/api/products
```

## 部署步骤总结

### 1. 安装 Docker
```bash
curl -fsSL https://get.docker.com | sh
sudo systemctl start docker
sudo systemctl enable docker
```

### 2. 创建项目目录
```bash
mkdir -p /opt/itemplatform/sql
cd /opt/itemplatform
```

### 3. 上传文件
使用上述任一方法上传所有必要文件

### 4. 配置环境变量
```bash
cp .env.example .env
# 可选：编辑 .env 文件修改配置
```

### 5. 启动服务
```bash
sudo docker-compose up -d
```

### 6. 验证部署
```bash
sudo docker-compose ps
curl http://localhost/health
curl http://localhost/api/health
```

## 常见问题

### Q: Docker 安装失败怎么办？
A: 使用 `fix_repos_aliyun.sh` 脚本修复 CentOS 7 仓库配置

### Q: 容器无法启动？
A: 查看日志 `sudo docker-compose logs -f`，检查端口占用

### Q: 数据库连接失败？
A: 检查 MySQL 容器状态，等待数据库完全启动

### Q: 如何查看日志？
A: `sudo docker-compose logs -f [service_name]`

### Q: 如何重启服务？
A: `sudo docker-compose restart`

### Q: 如何停止服务？
A: `sudo docker-compose down`

### Q: 数据会丢失吗？
A: 不会，MySQL 数据存储在 Docker volume 中

### Q: 如何备份数据？
A: `sudo docker-compose exec mysql mysqldump -u item_user -pItemPlatform2024! item_platform > backup.sql`

## 下一步

部署完成后，需要：

1. **修改 Android 客户端配置**
   - 更新 `CloudConfig.kt` 中的服务器地址
   - 重新编译并安装 Android 应用

2. **配置安全设置**
   - 修改默认密码
   - 配置防火墙
   - 考虑启用 HTTPS

3. **设置监控**
   - 配置日志收集
   - 设置告警机制
   - 定期备份数据

## 联系支持

如遇到问题，请参考：
- [README_DOCKER.md](README_DOCKER.md) - 完整部署指南
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - 多种部署方案
- [QUICK_START.md](QUICK_START.md) - 快速开始
- [CENTOS_EOL_TROUBLESHOOTING.md](CENTOS_EOL_TROUBLESHOOTING.md) - 故障排查
