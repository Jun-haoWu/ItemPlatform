const bcrypt = require('bcryptjs');

async function generateHash() {
  const password = 'password123';
  const hashedPassword = await bcrypt.hash(password, 10);
  console.log('Password:', password);
  console.log('Hashed:', hashedPassword);
  console.log('\nSQL to insert user:');
  console.log(`INSERT INTO users (username, password, email) VALUES ('testuser', '${hashedPassword}', 'test@example.com') ON DUPLICATE KEY UPDATE password = '${hashedPassword}';`);
}

generateHash();
