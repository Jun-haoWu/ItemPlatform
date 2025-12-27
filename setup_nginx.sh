#!/bin/bash
set -e

echo "=== Nginx反向代理配置 ==="

# 1. 安装Nginx
echo "正在安装Nginx..."
# 方法1: 尝试使用EPEL仓库
yum install -y epel-release
if yum install -y nginx; then
    echo "✅ 使用EPEL仓库安装Nginx成功"
else
    echo "EPEL仓库安装失败，尝试使用Nginx官方仓库..."
    
    # 方法2: 使用Nginx官方仓库
    echo "正在添加Nginx官方仓库..."
    cat > /etc/yum.repos.d/nginx.repo << 'EOF'
[nginx-stable]
name=nginx stable repo
baseurl=http://nginx.org/packages/centos/7/$basearch/
gpgcheck=1
enabled=1
gpgkey=https://nginx.org/keys/nginx_signing.key
module_hotfixes=true
EOF
    
    # 导入GPG密钥
    echo "正在导入GPG密钥..."
    rpm --import https://nginx.org/keys/nginx_signing.key || echo "GPG密钥导入失败，继续安装..."
    
    # 安装Nginx
    echo "正在安装Nginx..."
    if yum install -y nginx; then
        echo "✅ 使用Nginx官方仓库安装成功"
    else
        echo "❌ Nginx官方仓库安装失败，尝试备用方法..."
        
        # 方法3: 使用CentOS SCL仓库
        echo "正在尝试使用SCL仓库..."
        yum install -y centos-release-scl || echo "SCL仓库安装失败"
        
        # 方法4: 编译安装（最后手段）
        echo "所有仓库方法失败，尝试编译安装..."
        yum install -y gcc pcre-devel zlib-devel openssl-devel wget make
        
        cd /tmp
        wget -q http://nginx.org/download/nginx-1.20.2.tar.gz
        if [ $? -eq 0 ]; then
            tar -xzf nginx-1.20.2.tar.gz
            cd nginx-1.20.2
            ./configure --prefix=/usr/local/nginx --with-http_ssl_module --with-http_v2_module
            make && make install
            
            # 创建systemd服务文件
            cat > /etc/systemd/system/nginx.service << 'EOF'
[Unit]
Description=nginx - high performance web server
Documentation=http://nginx.org/en/docs/
After=network-online.target remote-fs.target nss-lookup.target
Wants=network-online.target

[Service]
Type=forking
PIDFile=/usr/local/nginx/logs/nginx.pid
ExecStartPre=/usr/local/nginx/sbin/nginx -t -c /usr/local/nginx/conf/nginx.conf
ExecStart=/usr/local/nginx/sbin/nginx -c /usr/local/nginx/conf/nginx.conf
ExecReload=/bin/kill -s HUP $MAINPID
ExecStop=/bin/kill -s TERM $MAINPID
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF
            
            systemctl daemon-reload
            systemctl enable nginx
            
            # 创建nginx用户和组（编译安装时需要手动创建）
            if ! id -u nginx > /dev/null 2>&1; then
                echo "正在创建nginx用户和组..."
                groupadd -r nginx
                useradd -r -g nginx -s /sbin/nologin nginx
                echo "✅ nginx用户和组创建成功"
            fi
            
            # 添加nginx到PATH
            ln -sf /usr/local/nginx/sbin/nginx /usr/bin/nginx
            echo "✅ 编译安装Nginx成功"
            
            # 清理临时文件
            cd /
            rm -rf /tmp/nginx-1.20.2*
        else
            echo "❌ 所有Nginx安装方法都失败"
            exit 1
        fi
    fi
fi

# 确保nginx用户存在（适用于所有安装方法）
if ! id -u nginx > /dev/null 2>&1; then
    echo "正在创建nginx用户和组..."
    groupadd -r nginx 2>/dev/null || true
    useradd -r -g nginx -s /sbin/nologin nginx 2>/dev/null || true
    echo "✅ nginx用户和组创建成功"
fi

# 2. 备份原始配置
if [ -f /etc/nginx/nginx.conf ]; then
    cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup
elif [ -f /usr/local/nginx/conf/nginx.conf ]; then
    cp /usr/local/nginx/conf/nginx.conf /usr/local/nginx/conf/nginx.conf.backup
fi

# 3. 配置Nginx
# 确定Nginx配置目录
if [ -d "/etc/nginx/conf.d" ]; then
    NGINX_CONF_DIR="/etc/nginx/conf.d"
    NGINX_MAIN_CONF="/etc/nginx/nginx.conf"
elif [ -d "/usr/local/nginx/conf" ]; then
    NGINX_CONF_DIR="/usr/local/nginx/conf/conf.d"
    NGINX_MAIN_CONF="/usr/local/nginx/conf/nginx.conf"
    mkdir -p "$NGINX_CONF_DIR"
else
    echo "❌ 无法确定Nginx配置目录"
    exit 1
fi

cat > "$NGINX_CONF_DIR/itemplatform.conf" << EOF
server {
    listen 80;
    server_name localhost;  # 替换为你的域名或IP
    
    # 客户端上传文件大小限制
    client_max_body_size 50M;
    
    # API代理
    location /api/ {
        proxy_pass http://localhost:3000/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # CORS配置
        add_header 'Access-Control-Allow-Origin' '*';
        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS';
        add_header 'Access-Control-Allow-Headers' 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization';
        
        # 处理OPTIONS请求
        if (\$request_method = 'OPTIONS') {
            return 204;
        }
    }
    
    # 静态文件服务（商品图片等）
    location /static/ {
        alias /opt/itemplatform/static/;
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # 防盗链配置（可选）
        valid_referers none blocked server_names;
        if (\$invalid_referer) {
            return 403;
        }
    }
    
    # 健康检查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
    
    # 默认根路径
    location / {
        return 200 'ItemPlatform API Server is running';
        add_header Content-Type text/plain;
    }
}
EOF

# 4. 优化Nginx主配置
# 确定日志目录
if [ -d "/var/log/nginx" ]; then
    NGINX_ERROR_LOG="/var/log/nginx/error.log"
    NGINX_ACCESS_LOG="/var/log/nginx/access.log"
elif [ -d "/usr/local/nginx/logs" ]; then
    NGINX_ERROR_LOG="/usr/local/nginx/logs/error.log"
    NGINX_ACCESS_LOG="/usr/local/nginx/logs/access.log"
else
    mkdir -p /var/log/nginx
    NGINX_ERROR_LOG="/var/log/nginx/error.log"
    NGINX_ACCESS_LOG="/var/log/nginx/access.log"
fi

# 确定PID文件路径
if [ -f "/run/nginx.pid" ]; then
    NGINX_PID="/run/nginx.pid"
elif [ -d "/usr/local/nginx/logs" ]; then
    NGINX_PID="/usr/local/nginx/logs/nginx.pid"
else
    NGINX_PID="/run/nginx.pid"
fi

# 确定日志目录并创建
if [ ! -d "$(dirname "$NGINX_ERROR_LOG")" ]; then
    mkdir -p "$(dirname "$NGINX_ERROR_LOG")"
fi

if [ ! -d "$(dirname "$NGINX_ACCESS_LOG")" ]; then
    mkdir -p "$(dirname "$NGINX_ACCESS_LOG")"
fi

cat > "$NGINX_MAIN_CONF" << EOF
user nginx;
worker_processes auto;
error_log $NGINX_ERROR_LOG;
pid $NGINX_PID;

# Load dynamic modules. See /usr/share/doc/nginx/README.dynamic.
include /usr/share/nginx/modules/*.conf;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    log_format main '\$remote_addr - \$remote_user [\$time_local] "\$request" '
                  '\$status \$body_bytes_sent "\$http_referer" '
                  '"\$http_user_agent" "\$http_x_forwarded_for"';

    access_log $NGINX_ACCESS_LOG main;

    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Gzip配置
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        application/atom+xml
        application/javascript
        application/json
        application/ld+json
        application/manifest+json
        application/rss+xml
        application/vnd.geo+json
        application/vnd.ms-fontobject
        application/x-font-ttf
        application/x-web-app-manifest+json
        application/xhtml+xml
        application/xml
        font/opentype
        image/bmp
        image/svg+xml
        image/x-icon
        text/cache-manifest
        text/css
        text/plain
        text/vcard
        text/vnd.rim.location.xloc
        text/vtt
        text/x-component
        text/x-cross-domain-policy;

    # 安全头配置
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    include $NGINX_CONF_DIR/*.conf;
}
EOF

# 5. 测试配置
echo "正在测试Nginx配置..."
if command -v nginx >/dev/null 2>&1; then
    nginx -t
elif [ -f "/usr/local/nginx/sbin/nginx" ]; then
    /usr/local/nginx/sbin/nginx -t
else
    echo "❌ 无法找到Nginx可执行文件"
    exit 1
fi

# 6. 创建必要的目录并设置权限
echo "正在创建必要的目录..."
mkdir -p /opt/itemplatform/static
chown -R nginx:nginx /opt/itemplatform/static
chmod -R 755 /opt/itemplatform/static

# 7. 启动Nginx
echo "正在启动Nginx..."
if systemctl list-unit-files | grep -q nginx.service; then
    systemctl start nginx
    systemctl enable nginx
    echo "✅ 使用systemd启动Nginx成功"
elif [ -f "/usr/local/nginx/sbin/nginx" ]; then
    # 对于编译安装，手动启动
    /usr/local/nginx/sbin/nginx
    echo "✅ 编译安装的Nginx已启动"
else
    echo "❌ 无法找到Nginx启动方式"
    exit 1
fi

# 8. 配置防火墙
echo "正在配置防火墙..."
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --reload

# 9. 验证Nginx
echo "正在验证Nginx..."
sleep 3

# 检查Nginx是否运行
if command -v nginx >/dev/null 2>&1; then
    nginx_status=$(curl -s http://localhost/health 2>/dev/null || echo "")
elif [ -f "/usr/local/nginx/sbin/nginx" ]; then
    nginx_status=$(curl -s http://localhost/health 2>/dev/null || echo "")
else
    nginx_status=""
fi

if [ "$nginx_status" = "healthy" ]; then
    echo "✅ Nginx配置完成"
    echo "- 服务状态: 运行中"
    echo "- 监听端口: 80"
    echo "- API代理: /api/ -> localhost:3000"
    echo "- 静态文件: /static/ -> /opt/itemplatform/static/"
else
    echo "❌ Nginx验证失败"
    echo "正在检查错误日志..."
    if [ -f "$NGINX_ERROR_LOG" ]; then
        tail -n 10 "$NGINX_ERROR_LOG"
    fi
    exit 1
fi