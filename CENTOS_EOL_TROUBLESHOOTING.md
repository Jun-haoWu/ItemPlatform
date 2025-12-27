# CentOS 7 EOL 部署问题解决方案

## 问题描述
CentOS 7 已于 2020 年 12 月进入维护阶段，许多官方仓库已经不可用，导致 `yum update` 和软件安装失败。

## 错误现象
```
Could not retrieve mirrorlist http://mirrorlist.centos.org?arch=x86_64&release=7&repo=sclo-rh
error was 14: curl#6 - "Could not resolve host: mirrorlist.centos.org; Unknown error"

One of the configured repositories failed (Unknown),
and yum doesn't have enough cached data to continue.
```

## 解决方案

### 方案一：使用修复脚本（推荐）

1. **运行仓库修复工具**:
```bash
sudo bash ./fix_centos_repos.sh
```

2. **然后继续部署**:
```bash
sudo bash ./fix_and_continue_deploy.sh
```

3. **最后运行完整部署**:
```bash
sudo bash ./one_click_deploy.sh
```

### 方案二：手动修复仓库

1. **备份现有仓库配置**:
```bash
sudo mkdir -p /etc/yum.repos.d/backup
sudo cp /etc/yum.repos.d/*.repo /etc/yum.repos.d/backup/
```

2. **修复 CentOS-Base.repo**:
```bash
sudo tee /etc/yum.repos.d/CentOS-Base.repo << 'EOF'
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
```

3. **清理缓存并重新生成**:
```bash
sudo yum clean all
sudo yum makecache fast
```

4. **继续系统初始化**:
```bash
sudo bash ./setup_base.sh
```

### 方案三：跳过系统更新（不推荐）

如果仓库问题无法解决，可以修改 `setup_base.sh` 跳过系统更新：

```bash
# 注释掉或修改这一行
# yum update -y
yum update -y --skip-broken --nobest || echo "跳过系统更新"
```

## 替代方案

### 使用其他操作系统
考虑使用以下替代方案：

1. **AlmaLinux 8/9** - CentOS 的替代品
2. **Rocky Linux 8/9** - 另一个 CentOS 替代品  
3. **Ubuntu 20.04/22.04** - 更活跃的社区支持
4. **Debian 11/12** - 稳定的发行版

### 使用容器化部署
考虑使用 Docker 容器化部署：

1. **安装 Docker**:
```bash
curl -fsSL https://get.docker.com | sh
sudo systemctl start docker
sudo systemctl enable docker
```

2. **使用 Docker Compose 部署**:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: your_password
      MYSQL_DATABASE: item_platform
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
  
  app:
    image: node:16
    working_dir: /app
    volumes:
      - ./:/app
    ports:
      - "3000:3000"
    command: npm start
    depends_on:
      - mysql

volumes:
  mysql_data:
```

## 验证修复

修复后，验证系统是否正常工作：

```bash
# 检查仓库是否可用
sudo yum repolist

# 测试安装基础工具
sudo yum install -y curl wget

# 检查网络连接
ping -c 3 baidu.com

# 查看系统信息
cat /etc/redhat-release
uname -a
```

## 故障排除

### 如果仍然无法连接仓库

1. **检查 DNS 设置**:
```bash
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf
```

2. **检查网络配置**:
```bash
ip addr show
ping -c 3 8.8.8.8
```

3. **检查防火墙**:
```bash
sudo systemctl status firewalld
sudo iptables -L
```

### 如果某些包无法安装

1. **使用 --skip-broken 参数**:
```bash
sudo yum install -y package-name --skip-broken
```

2. **手动下载 RPM 包**:
```bash
# 从 vault.centos.org 手动下载
wget http://vault.centos.org/7.9.2009/os/x86_64/Packages/package-name.rpm
sudo rpm -ivh package-name.rpm
```

## 联系支持

如果以上方案都无法解决问题：

1. 检查阿里云官方文档
2. 联系阿里云技术支持
3. 考虑升级操作系统版本
4. 寻求社区帮助（Stack Overflow、Reddit等）

## 预防措施

1. **定期备份系统**:
```bash
sudo tar -czpf /backup/system-$(date +%Y%m%d).tar.gz /etc /home /opt
```

2. **监控系统状态**:
```bash
# 创建监控脚本
echo '#!/bin/bash
echo "$(date): System check" >> /var/log/system-check.log
df -h >> /var/log/system-check.log
free -h >> /var/log/system-check.log
systemctl status firewalld >> /var/log/system-check.log' > /usr/local/bin/system-check.sh
chmod +x /usr/local/bin/system-check.sh
```

3. **保持系统更新**（如果可能）:
```bash
# 设置自动更新（谨慎使用）
sudo yum install -y yum-cron
sudo systemctl enable yum-cron
sudo systemctl start yum-cron
```