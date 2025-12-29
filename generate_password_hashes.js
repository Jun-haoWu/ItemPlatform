const bcrypt = require('bcryptjs');

const passwords = [
  '123456',
  'password123',
  'admin',
  'test123'
];

async function generateHashes() {
  console.log('正在生成密码哈希值...\n');
  
  for (const password of passwords) {
    const saltRounds = 10;
    const hash = await bcrypt.hash(password, saltRounds);
    console.log(`密码: ${password}`);
    console.log(`哈希值: ${hash}`);
    console.log('');
  }
}

generateHashes().catch(console.error);
