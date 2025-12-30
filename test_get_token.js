// 获取新token并测试消息功能的脚本
const http = require('http');
const querystring = require('querystring');

// 测试配置
const USERNAME = '4';  // 用户4
const PASSWORD = '123456';  // 假设密码

let authToken = '';

function makeRequest(path, method = 'GET', data = null, token = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 3000,
      path: '/api' + path,
      method: method,
      headers: {
        'Content-Type': 'application/json'
      }
    };
    
    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }
    
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

async function getNewToken() {
  console.log('正在获取新的认证token...');
  
  // 尝试使用通用密码
  const loginData = {
    username: USERNAME,
    password: 'password123'  // 通用密码
  };
  
  const result = await makeRequest('/auth/login', 'POST', loginData);
  
  if (result.status === 200 && result.data.data && result.data.data.token) {
    console.log('✅ 使用通用密码登录成功');
    return result.data.data.token;
  }
  
  // 如果通用密码失败，尝试用户密码
  const loginData2 = {
    username: USERNAME,
    password: PASSWORD
  };
  
  const result2 = await makeRequest('/auth/login', 'POST', loginData2);
  
  if (result2.status === 200 && result2.data.data && result2.data.data.token) {
    console.log('✅ 使用用户密码登录成功');
    return result2.data.data.token;
  }
  
  console.log('❌ 登录失败，响应:', JSON.stringify(result2.data, null, 2));
  return null;
}

async function runCompleteTest() {
  console.log('=== 完整消息功能测试 ===\n');
  
  try {
    // 1. 获取新的token
    authToken = await getNewToken();
    if (!authToken) {
      console.log('无法获取有效的认证token，测试终止');
      return;
    }
    
    console.log('新的token:', authToken.substring(0, 50) + '...\n');
    
    // 2. 获取用户列表
    console.log('2. 获取用户列表...');
    const users = await makeRequest('/users?page=1&limit=10', 'GET', null, authToken);
    console.log('状态:', users.status);
    if (users.status === 200 && users.data.users) {
      console.log('找到用户数:', users.data.users.length);
      users.data.users.forEach(user => {
        console.log(`  - ID: ${user.id}, 用户名: ${user.username}`);
      });
    }
    
    // 3. 获取未读消息
    console.log('\n3. 获取未读消息数量...');
    const unread = await makeRequest('/chat/unread-count', 'GET', null, authToken);
    console.log('未读消息:', unread.data);
    
    // 4. 发送测试消息给用户2
    console.log('\n4. 发送测试消息给用户2...');
    const messageData = {
      receiver_id: 16,
      message: `测试消息：${new Date().toLocaleString()} - 验证消息传递`
    };
    const sendResult = await makeRequest('/chat/send', 'POST', messageData, authToken);
    console.log('发送结果:', JSON.stringify(sendResult.data, null, 2));
    
    // 5. 获取聊天历史
    console.log('\n5. 获取与用户2的聊天历史...');
    const history = await makeRequest('/chat/history/16?page=1&limit=5', 'GET', null, authToken);
    console.log('聊天历史:', JSON.stringify(history.data, null, 2));
    
    // 6. 获取所有会话
    console.log('\n6. 获取所有聊天会话...');
    const conversations = await makeRequest('/chat/conversations?page=1&limit=5', 'GET', null, authToken);
    console.log('聊天会话:', JSON.stringify(conversations.data, null, 2));
    
    console.log('\n=== 测试总结 ===');
    if (sendResult.status === 200) {
      console.log('✅ 消息发送成功');
      console.log('✅ 现在用户2应该能看到这条消息了！');
    } else {
      console.log('❌ 消息发送失败');
    }
    
  } catch (error) {
    console.error('测试失败:', error.message);
  }
}

// 运行完整测试
runCompleteTest();