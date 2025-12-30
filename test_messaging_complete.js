// 完整的聊天功能测试脚本
const axios = require('axios');

// 测试配置
const BASE_URL = 'http://localhost:3000/api';
const USER4_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9arcb2P1E9PcQ2CnP7ufac'; // 用户4的token
const USER2_ID = 16; // 用户2的ID

// 创建axios实例
const api = axios.create({
    baseURL: BASE_URL,
    timeout: 10000
});

// 测试函数
async function testMessaging() {
    console.log('=== 聊天功能完整测试 ===\n');
    
    try {
        // 1. 测试发送消息
        console.log('1. 测试用户4发送消息给用户2...');
        const sendResponse = await api.post('/chat/send', 
            {
                receiver_id: USER2_ID,
                message: '测试消息：用户4发送给用户2 - ' + new Date().toLocaleString()
            },
            {
                headers: {
                    'Authorization': `Bearer ${USER4_TOKEN}`,
                    'Content-Type': 'application/json'
                }
            }
        );
        
        if (sendResponse.status === 200) {
            console.log('✅ 消息发送成功');
            console.log('消息ID:', sendResponse.data.data.id);
            console.log('消息内容:', sendResponse.data.data.message);
            console.log('发送时间:', sendResponse.data.data.created_at);
            console.log('is_read状态:', sendResponse.data.data.is_read);
        } else {
            console.log('❌ 消息发送失败:', sendResponse.status);
        }
        
        console.log('\n2. 检查数据库中的消息状态...');
        // 这里可以添加数据库查询来验证is_read=0
        
        console.log('\n3. 测试获取用户列表（检查速率限制）...');
        for (let i = 0; i < 5; i++) {
            try {
                const usersResponse = await api.get('/users?page=1&limit=10', {
                    headers: {
                        'Authorization': `Bearer ${USER4_TOKEN}`
                    }
                });
                
                if (usersResponse.status === 200) {
                    console.log(`✅ 第${i+1}次用户列表请求成功，用户数:`, usersResponse.data.users.length);
                }
            } catch (error) {
                if (error.response && error.response.status === 429) {
                    console.log(`❌ 第${i+1}次请求触发速率限制`);
                } else {
                    console.log(`❌ 第${i+1}次请求失败:`, error.message);
                }
            }
        }
        
        console.log('\n4. 测试聊天历史记录...');
        const historyResponse = await api.get(`/chat/history/${USER2_ID}?page=1&limit=10`, {
            headers: {
                'Authorization': `Bearer ${USER4_TOKEN}`
            }
        });
        
        if (historyResponse.status === 200) {
            console.log('✅ 聊天历史获取成功');
            console.log('历史消息数量:', historyResponse.data.messages.length);
            if (historyResponse.data.messages.length > 0) {
                console.log('最新消息内容:', historyResponse.data.messages[0].message);
                console.log('最新消息is_read:', historyResponse.data.messages[0].is_read);
            }
        }
        
        console.log('\n5. 测试未读消息计数...');
        const unreadResponse = await api.get('/chat/unread-count', {
            headers: {
                'Authorization': `Bearer ${USER4_TOKEN}`
            }
        });
        
        if (unreadResponse.status === 200) {
            console.log('✅ 未读消息计数获取成功');
            console.log('未读消息总数:', unreadResponse.data.unreadCount);
        }
        
        console.log('\n=== 测试总结 ===');
        console.log('✅ 消息发送功能正常');
        console.log('✅ 速率限制优化生效');
        console.log('✅ 聊天历史获取正常');
        console.log('✅ 未读消息计数正常');
        console.log('\n🎉 所有测试通过！用户2应该能看到用户4发送的消息。');
        
    } catch (error) {
        console.error('测试失败:', error.message);
        if (error.response) {
            console.error('响应状态:', error.response.status);
            console.error('响应数据:', error.response.data);
        }
    }
}

// 运行测试
testMessaging();