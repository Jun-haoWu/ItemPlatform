#!/bin/bash
set -e

echo "=== CentOS 7 EOL 仓库修复工具 ==="

# 备份现有仓库配置
backup_dir="/etc/yum.repos.d/backup_$(date +%Y%m%d_%H%M%S)"
if [ -d "/etc/yum.repos.d" ]; then
    mkdir -p "$backup_dir"
    cp /etc/yum.repos.d/*.repo "$backup_dir/" 2>/dev/null || true
    echo "✅ 仓库配置已备份到: $backup_dir"
fi

# 修复CentOS-Base.repo
echo "正在修复CentOS基础仓库..."
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

# 修复或禁用SCL仓库
echo "正在修复SCL仓库..."
if [ -f "/etc/yum.repos.d/CentOS-SCLo.repo" ]; then
    cat > /etc/yum.repos.d/CentOS-SCLo.repo << 'EOF'
[centos-sclo-rh]
name=CentOS-7 - SCLo rh
baseurl=http://vault.centos.org/7.9.2009/sclo/$basearch/rh/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-SIG-SCLo
enabled=1

[centos-sclo-sclo]
name=CentOS-7 - SCLo sclo
baseurl=http://vault.centos.org/7.9.2009/sclo/$basearch/sclo/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-SIG-SCLo
enabled=1
EOF
fi

# 修复EPEL仓库（如果存在）
if [ -f "/etc/yum.repos.d/epel.repo" ]; then
    echo "正在修复EPEL仓库..."
    cat > /etc/yum.repos.d/epel.repo << 'EOF'
[epel]
name=Extra Packages for Enterprise Linux 7 - $basearch
baseurl=http://archive.fedoraproject.org/pub/archive/epel/7/$basearch
failovermethod=priority
enabled=1
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7

[epel-debuginfo]
name=Extra Packages for Enterprise Linux 7 - $basearch - Debug
baseurl=http://archive.fedoraproject.org/pub/archive/epel/7/$basearch/debug
failovermethod=priority
enabled=0
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7

[epel-source]
name=Extra Packages for Enterprise Linux 7 - $basearch - Source
baseurl=http://archive.fedoraproject.org/pub/archive/epel/7/SRPMS
failovermethod=priority
enabled=0
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-EPEL-7
EOF
fi

# 清理并重建缓存
echo "正在清理yum缓存..."
yum clean all
yum makecache fast

echo "✅ 仓库修复完成"
echo ""
echo "现在可以重新运行系统初始化脚本了。"
echo "如果仍然有问题，可以尝试手动安装："
echo "  yum install -y curl wget vim net-tools --skip-broken"
echo ""
echo "备份文件保存在: $backup_dir"