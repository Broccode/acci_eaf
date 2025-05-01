const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const bcrypt = require('bcrypt');

// Read command line arguments
const args = process.argv.slice(2);
let username = '';
let password = '';
let configFile = '.env.control-plane';

// Parse command line arguments
for (let i = 0; i < args.length; i++) {
  if (args[i] === '-u' || args[i] === '--username') {
    username = args[i + 1];
    i++;
  } else if (args[i] === '-p' || args[i] === '--password') {
    password = args[i + 1];
    i++;
  } else if (args[i] === '-c' || args[i] === '--config-file') {
    configFile = args[i + 1];
    i++;
  }
}

// Validate inputs
if (!username || !password) {
  console.error('Error: Username and password are required.');
  console.error('Usage: node bootstrap.js -u <username> -p <password> [-c <config-file>]');
  process.exit(1);
}

async function bootstrap() {
  try {
    // Generate bootstrap token
    const bootstrapToken = crypto.randomBytes(32).toString('hex');
    
    // Generate secure JWT secret
    const jwtSecret = crypto.randomBytes(32).toString('hex');
    
    // Hash the password
    const hashedPassword = await bcrypt.hash(password, 10);
    
    // Create environment config
    const envConfig = {
      ADMIN_USERNAME: username,
      ADMIN_PASSWORD: hashedPassword,
      JWT_SECRET: jwtSecret,
      BOOTSTRAP_ADMIN_TOKEN: bootstrapToken,
    };
    
    // Convert to .env format
    const envContent = Object.entries(envConfig)
      .map(([key, value]) => `${key}=${value}`)
      .join('\n');
    
    // Write to file
    const configFilePath = path.resolve(process.cwd(), configFile);
    fs.writeFileSync(configFilePath, envContent);
    
    console.log(`
Bootstrap completed successfully!
Configuration saved to: ${configFilePath}

To create additional admin users, use the following bootstrap token:
${bootstrapToken}

Command example:
curl -X POST http://localhost:3000/auth/create-admin \\
  -H "Content-Type: application/json" \\
  -d '{"username":"newadmin","password":"securepassword","adminToken":"${bootstrapToken}"}'
`);
  } catch (error) {
    console.error('Bootstrap failed:', error);
    process.exit(1);
  }
}

bootstrap(); 