# ItemPlatform 阿里云部署 - 快速开始

## 🚀 最快部署方案（推荐）

### 方案A：Docker容器化部署（最简单，10分钟）

```bash
# 1. 安装Docker
curl -fsSL https://get.docker.com | sh
sudo systemctl start docker
sudo systemctl enable docker

# 2. 创建项目目录
mkdir -p /opt/itemplatform
cd /opt/itemplatform

# 3. 创建docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: itemplatform-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ItemPlatform2024!
      MYSQL_DATABASE: item_platform
      MYSQL_USER: item_user
      MYSQL_PASSWORD: ItemPlatform2024!
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    restart: unless-stopped

  app:
    image: node:16-alpine
    container_name: itemplatform-app
    working_dir: /app
    volumes:
      - ./:/app
      - /app/node_modules
    ports:
      - "3000:3000"
    environment:
      NODE_ENV: production
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: item_platform
      DB_USER: item_user
      DB_PASSWORD: ItemPlatform2024!
    depends_on:
      - mysql
    restart: unless-stopped
    command: sh -c "npm install && npm start"

  nginx:
    image: nginx:alpine
    container_name: itemplatform-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./static:/usr/share/nginx/html/static:ro
    depends_on:
      - app
    restart: unless-stopped

volumes:
  mysql_data:
EOF

# 4. 创建nginx.conf
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream app {
        server app:3000;
    }

    server {
        listen 80;
        server_name localhost;

        client_max_body_size 50M;

        location /api/ {
            proxy_pass http://app/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_cache_bypass $http_upgrade;

            add_header 'Access-Control-Allow-Origin' '*';
            add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
            add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization';

            if ($request_method = 'OPTIONS') {
                return 204;
            }
        }

        location /static/ {
            alias /usr/share/nginx/html/static/;
            expires 30d;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }

        location / {
            return 200 'ItemPlatform API Server is running';
            add_header Content-Type text/plain;
        }
    }
}
EOF

# 5. 上传文件到 /opt/itemplatform
# 需要上传的文件：
# - server.js
# - package.json
# - sql/init_database.sql
# - static/ 目录（如果有）

# 6. 启动服务
sudo docker-compose up -d

# 7. 查看日志
sudo docker-compose logs -f

# 8. 验证部署
curl http://localhost/health
curl http://localhost/api/health
```

---

### 方案B：手动部署（如果Docker不可用）

#### 步骤1：修复CentOS 7仓库（使用阿里云镜像）

```bash
# 备份仓库
sudo mkdir -p /etc/yum.repos.d/backup
sudo cp /etc/yum.repos.d/*.repo /etc/yum.repos.d/backup/

# 使用阿里云镜像
sudo tee /etc/yum.repos.d/CentOS-Base.repo << 'EOF'
[base]
name=CentOS-7 - Base - Aliyun
baseurl=http://mirrors.aliyun.com/centos-vault/7.9.2009/os/$basearch/
gpgcheck=1
gpgkey=http://mirrors.aliyun.com/centos-vault/RPM-GPG-KEY-CentOS-7
enabled=1

[updates]
name=CentOS-7 - Updates - Aliyun
baseurl=http://mirrors.aliyun.com/centos-vault/7.9.2009/updates/$basearch/
gpgcheck=1
gpgkey=http://mirrors.aliyun.com/centos-vault/RPM-GPG-KEY-CentOS-7
enabled=1

[extras]
name=CentOS-7 - Extras - Aliyun
baseurl=http://mirrors.aliyun.com/centos-vault/7.9.2009/extras/$basearch/
gpgcheck=1
gpgkey=http://mirrors.aliyun.com/centos-vault/RPM-GPG-KEY-CentOS-7
enabled=1
EOF

# 禁用有问题的仓库
sudo yum-config-manager --disable centos-sclo-rh 2>/dev/null || true
sudo yum-config-manager --disable centos-sclo-sclo 2>/dev/null || true

# 清理缓存
sudo yum clean all
sudo yum makecache fast
```

#### 步骤2：安装基础工具

```bash
sudo yum install -y curl wget vim net-tools git gcc gcc-c++ make lsof unzip tar
```

#### 步骤3：配置防火墙

```bash
sudo systemctl start firewalld
sudo systemctl enable firewalld
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

#### 步骤4：安装MySQL

```bash
# 下载MySQL仓库
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm

# 安装MySQL
sudo yum install -y mysql-server

# 启动MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 安全配置（使用临时密码）
sudo mysql_secure_installation

# 创建数据库和用户
mysql -u root -p << 'EOF'
CREATE DATABASE item_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'item_user'@'localhost' IDENTIFIED BY 'ItemPlatform2024!';
CREATE USER 'item_user'@'%' IDENTIFIED BY 'ItemPlatform2024!';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'localhost';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'%';
FLUSH PRIVILEGES;
EOF

# 导入数据库
mysql -u item_user -pItemPlatform2024! item_platform < /opt/itemplatform/sql/init_database.sql
```

#### 步骤5：安装Node.js

```bash
# 方法1：使用NodeSource仓库
curl -fsSL https://rpm.nodesource.com/setup_16.x | sudo bash -
sudo yum install -y nodejs

# 方法2：如果方法1失败，使用二进制包
cd /tmp
wget https://nodejs.org/dist/v16.20.2/node-v16.20.2-linux-x64.tar.xz
tar -xf node-v16.20.2-linux-x64.tar.xz
sudo mv node-v16.20.2-linux-x64 /usr/local/node
sudo ln -sf /usr/local/node/bin/node /usr/bin/node
sudo ln -sf /usr/local/node/bin/npm /usr/bin/npm

# 验证安装
node --version
npm --version
```

#### 步骤6：部署应用

```bash
# 创建目录
sudo mkdir -p /opt/itemplatform
sudo mkdir -p /opt/itemplatform/logs
sudo mkdir -p /opt/itemplatform/static

# 上传文件到 /opt/itemplatform
# - server.js
# - package.json

# 安装依赖
cd /opt/itemplatform
npm install --production

# 测试运行
node server.js

# 使用PM2管理进程
sudo npm install -g pm2
pm2 start server.js --name itemplatform-api
pm2 save
pm2 startup
```

#### 步骤7：安装和配置Nginx

```bash
# 安装Nginx
sudo yum install -y nginx

# 配置Nginx
sudo tee /etc/nginx/conf.d/itemplatform.conf << 'EOF'
server {
    listen 80;
    server_name localhost;

    client_max_body_size 50M;

    location /api/ {
        proxy_pass http://localhost:3000/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_cache_bypass $http_upgrade;

        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization';

        if ($request_method = 'OPTIONS') {
            return 204;
        }
    }

    location /static/ {
        alias /opt/itemplatform/static/;
        expires 30d;
    }

    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }

    location / {
        return 200 'ItemPlatform API Server is running';
        add_header Content-Type text/plain;
    }
}
EOF

# 测试配置
sudo nginx -t

# 启动Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

#### 步骤8：验证部署

```bash
# 检查服务状态
sudo systemctl status mysqld
sudo systemctl status nginx
pm2 status

# 测试API
curl http://localhost/health
curl http://localhost/api/health

# 获取服务器IP
curl ifconfig.me
```

---

## 🎯 选择哪个方案？

| 方案 | 时间 | 难度 | 推荐度 | 适用场景 |
|------|------|------|--------|----------|
| Docker部署 | 10分钟 | 简单 | ⭐⭐⭐⭐⭐ | 所有情况，特别是CentOS 7 EOL问题 |
| 手动部署 | 30分钟 | 中等 | ⭐⭐⭐⭐ | Docker不可用或需要完全控制 |

---

## ✅ 部署完成后的配置

### 在Android客户端配置API地址

```kotlin
// 在你的Android项目中更新API地址
const val BASE_URL = "http://你的服务器IP/api/"
```

### 常用命令

```bash
# Docker方案
sudo docker-compose ps          # 查看服务状态
sudo docker-compose logs -f     # 查看日志
sudo docker-compose restart     # 重启服务
sudo docker-compose down        # 停止服务

# 手动部署方案
sudo systemctl status mysqld    # MySQL状态
sudo systemctl status nginx     # Nginx状态
pm2 status                     # Node.js应用状态
pm2 logs itemplatform-api       # 查看应用日志
pm2 restart itemplatform-api    # 重启应用
```

---

## 🆘 遇到问题？

### Docker部署问题

```bash
# 检查Docker是否运行
sudo systemctl status docker

# 查看容器日志
sudo docker logs itemplatform-mysql
sudo docker logs itemplatform-app
sudo docker logs itemplatform-nginx

# 重启容器
sudo docker-compose restart
```

### 手动部署问题

```bash
# 检查MySQL
sudo systemctl status mysqld
sudo tail -f /var/log/mysqld.log

# 检查Nginx
sudo systemctl status nginx
sudo tail -f /var/log/nginx/error.log

# 检查Node.js应用
pm2 logs itemplatform-api

# 检查端口占用
sudo netstat -tlnp | grep -E ':(80|3000|3306)'
```

### 网络问题

```bash
# 检查防火墙
sudo firewall-cmd --list-all

# 检查安全组（在阿里云控制台）
# 确保开放端口：22, 80, 443, 3000, 3306

# 测试网络连接
ping -c 3 baidu.com
curl -I http://mirrors.aliyun.com
```

---

## 📞 需要帮助？

1. **查看详细部署指南**：`DEPLOYMENT_GUIDE.md`
2. **查看故障排除**：`CENTOS_EOL_TROUBLESHOOTING.md`
3. **联系阿里云技术支持**
4. **在GitHub提交Issue**

---

## 🎉 完成！

如果所有检查都通过，恭喜你成功部署了ItemPlatform！

现在可以在Android客户端配置API地址：
```
BASE_URL = "http://你的服务器IP/api/"
```