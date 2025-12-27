# ItemPlatform 阿里云部署完整指南

## 🎯 总体方案（按推荐顺序）

### 方案一：Docker容器化部署（最推荐）⭐⭐⭐⭐⭐
**优点**：避免系统依赖问题，环境隔离，易于维护
**缺点**：需要学习Docker
**适用场景**：所有情况，特别是CentOS 7 EOL问题

### 方案二：手动分步部署（最稳定）⭐⭐⭐⭐
**优点**：完全控制，问题易排查
**缺点**：步骤多，需要手动操作
**适用场景**：自动化脚本失败时

### 方案三：更换操作系统（最彻底）⭐⭐⭐⭐⭐
**优点**：彻底解决CentOS 7问题
**缺点**：需要重新创建服务器
**适用场景**：新服务器或可以重装

---

## 📋 方案一：Docker容器化部署（推荐）

### 步骤1：安装Docker
```bash
# 1. 卸载旧版本（如果有）
sudo yum remove docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine

# 2. 安装Docker
curl -fsSL https://get.docker.com | sh

# 3. 启动Docker
sudo systemctl start docker
sudo systemctl enable docker

# 4. 验证安装
sudo docker --version
```

### 步骤2：创建Docker Compose配置
```bash
# 创建项目目录
mkdir -p /opt/itemplatform
cd /opt/itemplatform

# 创建docker-compose.yml
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
    networks:
      - itemplatform-network
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
    networks:
      - itemplatform-network
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
    networks:
      - itemplatform-network
    restart: unless-stopped

volumes:
  mysql_data:

networks:
  itemplatform-network:
    driver: bridge
EOF
```

### 步骤3：创建Nginx配置
```bash
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
```

### 步骤4：上传项目文件
```bash
# 上传以下文件到服务器 /opt/itemplatform 目录：
# - server.js
# - package.json
# - sql/init_database.sql
# - static/ 目录（如果有）

# 创建必要的目录
mkdir -p /opt/itemplatform/sql
mkdir -p /opt/itemplatform/static
```

### 步骤5：启动服务
```bash
# 启动所有服务
sudo docker-compose up -d

# 查看日志
sudo docker-compose logs -f

# 检查服务状态
sudo docker-compose ps
```

### 步骤6：验证部署
```bash
# 检查MySQL
sudo docker exec -it itemplatform-mysql mysql -u item_user -pItemPlatform2024! item_platform -e "SHOW TABLES;"

# 检查API
curl http://localhost/health

# 检查数据库连接
curl http://localhost/api/health
```

---

## 📋 方案二：手动分步部署（稳定）

### 步骤1：修复仓库（使用阿里云镜像）
```bash
# 1. 备份仓库配置
sudo mkdir -p /etc/yum.repos.d/backup
sudo cp /etc/yum.repos.d/*.repo /etc/yum.repos.d/backup/

# 2. 使用阿里云镜像源
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

# 3. 禁用有问题的仓库
sudo yum-config-manager --disable centos-sclo-rh 2>/dev/null || true
sudo yum-config-manager --disable centos-sclo-sclo 2>/dev/null || true

# 4. 清理缓存
sudo yum clean all
sudo yum makecache fast
```

### 步骤2：安装基础工具
```bash
# 安装核心工具
sudo yum install -y curl wget vim net-tools git

# 安装开发工具
sudo yum install -y gcc gcc-c++ make

# 安装其他工具
sudo yum install -y lsof unzip tar
```

### 步骤3：配置防火墙
```bash
# 启动防火墙
sudo systemctl start firewalld
sudo systemctl enable firewalld

# 开放端口
sudo firewall-cmd --permanent --add-port=22/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --permanent --add-port=443/tcp
sudo firewall-cmd --permanent --add-port=3000/tcp
sudo firewall-cmd --permanent --add-port=3306/tcp
sudo firewall-cmd --reload
```

### 步骤4：安装MySQL
```bash
# 1. 下载MySQL仓库
sudo yum install -y https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm

# 2. 安装MySQL
sudo yum install -y mysql-server

# 3. 启动MySQL
sudo systemctl start mysqld
sudo systemctl enable mysqld

# 4. 获取临时密码
sudo grep 'temporary password' /var/log/mysqld.log

# 5. 安全配置（使用临时密码）
sudo mysql_secure_installation

# 6. 创建数据库和用户
mysql -u root -p << 'EOF'
CREATE DATABASE item_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'item_user'@'localhost' IDENTIFIED BY 'ItemPlatform2024!';
CREATE USER 'item_user'@'%' IDENTIFIED BY 'ItemPlatform2024!';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'localhost';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'%';
FLUSH PRIVILEGES;
EOF

# 7. 导入数据库
mysql -u item_user -pItemPlatform2024! item_platform < /opt/itemplatform/sql/init_database.sql
```

### 步骤5：安装Node.js
```bash
# 使用NodeSource仓库
curl -fsSL https://rpm.nodesource.com/setup_16.x | sudo bash -
sudo yum install -y nodejs

# 验证安装
node --version
npm --version
```

### 步骤6：部署应用
```bash
# 1. 创建目录
sudo mkdir -p /opt/itemplatform
sudo mkdir -p /opt/itemplatform/logs
sudo mkdir -p /opt/itemplatform/static

# 2. 上传文件到 /opt/itemplatform
# - server.js
# - package.json

# 3. 安装依赖
cd /opt/itemplatform
npm install --production

# 4. 测试运行
node server.js

# 5. 使用PM2管理进程
sudo npm install -g pm2
pm2 start server.js --name itemplatform-api
pm2 save
pm2 startup
```

### 步骤7：安装和配置Nginx
```bash
# 1. 安装Nginx
sudo yum install -y nginx

# 2. 配置Nginx
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

# 3. 测试配置
sudo nginx -t

# 4. 启动Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 步骤8：验证部署
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

## 📋 方案三：更换操作系统（最彻底）

### 推荐的操作系统

1. **AlmaLinux 8/9** - CentOS的直接替代品
2. **Rocky Linux 8/9** - 另一个CentOS替代品
3. **Ubuntu 20.04/22.04 LTS** - 社区支持好
4. **Debian 11/12** - 非常稳定

### 更换步骤

1. **在阿里云控制台停止当前服务器**
2. **创建新的服务器实例**
3. **选择推荐的操作系统**
4. **使用方案一或方案二进行部署**

---

## 🔍 故障排除

### 问题1：yum命令失败
```bash
# 检查DNS
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf

# 检查网络
ping -c 3 baidu.com

# 手动测试仓库
curl -I http://mirrors.aliyun.com/centos-vault/
```

### 问题2：MySQL连接失败
```bash
# 检查MySQL状态
sudo systemctl status mysqld

# 检查MySQL日志
sudo tail -f /var/log/mysqld.log

# 测试连接
mysql -u item_user -pItemPlatform2024! -h localhost item_platform
```

### 问题3：Node.js安装失败
```bash
# 使用二进制包安装
cd /tmp
wget https://nodejs.org/dist/v16.20.2/node-v16.20.2-linux-x64.tar.xz
tar -xf node-v16.20.2-linux-x64.tar.xz
sudo mv node-v16.20.2-linux-x64 /usr/local/node
sudo ln -sf /usr/local/node/bin/node /usr/bin/node
sudo ln -sf /usr/local/node/bin/npm /usr/bin/npm
```

### 问题4：Nginx启动失败
```bash
# 检查配置
sudo nginx -t

# 检查日志
sudo tail -f /var/log/nginx/error.log

# 检查端口占用
sudo netstat -tlnp | grep :80
```

---

## 📞 获取帮助

如果以上方案都无法解决：

1. **检查阿里云控制台的安全组设置**
2. **查看服务器日志**：`/var/log/`
3. **联系阿里云技术支持**
4. **在Stack Overflow或GitHub Issues提问**

---

## ✅ 部署检查清单

部署完成后，请检查以下项目：

- [ ] MySQL服务运行正常
- [ ] Node.js应用运行正常
- [ ] Nginx服务运行正常
- [ ] 防火墙端口已开放
- [ ] 数据库连接正常
- [ ] API接口可访问
- [ ] 静态文件可访问
- [ ] 健康检查通过
- [ ] Android客户端可以连接

---

## 🎉 完成！

如果所有检查都通过，恭喜你成功部署了ItemPlatform！

现在可以在Android客户端配置API地址：
```
BASE_URL = "http://你的服务器IP/api/"
```