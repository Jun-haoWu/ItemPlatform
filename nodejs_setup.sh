#!/bin/bash
set -e

echo "=== Node.js环境安装 ==="

# 1. 安装Node.js（CentOS 7兼容版本）
echo "正在安装Node.js..."

# 首先清理可能存在的旧安装
echo "正在清理旧版本Node.js..."
rm -rf /usr/local/node
rm -f /usr/bin/node /usr/bin/npm /usr/bin/npx

# 方法1: 尝试使用EPEL仓库（最简单）
echo "尝试使用EPEL仓库安装Node.js..."
yum install -y epel-release
if yum install -y nodejs npm; then
    echo "✅ 使用EPEL仓库安装成功"
else
    echo "EPEL仓库安装失败，尝试使用官方二进制包..."
    
    # 方法2: 使用官方二进制包
    cd /tmp
    echo "正在下载Node.js 16.x二进制包..."
    rm -f node-v16.20.2-linux-x64.tar.xz
    if wget -q https://nodejs.org/dist/v16.20.2/node-v16.20.2-linux-x64.tar.xz; then
        echo "正在解压和安装Node.js..."
        rm -rf /usr/local/node
        tar -xf node-v16.20.2-linux-x64.tar.xz
        mv node-v16.20.2-linux-x64 /usr/local/node
        ln -sf /usr/local/node/bin/node /usr/bin/node
        ln -sf /usr/local/node/bin/npm /usr/bin/npm
        ln -sf /usr/local/node/bin/npx /usr/bin/npx
        export PATH=$PATH:/usr/local/node/bin
        echo 'export PATH=$PATH:/usr/local/node/bin' >> /etc/profile
        rm -f node-v16.20.2-linux-x64.tar.xz
        echo "✅ 使用二进制包安装成功"
    else
        echo "二进制包下载失败，尝试使用NodeSource仓库..."
        
        # 方法3: 使用NodeSource仓库（最后尝试）
        curl -fsSL https://rpm.nodesource.com/setup_16.x | bash -
        yum install -y nodejs
        echo "✅ 使用NodeSource仓库安装成功"
    fi
fi

# 2. 验证安装
echo "正在验证Node.js安装..."
node_version=$(node -v)
npm_version=$(npm -v)
echo "Node.js版本: $node_version"
echo "npm版本: $npm_version"

# 3. 安装PM2进程管理器
echo "正在安装PM2..."
# 使用淘宝镜像源，提高安装成功率
npm config set registry https://registry.npmmirror.com
npm install -g pm2 --unsafe-perm

# 4. 安装常用工具包
echo "正在安装开发工具..."
npm install -g nodemon --unsafe-perm || echo "nodemon安装失败，跳过"
npm install -g typescript --unsafe-perm || echo "typescript安装失败，跳过"
npm install -g ts-node --unsafe-perm || echo "ts-node安装失败，跳过"

# 5. 配置PM2开机启动
echo "正在配置PM2..."
# 创建简单的systemd服务文件
cat > /etc/systemd/system/pm2-root.service << 'EOF'
[Unit]
Description=PM2 process manager
Documentation=https://pm2.keymetrics.io/
After=network.target

[Service]
Type=forking
User=root
LimitNOFILE=infinity
LimitNPROC=infinity
LimitCORE=infinity
TimeoutStartSec=8
TimeoutStopSec=8
Restart=always
RestartSec=3
KillMode=mixed
Environment=PATH=/usr/local/node/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin
ExecStart=/usr/local/node/bin/pm2 resurrect
ExecReload=/usr/local/node/bin/pm2 reload all
ExecStop=/usr/local/node/bin/pm2 kill

[Install]
WantedBy=multi-user.target
EOF

# 重新加载systemd配置
systemctl daemon-reload
systemctl enable pm2-root.service

# 6. 验证PM2安装
pm2 -v || echo "PM2安装验证失败"

# 7. 创建PM2配置文件目录
mkdir -p /root/.pm2
chmod 700 /root/.pm2

echo "✅ Node.js环境安装完成"
echo "- Node.js版本: $node_version"
echo "- npm版本: $npm_version"
echo "- PM2已配置开机启动"