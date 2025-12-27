#!/bin/bash
set -e

echo "=== ItemPlatform 一键部署脚本 ==="
echo "======================================"
echo ""

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then 
    echo "❌ 错误: 请以root用户运行此脚本"
    echo "请使用: sudo ./one_click_deploy.sh"
    exit 1
fi

# 检查网络连接
echo "正在检查网络连接..."
ping -c 1 baidu.com > /dev/null 2>&1 || {
    echo "❌ 错误: 无法连接互联网，请检查网络配置"
    exit 1
}

# 记录开始时间
start_time=$(date +%s)

# 定义部署步骤
steps=(
    "系统初始化:setup_base.sh"
    "MySQL安装:mysql_setup.sh"
    "Node.js环境:nodejs_setup.sh"
    "数据库初始化:init_database.sh"
    "API部署:deploy_api.sh"
    "Nginx配置:setup_nginx.sh"
    "系统服务:setup_systemd.sh"
)

total_steps=${#steps[@]}
failed_steps=0

# 创建日志文件
LOG_FILE="/opt/itemplatform/logs/deploy_$(date +%Y%m%d_%H%M%S).log"
mkdir -p /opt/itemplatform/logs
touch "$LOG_FILE"

# 日志函数
log_info() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [INFO] $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [ERROR] $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [SUCCESS] $1" | tee -a "$LOG_FILE"
}

# 执行步骤函数
execute_step() {
    local step_num=$1
    local step_info=$2
    local step_name=$(echo $step_info | cut -d':' -f1)
    local script_name=$(echo $step_info | cut -d':' -f2)
    
    echo ""
    echo "【步骤 $step_num/$total_steps】$step_name"
    echo "=================================="
    log_info "开始执行: $step_name"
    
    if [ -f "./$script_name" ]; then
        # 执行脚本
        bash "./$script_name" >> "$LOG_FILE" 2>&1
        if [ $? -eq 0 ]; then
            log_success "$step_name 执行成功"
            echo "✅ $step_name 完成"
        else
            log_error "$step_name 执行失败"
            echo "❌ $step_name 失败"
            ((failed_steps++))
            return 1
        fi
    else
        log_error "脚本 $script_name 不存在"
        echo "⚠️  脚本 $script_name 不存在，跳过此步骤"
        ((failed_steps++))
        return 1
    fi
    
    return 0
}

# 显示系统信息
echo "系统信息:"
echo "- 操作系统: $(cat /etc/redhat-release)"
echo "- 内核版本: $(uname -r)"
echo "- CPU核心: $(nproc)"
echo "- 内存大小: $(free -h | grep '^Mem:' | awk '{print $2}')"
echo "- 磁盘空间: $(df -h / | tail -1 | awk '{print $4}') 可用"
echo ""

# 开始部署
log_info "开始ItemPlatform一键部署"
echo "🚀 开始部署 ItemPlatform..."
echo "日志文件: $LOG_FILE"
echo ""

current_step=0
for step in "${steps[@]}"; do
    ((current_step++))
    execute_step $current_step "$step"
    if [ $? -ne 0 ] && [ $failed_steps -ge 2 ]; then
        echo ""
        echo "❌ 部署失败，错误步骤过多，终止部署"
        log_error "部署终止，失败步骤: $failed_steps"
        break
    fi
done

# 记录结束时间
end_time=$(date +%s)
duration=$((end_time - start_time))

# 显示部署结果
echo ""
echo "======================================"
echo "部署完成！"
echo "======================================"
echo "总耗时: $duration 秒"
echo "失败步骤: $failed_steps"
echo "日志文件: $LOG_FILE"
echo ""

if [ $failed_steps -eq 0 ]; then
    echo "🎉 部署成功！所有步骤均已完成"
    log_success "部署成功完成"
    
    # 显示服务信息
    echo ""
    echo "服务信息:"
    echo "- API地址: http://$(curl -s ifconfig.me)/api/"
    echo "- 数据库: mysql -u item_user -pItemPlatform2024! item_platform"
    echo "- 日志文件: /opt/itemplatform/logs/"
    echo "- 服务管理: itemplatform-service {start|stop|restart|status|logs}"
    echo ""
    echo "请确保在Android客户端更新API地址:"
    echo "BASE_URL = \"http://$(curl -s ifconfig.me)/api/\""
    echo ""
    echo "验证部署:"
    echo "curl http://$(curl -s ifconfig.me)/api/health"
    
elif [ $failed_steps -lt 3 ]; then
    echo "⚠️  部署部分成功，有 $failed_steps 个步骤失败"
    echo "请查看日志文件了解详细信息，并手动修复失败的步骤"
    log_error "部署部分成功，失败步骤: $failed_steps"
else
    echo "❌ 部署失败，请查看日志文件了解详细信息"
    echo "建议:"
    echo "1. 检查网络连接"
    echo "2. 确保有足够的磁盘空间"
    echo "3. 查看日志文件: tail -f $LOG_FILE"
    log_error "部署失败"
fi

echo ""
echo "部署脚本执行完毕"