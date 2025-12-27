#!/bin/bash

echo "开始修复MySQL安装问题..."

# 1. 导入正确的GPG密钥
echo "导入MySQL GPG密钥..."
sudo rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2022
sudo rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql

# 2. 重新安装MySQL仓库
echo "重新安装MySQL仓库..."
sudo yum remove -y mysql80-community-release
wget https://dev.mysql.com/get/mysql80-community-release-el7-5.noarch.rpm
sudo rpm -ivh mysql80-community-release-el7-5.noarch.rpm --force

# 3. 清除yum缓存
echo "清除yum缓存..."
sudo yum clean all
sudo yum makecache

# 4. 禁用GPG检查进行安装
echo "安装MySQL（禁用GPG检查）..."
sudo yum install -y mysql-community-server --nogpgcheck

# 5. 检查安装结果
if [ $? -eq 0 ]; then
    echo "MySQL安装成功！"
    
    # 启动服务
    echo "启动MySQL服务..."
    sudo systemctl start mysqld
    sudo systemctl enable mysqld
    
    # 检查服务状态
    sudo systemctl status mysqld
    
    if [ $? -eq 0 ]; then
        echo "MySQL服务启动成功！"
        
        # 获取临时密码
        echo "获取临时密码..."
        temp_password=$(sudo grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
        echo "临时密码: $temp_password"
        
        # 安全配置
        echo "请运行以下命令进行安全配置："
        echo "mysql_secure_installation"
        echo ""
        echo "或者手动设置密码："
        echo "mysql -u root -p'\$temp_password'"
        echo "ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新密码';"
        
    else
        echo "MySQL服务启动失败，请检查日志："
        echo "sudo journalctl -xe"
        echo "sudo tail -f /var/log/mysqld.log"
    fi
else
    echo "MySQL安装失败，尝试使用MariaDB..."
    echo "执行MariaDB安装方案..."
    sudo yum install -y mariadb-server mariadb
    sudo systemctl start mariadb
    sudo systemctl enable mariadb
    sudo mysql_secure_installation
fi

echo "安装完成！"