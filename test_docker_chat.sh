#!/bin/bash
# Docker环境下的聊天功能测试脚本

echo "=== Docker环境聊天功能测试 ==="
echo ""

# 1. 检查服务器健康状态
echo "1. 检查服务器健康状态..."
curl -s -X GET http://localhost:3000/health | jq .

# 2. 测试用户列表接口（检查速率限制）
echo ""
echo "2. 测试用户列表接口..."
for i in {1..3}; do
    echo "第$i次请求:"
    curl -s -X GET "http://localhost:3000/api/users?page=1&limit=5" \
         -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9arcb2P1E9PcQ2CnP7ufac" | jq .
    sleep 1
done

# 3. 测试未读消息计数
echo ""
echo "3. 测试未读消息计数..."
curl -s -X GET "http://localhost:3000/api/chat/unread-count" \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9arcb2P1E9PcQ2CnP7ufac" | jq .

# 4. 测试发送消息
echo ""
echo "4. 测试发送消息给用户2..."
curl -s -X POST "http://localhost:3000/api/chat/send" \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9arcb2P1E9PcQ2CnP7ufac" \
     -H "Content-Type: application/json" \
     -d '{
       "receiver_id": 16,
       "message": "测试消息：验证is_read=0功能 - '"$(date)"'"
     }' | jq .

# 5. 测试聊天历史
echo ""
echo "5. 测试用户2的聊天历史..."
curl -s -X GET "http://localhost:3000/api/chat/history/16?page=1&limit=5" \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9arcb2P1E9PcQ2CnP7ufac" | jq .

echo ""
echo "=== 测试完成 ==="
echo "如果以上测试都成功，说明消息功能已修复！"
echo "用户2现在应该能看到用户4发送的消息（is_read=0）"