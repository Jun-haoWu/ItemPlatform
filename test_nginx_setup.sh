#!/bin/bash
set -e

echo "=== Nginx配置测试 ==="

# 测试nginx用户是否存在
echo "1. 检查nginx用户..."
if id -u nginx > /dev/null 2>&1; then
    echo "✅ nginx用户已存在"
else
    echo "❌ nginx用户不存在"
fi

# 测试nginx命令是否可用
echo "2. 检查nginx命令..."
if command -v nginx >/dev/null 2>&1; then
    echo "✅ nginx命令可用: $(which nginx)"
elif [ -f "/usr/local/nginx/sbin/nginx" ]; then
    echo "✅ 编译版nginx可用: /usr/local/nginx/sbin/nginx"
else
    echo "❌ nginx命令不可用"
fi

# 测试配置目录
echo "3. 检查配置目录..."
if [ -d "/etc/nginx/conf.d" ]; then
    echo "✅ 包安装配置目录: /etc/nginx/conf.d"
elif [ -d "/usr/local/nginx/conf" ]; then
    echo "✅ 编译安装配置目录: /usr/local/nginx/conf"
    if [ -d "/usr/local/nginx/conf/conf.d" ]; then
        echo "✅ 子配置目录存在"
    else
        echo "⚠️  子配置目录不存在，需要创建"
    fi
else
    echo "❌ 无法找到配置目录"
fi

# 测试日志目录
echo "4. 检查日志目录..."
if [ -d "/var/log/nginx" ]; then
    echo "✅ 包安装日志目录: /var/log/nginx"
elif [ -d "/usr/local/nginx/logs" ]; then
    echo "✅ 编译安装日志目录: /usr/local/nginx/logs"
else
    echo "⚠️  日志目录不存在，需要创建"
fi

# 测试静态文件目录
echo "5. 检查静态文件目录..."
if [ -d "/opt/itemplatform/static" ]; then
    echo "✅ 静态文件目录存在"
    if [ "$(stat -c %U /opt/itemplatform/static)" = "nginx" ]; then
        echo "✅ 目录权限正确（nginx用户）"
    else
        echo "⚠️  目录权限需要修复"
    fi
else
    echo "⚠️  静态文件目录不存在，需要创建"
fi

# 测试服务状态
echo "6. 检查Nginx服务状态..."
if systemctl list-unit-files | grep -q nginx.service; then
    if systemctl is-active --quiet nginx; then
        echo "✅ systemd管理的Nginx服务正在运行"
    else
        echo "⚠️  systemd管理的Nginx服务未运行"
    fi
elif pgrep nginx > /dev/null; then
    echo "✅ Nginx进程正在运行（手动启动）"
else
    echo "⚠️  Nginx未运行"
fi

# 测试端口监听
echo "7. 检查端口监听..."
if netstat -tlnp 2>/dev/null | grep -q ":80 "; then
    echo "✅ 端口80正在监听"
else
    echo "⚠️  端口80未监听"
fi

# 测试HTTP响应
echo "8. 测试HTTP响应..."
if curl -s http://localhost/health 2>/dev/null | grep -q "healthy"; then
    echo "✅ HTTP健康检查正常"
else
    echo "⚠️  HTTP健康检查失败"
fi

echo ""
echo "=== 测试完成 ==="
echo "如果所有检查都通过，说明Nginx配置正确。"
echo "如果有⚠️标记的项目，请运行setup_nginx.sh进行修复。"