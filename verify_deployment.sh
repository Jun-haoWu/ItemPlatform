#!/bin/bash
set -e

echo "=== 部署验证脚本 ==="
echo "======================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_service() {
    local service_name=$1
    local service_type=$2
    
    echo -n "检查 $service_name 状态... "
    
    case $service_type in
        "systemd")
            if systemctl is-active --quiet $service_name; then
                echo -e "${GREEN}✓ 运行中${NC}"
                return 0
            else
                echo -e "${RED}✗ 未运行${NC}"
                return 1
            fi
            ;;
        "pm2")
            if pm2 list | grep -q $service_name; then
                echo -e "${GREEN}✓ 运行中${NC}"
                return 0
            else
                echo -e "${RED}✗ 未运行${NC}"
                return 1
            fi
            ;;
        "port")
            if netstat -tlnp 2>/dev/null | grep -q ":$service_name "; then
                echo -e "${GREEN}✓ 端口监听中${NC}"
                return 0
            else
                echo -e "${RED}✗ 端口未监听${NC}"
                return 1
            fi
            ;;
        "process")
            if pgrep -f "$service_name" > /dev/null; then
                echo -e "${GREEN}✓ 进程运行中${NC}"
                return 0
            else
                echo -e "${RED}✗ 进程未运行${NC}"
                return 1
            fi
            ;;
    esac
}

check_url() {
    local url=$1
    local expected_status=$2
    
    echo -n "检查 $url ... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)
    
    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}✓ HTTP $response${NC}"
        return 0
    else
        echo -e "${RED}✗ HTTP $response (期望: $expected_status)${NC}"
        return 1
    fi
}

check_database() {
    echo -n "检查数据库连接... "
    
    if mysql -u item_user -pItemPlatform2024! item_platform -e "SELECT 1;" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 连接正常${NC}"
        return 0
    else
        echo -e "${RED}✗ 连接失败${NC}"
        return 1
    fi
}

# 统计变量
total_checks=0
passed_checks=0
failed_checks=0

# 执行检查函数
run_check() {
    ((total_checks++))
    if "$@"; then
        ((passed_checks++))
    else
        ((failed_checks++))
    fi
}

# 开始检查
echo "开始部署验证..."
echo ""

# 1. 系统服务检查
echo "【系统服务】"
echo "-------------"
run_check check_service "mysqld" "systemd"
run_check check_service "nginx" "systemd"
run_check check_service "itemplatform-api" "systemd"
echo ""

# 2. 端口检查
echo "【端口监听】"
echo "-------------"
run_check check_service "22" "port"     # SSH
run_check check_service "80" "port"     # HTTP
run_check check_service "3000" "port"   # API
run_check check_service "3306" "port"   # MySQL
echo ""

# 3. 进程检查
echo "【进程状态】"
echo "-------------"
run_check check_service "mysqld" "process"
run_check check_service "nginx" "process"
run_check check_service "node.*server.js" "process"
echo ""

# 4. 数据库检查
echo "【数据库】"
echo "-----------"
run_check check_database
echo ""

# 5. API接口检查
echo "【API接口】"
echo "-------------"
# 获取服务器IP
server_ip=$(curl -s ifconfig.me 2>/dev/null || echo "localhost")
base_url="http://$server_ip"

run_check check_url "$base_url/health" "200"
run_check check_url "$base_url/api/health" "200"
echo ""

# 6. 文件和目录检查
echo "【文件和目录】"
echo "---------------"
important_paths=(
    "/opt/itemplatform"
    "/opt/itemplatform/logs"
    "/opt/itemplatform/server.js"
    "/var/log/nginx"
    "/var/lib/mysql"
)

for path in "${important_paths[@]}"; do
    echo -n "检查 $path ... "
    if [ -e "$path" ]; then
        echo -e "${GREEN}✓ 存在${NC}"
        ((passed_checks++))
    else
        echo -e "${RED}✗ 不存在${NC}"
        ((failed_checks++))
    fi
    ((total_checks++))
done
echo ""

# 7. 日志检查
echo "【日志检查】"
echo "-------------"
echo -n "检查最近错误日志... "

error_count=0
for log_file in "/opt/itemplatform/logs/err.log" "/var/log/nginx/error.log" "/var/log/mysqld.log"; do
    if [ -f "$log_file" ]; then
        recent_errors=$(tail -n 100 "$log_file" 2>/dev/null | grep -i "error\|fatal" | wc -l)
        if [ "$recent_errors" -gt 0 ]; then
            echo -e "${YELLOW}⚠️  $log_file 有 $recent_errors 个错误${NC}"
            ((error_count++))
        fi
    fi
done

if [ "$error_count" -eq 0 ]; then
    echo -e "${GREEN}✓ 无最近错误${NC}"
else
    echo -e "${YELLOW}⚠️  发现 $error_count 个日志文件有错误${NC}"
fi
echo ""

# 显示统计结果
echo "======================================"
echo "验证完成！"
echo "======================================"
echo -e "总检查项: $total_checks"
echo -e "通过: ${GREEN}$passed_checks${NC}"
echo -e "失败: ${RED}$failed_checks${NC}"

if [ $failed_checks -eq 0 ]; then
    echo -e "\n${GREEN}🎉 所有检查均通过，部署成功！${NC}"
    echo ""
    echo "服务访问信息:"
    echo "- 服务器IP: $server_ip"
    echo "- API地址: $base_url/api/"
    echo "- 健康检查: $base_url/health"
    echo ""
    echo "管理命令:"
    echo "- 服务管理: systemctl {start|stop|restart} itemplatform-api"
    echo "- 查看日志: tail -f /opt/itemplatform/logs/system.log"
    echo "- 数据库: mysql -u item_user -pItemPlatform2024! item_platform"
    
elif [ $failed_checks -lt 3 ]; then
    echo -e "\n${YELLOW}⚠️  部署部分成功，有 $failed_checks 个问题需要修复${NC}"
    echo "建议查看相关日志文件了解详细信息"
else
    echo -e "\n${RED}❌ 部署存在较多问题，建议重新检查配置${NC}"
    echo "请查看日志文件:"
    echo "- 应用日志: /opt/itemplatform/logs/system.log"
    echo "- Nginx日志: /var/log/nginx/error.log"
    echo "- MySQL日志: /var/log/mysqld.log"
fi

echo ""
echo "验证脚本执行完毕"