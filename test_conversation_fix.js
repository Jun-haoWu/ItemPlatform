const axios = require('axios');

const API_BASE = 'http://223.6.254.237/api';

// 测试用户2登录后的会话列表
async function testUser2Conversations() {
  console.log('=== 测试用户2的会话列表 ===\n');
  
  try {
    // 用户2登录
    console.log('1. 用户2登录...');
    const loginResponse = await axios.post(`${API_BASE}/auth/login`, {
      username: '2',
      password: 'password123'
    });
    
    const token = loginResponse.data.data.token;
    console.log('用户2登录成功，Token:', token.substring(0, 50) + '...');
    
    // 获取会话列表
    console.log('\n2. 获取用户2的会话列表...');
    const conversationsResponse = await axios.get(`${API_BASE}/chat/conversations?page=1&limit=10`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const conversations = conversationsResponse.data.data.conversations;
    console.log(`获取到 ${conversations.length} 个会话:`);
    
    conversations.forEach((conv, index) => {
      console.log(`\n会话 ${index + 1}:`);
      console.log(`  - 会话ID: ${conv.conversation_id}`);
      console.log(`  - 对方用户名: ${conv.other_username}`);
      console.log(`  - 对方用户ID: ${conv.other_user_id}`);
      console.log(`  - 最后消息: ${conv.last_message}`);
      console.log(`  - 未读数量: ${conv.unread_count}`);
      console.log(`  - 最后发送者ID: ${conv.last_sender_id}`);
    });
    
    // 检查与用户4的会话
    const conversationWith4 = conversations.find(conv => conv.other_username === '4');
    if (conversationWith4) {
      console.log(`\n✓ 找到与用户4的会话:`);
      console.log(`  - 会话ID: ${conversationWith4.conversation_id}`);
      console.log(`  - 未读消息: ${conversationWith4.unread_count}`);
      console.log(`  - 最后消息: ${conversationWith4.last_message}`);
    } else {
      console.log('\n✗ 未找到与用户4的会话');
    }
    
  } catch (error) {
    console.error('测试失败:', error.response?.data || error.message);
  }
}

// 运行测试
testUser2Conversations();