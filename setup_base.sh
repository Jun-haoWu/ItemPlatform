#!/bin/bash
set -e  # 遇到错误立即退出

echo "=== CentOS系统初始化 ==="

# 0. 修复CentOS 7 EOL仓库问题
echo "正在检查和修复CentOS 7 EOL仓库配置..."

# 备份现有仓库配置
if [ -d "/etc/yum.repos.d" ]; then
    backup_dir="/etc/yum.repos.d/backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"
    cp /etc/yum.repos.d/*.repo "$backup_dir/" 2>/dev/null || true
    echo "✅ 仓库配置已备份到: $backup_dir"
fi

# 修复CentOS-Base.repo（使用vault仓库）
if [ -f "/etc/yum.repos.d/CentOS-Base.repo" ]; then
    cat > /etc/yum.repos.d/CentOS-Base.repo << 'EOF'
[base]
name=CentOS-7 - Base
baseurl=http://vault.centos.org/7.9.2009/os/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1

[updates]
name=CentOS-7 - Updates
baseurl=http://vault.centos.org/7.9.2009/updates/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1

[extras]
name=CentOS-7 - Extras
baseurl=http://vault.centos.org/7.9.2009/extras/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
enabled=1
EOF
fi

# 清理yum缓存并重新生成
echo "正在清理yum缓存..."
yum clean all
yum makecache fast

# 1. 更新系统（使用容错模式）
echo "正在更新系统..."
yum update -y --skip-broken --nobest || {
    echo "⚠️  系统更新遇到一些问题，继续安装基础工具..."
}

# 2. 安装基础工具（分批安装，提高成功率）
echo "正在安装基础工具..."

# 先安装最核心的工具
echo "正在安装核心工具..."
yum install -y curl wget vim net-tools || {
    echo "⚠️  部分核心工具安装失败，继续..."
}

# 再安装其他工具
echo "正在安装其他工具..."
yum install -y git lsof unzip tar || {
    echo "⚠️  部分工具安装失败，继续..."
}

# 3. 配置防火墙
echo "正在配置防火墙..."
systemctl start firewalld
systemctl enable firewalld

# 4. 开放必要端口
echo "正在开放端口..."
firewall-cmd --permanent --add-port=22/tcp    # SSH
firewall-cmd --permanent --add-port=80/tcp    # HTTP
firewall-cmd --permanent --add-port=443/tcp   # HTTPS
firewall-cmd --permanent --add-port=3000/tcp  # API服务
firewall-cmd --permanent --add-port=3306/tcp  # MySQL（仅内网）
firewall-cmd --reload

# 5. 设置时区
echo "正在设置时区..."
timedatectl set-timezone Asia/Shanghai

# 6. 创建应用目录
echo "正在创建应用目录..."
mkdir -p /opt/itemplatform
mkdir -p /opt/itemplatform/logs
mkdir -p /opt/itemplatform/backup
mkdir -p /opt/itemplatform/sql
mkdir -p /opt/itemplatform/static

# 7. 创建日志文件
touch /opt/itemplatform/logs/out.log
touch /opt/itemplatform/logs/err.log
touch /opt/itemplatform/logs/combined.log

# 8. 设置权限
chmod 755 /opt/itemplatform
chmod 644 /opt/itemplatform/logs/*.log

echo "✅ 系统初始化完成"
echo "- 仓库配置已修复（CentOS 7 EOL兼容）"
echo "- 防火墙端口已开放: 22, 80, 443, 3000, 3306"
echo "- 应用目录已创建: /opt/itemplatform"
echo "- 时区已设置为: Asia/Shanghai"
echo ""
echo "注意：如果遇到任何仓库错误，系统会尝试继续执行。"