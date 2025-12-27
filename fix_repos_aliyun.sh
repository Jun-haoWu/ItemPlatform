#!/bin/bash
set -e

echo "=== 使用国内镜像源修复CentOS 7仓库 ==="

# 备份现有仓库配置
backup_dir="/etc/yum.repos.d/backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$backup_dir"
cp /etc/yum.repos.d/*.repo "$backup_dir/" 2>/dev/null || true
echo "✅ 仓库配置已备份到: $backup_dir"

# 方案1: 使用阿里云镜像源
echo "正在配置阿里云镜像源..."
cat > /etc/yum.repos.d/CentOS-Base.repo << 'EOF'
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

# 配置EPEL使用阿里云镜像
echo "正在配置EPEL镜像源..."
cat > /etc/yum.repos.d/epel.repo << 'EOF'
[epel]
name=Extra Packages for Enterprise Linux 7 - $basearch - Aliyun
baseurl=http://mirrors.aliyun.com/epel/7/$basearch
failovermethod=priority
enabled=1
gpgcheck=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7
EOF

# 禁用有问题的仓库
echo "正在禁用有问题的仓库..."
yum-config-manager --disable centos-sclo-rh 2>/dev/null || true
yum-config-manager --disable centos-sclo-sclo 2>/dev/null || true

# 清理缓存
echo "正在清理yum缓存..."
yum clean all
yum makecache fast

echo "✅ 国内镜像源配置完成"
echo ""
echo "现在可以运行: yum update -y"