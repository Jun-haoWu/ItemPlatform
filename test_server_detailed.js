// 详细的Docker环境测试脚本 - 使用Node.js
const http = require('http');

// 测试配置
const AUTH_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MjUsInVzZXJuYW1lIjoiNCIsImlhdCI6MTc2NzA3MzY5NywiZXhwIjoxNzY3Njc4NDk3fQ.wdcWLrdsSQ2rGlJlpi-7m9m9arcb2P1E9PcQ2CnP7ufac';

function makeRequest(path, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 3000,
      path: '/api' + path,
      method: method,
      headers: {
        'Authorization': `Bearer ${AUTH_TOKEN}`,
        'Content-Type': 'application/json'
      }
    };
    
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          resolve({ status: res.statusCode, data: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, data: body });
        }
      });
    });
    
    req.on('error', reject);
    
    if (data) {
      req.write(JSON.stringify(data));
    }
    
    req.end();
  });
}

async function runDetailedTests() {
  console.log('=== 详细Docker环境聊天功能测试 ===\n');
  
  try {
    // 1. 获取用户列表（详细）
    console.log('1. 获取用户列表（详细）...');
    const users = await makeRequest('/users?page=1&limit=20');
    console.log('状态:', users.status);
    if (users.status === 200 && users.data.users) {
      console.log('总用户数:', users.data.total);
      console.log('当前页用户数:', users.data.users.length);
      users.data.users.forEach(user => {
        console.log(`  - ID: ${user.id}, 用户名: ${user.username}, 姓名: ${user.real_name || '未设置'}`);
      });
    } else {
      console.log('响应数据:', JSON.stringify(users.data, null, 2));
    }
    
    // 2. 检查未读消息
    console.log('\n2. 检查未读消息...');
    const unread = await makeRequest('/chat/unread-count');
    console.log('未读消息响应:', JSON.stringify(unread.data, null, 2));
    
    // 3. 发送测试消息给用户2（ID: 16）
    console.log('\n3. 发送测试消息给用户2（ID: 16）...');
    const messageData = {
      receiver_id: 16,
      message: `测试消息：验证消息传递功能 - ${new Date().toLocaleString()}`
    };
    const sendResult = await makeRequest('/chat/send', 'POST', messageData);
    console.log('发送结果:', JSON.stringify(sendResult.data, null, 2));
    
    // 4. 获取与用户2的聊天历史
    console.log('\n4. 获取与用户2的聊天历史...');
    const history = await makeRequest('/chat/history/16?page=1&limit=10');
    console.log('聊天历史响应:', JSON.stringify(history.data, null, 2));
    
    // 5. 获取所有聊天会话
    console.log('\n5. 获取所有聊天会话...');
    const conversations = await makeRequest('/chat/conversations?page=1&limit=10');
    console.log('聊天会话响应:', JSON.stringify(conversations.data, null, 2));
    
    console.log('\n=== 测试结果分析 ===');
    if (users.status === 200 && users.data.users && users.data.users.length > 0) {
      console.log('✅ 用户列表获取正常');
    } else {
      console.log('❌ 用户列表获取异常');
    }
    
    if (sendResult.status === 200) {
      console.log('✅ 消息发送功能正常');
    } else {
      console.log('❌ 消息发送功能异常');
    }
    
    if (history.status === 200 && history.data.messages && history.data.messages.length > 0) {
      console.log('✅ 聊天历史获取正常');
      console.log(`📱 找到 ${history.data.messages.length} 条消息`);
    } else {
      console.log('❌ 聊天历史获取异常或为空');
    }
    
  } catch (error) {
    console.error('测试失败:', error.message);
  }
}

// 运行详细测试
runDetailedTests();