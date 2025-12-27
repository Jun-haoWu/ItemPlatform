#!/bin/bash
set -e

echo "=== MySQL安装和配置 ==="

# 1. 卸载已存在的MySQL/MariaDBecho "正在清理现有数据库..."
yum remove -y mysql-community-server mariadb-server mysql-server 2>/dev/null || true
rm -rf /var/lib/mysql /etc/my.cnf* 2>/dev/null || true

# 2. 添加MySQL官方仓库
echo "正在添加MySQL仓库..."
# 检查是否已安装MySQL仓库
if rpm -qa | grep -q mysql80-community-release; then
    echo "MySQL仓库已安装，跳过安装步骤"
else
    rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-5.noarch.rpm
fi
yum-config-manager --disable mysql57-community 2>/dev/null || true
yum-config-manager --enable mysql80-community 2>/dev/null || true

# 3. 安装MySQL
echo "正在安装MySQL..."
# 尝试安装MySQL，如果失败则尝试MariaDB
if ! yum install -y mysql-community-server mysql-community-client; then
    echo "MySQL安装失败，尝试安装MariaDB..."
    yum install -y mariadb-server mariadb
fi

# 4. 启动MySQL服务
echo "正在启动MySQL服务..."
# 尝试启动MySQL服务，如果不存在则尝试MariaDB
if systemctl list-unit-files | grep -q mysqld.service; then
    systemctl start mysqld
    systemctl enable mysqld
    SERVICE_NAME="mysqld"
elif systemctl list-unit-files | grep -q mariadb.service; then
    systemctl start mariadb
    systemctl enable mariadb
    SERVICE_NAME="mariadb"
else
    echo "❌ 未找到MySQL或MariaDB服务"
    exit 1
fi

# 5. 等待MySQL启动完成
echo "等待MySQL启动..."
sleep 10

# 6. 获取临时密码
echo "正在获取临时密码..."
if [ "$SERVICE_NAME" = "mysqld" ]; then
    TEMP_PASSWORD=$(grep 'temporary password' /var/log/mysqld.log | tail -1 | awk '{print $NF}')
    if [ -z "$TEMP_PASSWORD" ]; then
        echo "⚠️  无法获取临时密码，尝试使用空密码..."
        TEMP_PASSWORD=""
    fi
else
    # MariaDB默认没有密码
    TEMP_PASSWORD=""
fi

# 7. 修改root密码并创建数据库
echo "正在配置数据库..."
mysql -u root -p"$TEMP_PASSWORD" --connect-expired-password << EOF
-- 修改root密码
ALTER USER 'root'@'localhost' IDENTIFIED BY 'ItemPlatform@2024!';

-- 创建数据库
CREATE DATABASE IF NOT EXISTS item_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建应用用户
CREATE USER IF NOT EXISTS 'item_user'@'localhost' IDENTIFIED BY 'ItemPlatform2024!';
GRANT ALL PRIVILEGES ON item_platform.* TO 'item_user'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;
EOF

# 8. 配置MySQL
echo "正在配置MySQL参数..."
# 根据服务类型选择配置文件路径
if [ "$SERVICE_NAME" = "mysqld" ]; then
    CONFIG_PATH="/etc/my.cnf.d/itemplatform.cnf"
else
    CONFIG_PATH="/etc/my.cnf.d/mariadb-server.cnf"
fi
cat > "$CONFIG_PATH" << EOF
[mysqld]
# 基础配置
default-storage-engine=INNODB
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

# 性能优化
max_connections=200
innodb_buffer_pool_size=256M
query_cache_size=64M
query_cache_limit=2M

# 日志配置
slow_query_log=1
slow_query_log_file=/var/log/mysql_slow.log
long_query_time=2

# 安全配置
skip-symbolic-links=1
max_connect_errors=100
bind-address=127.0.0.1
EOF

# 9. 重启MySQL应用配置
echo "正在重启MySQL..."
systemctl restart $SERVICE_NAME

# 10. 验证安装
echo "正在验证MySQL安装..."
# 使用正确的服务名称进行验证
if [ "$SERVICE_NAME" = "mysqld" ]; then
    mysql_cmd="mysql"
else
    mysql_cmd="mysql"
fi

$mysql_cmd -u item_user -pItemPlatform2024! item_platform -e "SELECT 1;" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ MySQL安装和配置完成"
    echo "- 数据库: item_platform"
    echo "- 用户名: item_user"
    echo "- 密码: ItemPlatform2024!"
    if [ "$SERVICE_NAME" = "mysqld" ]; then
        echo "- root密码: ItemPlatform@2024!"
    fi
else
    echo "❌ MySQL配置验证失败"
    exit 1
fi