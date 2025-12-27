#!/bin/bash
set -e

echo "=== 系统服务配置 ==="

# 1. 检测Node.js路径
echo "正在检测Node.js路径..."
if [ -f /usr/local/node/bin/node ]; then
    NODE_PATH="/usr/local/node/bin/node"
elif [ -f /usr/bin/node ]; then
    NODE_PATH="/usr/bin/node"
else
    NODE_PATH="$(which node)"
fi

echo "检测到Node.js路径: $NODE_PATH"

# 2. 创建系统服务文件
echo "正在创建系统服务文件..."
cat > /etc/systemd/system/itemplatform-api.service << EOF
[Unit]
Description=ItemPlatform API Service
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/itemplatform
ExecStart=$NODE_PATH server.js
ExecReload=/bin/kill -HUP \$MAINPID
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StartLimitInterval=60s
StartLimitBurst=3

# 环境变量
Environment=NODE_ENV=production
Environment=PORT=3000
Environment=DB_HOST=localhost
Environment=DB_USER=item_user
Environment=DB_PASSWORD=ItemPlatform2024!
Environment=DB_NAME=item_platform
Environment=JWT_SECRET=your-super-secret-jwt-key-change-this

# 日志配置
StandardOutput=append:/opt/itemplatform/logs/system.log
StandardError=append:/opt/itemplatform/logs/system.log
SyslogIdentifier=itemplatform-api

# 资源限制
LimitNOFILE=65536
LimitNPROC=32768

[Install]
WantedBy=multi-user.target
EOF

# 3. 创建日志文件
touch /opt/itemplatform/logs/system.log
chmod 644 /opt/itemplatform/logs/system.log

# 4. 重新加载系统服务
echo "正在重新加载系统服务..."
systemctl daemon-reload

# 5. 启用服务开机启动
echo "正在启用服务开机启动..."
systemctl enable itemplatform-api

# 6. 创建服务管理脚本
cat > /usr/local/bin/itemplatform-service << 'EOF'
#!/bin/bash
case "$1" in
    start)
        systemctl start itemplatform-api
        ;;
    stop)
        systemctl stop itemplatform-api
        ;;
    restart)
        systemctl restart itemplatform-api
        ;;
    status)
        systemctl status itemplatform-api
        ;;
    logs)
        tail -f /opt/itemplatform/logs/system.log
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs}"
        exit 1
        ;;
esac
EOF

chmod +x /usr/local/bin/itemplatform-service

echo "✅ 系统服务配置完成"
echo "- 服务名称: itemplatform-api"
echo "- 管理命令: itemplatform-service {start|stop|restart|status|logs}"
echo "- 日志文件: /opt/itemplatform/logs/system.log"
echo "- 配置文件: /etc/systemd/system/itemplatform-api.service"