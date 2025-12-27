#!/bin/bash
set -e

echo "=== 修复CentOS仓库并继续部署 ==="
echo ""

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo "❌ 错误: 请以root用户运行此脚本"
    echo "请使用: sudo ./fix_and_continue_deploy.sh"
    exit 1
fi

# 1. 首先修复仓库问题
echo "【步骤 1/2】修复CentOS仓库配置"
echo "=================================="
echo "正在运行仓库修复工具..."
bash ./fix_centos_repos.sh

if [ $? -eq 0 ]; then
    echo "✅ 仓库修复完成"
else
    echo "⚠️  仓库修复遇到问题，继续尝试部署..."
fi

# 2. 重新运行系统初始化
echo ""
echo "【步骤 2/2】重新运行系统初始化"
echo "=================================="
echo "正在运行修复后的系统初始化..."
bash ./setup_base.sh

if [ $? -eq 0 ]; then
    echo "✅ 系统初始化完成"
    echo ""
    echo "🎉 仓库问题已修复！"
    echo "现在可以继续运行一键部署脚本了："
    echo "  bash ./one_click_deploy.sh"
else
    echo "❌ 系统初始化仍然失败"
    echo "建议："
    echo "1. 手动检查网络连接"
    echo "2. 检查系统时间是否正确"
    echo "3. 考虑使用其他CentOS版本或替代方案"
    exit 1
fi