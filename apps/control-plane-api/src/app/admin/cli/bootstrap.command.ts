import { Command, CommandRunner, Option } from 'nest-commander';
import * as bcrypt from 'bcrypt';
import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';

interface BootstrapCommandOptions {
  username: string;
  password: string;
  configFile?: string;
}

@Command({ name: 'bootstrap', description: 'Bootstrap the Control Plane API' })
export class BootstrapCommand extends CommandRunner {
  async run(
    passedParams: string[],
    options?: BootstrapCommandOptions,
  ): Promise<void> {
    if (!options) {
      console.error('No options provided');
      return;
    }

    try {
      // Generate bootstrap token
      const bootstrapToken = crypto.randomBytes(32).toString('hex');
      
      // Generate secure JWT secret if not already set
      const jwtSecret = crypto.randomBytes(32).toString('hex');
      
      // Create environment config
      const envConfig = {
        ADMIN_USERNAME: options.username,
        ADMIN_PASSWORD: await bcrypt.hash(options.password, 10),
        JWT_SECRET: jwtSecret,
        BOOTSTRAP_ADMIN_TOKEN: bootstrapToken,
      };
      
      // Default config file path
      const configFilePath = options.configFile || 
        path.join(process.cwd(), '.env.control-plane');
      
      // Write to .env file
      const envContent = Object.entries(envConfig)
        .map(([key, value]) => `${key}=${value}`)
        .join('\n');
      
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
    }
  }

  @Option({
    flags: '-u, --username [username]',
    description: 'Admin username',
    required: true,
  })
  parseUsername(username: string): string {
    return username;
  }

  @Option({
    flags: '-p, --password [password]',
    description: 'Admin password',
    required: true,
  })
  parsePassword(password: string): string {
    return password;
  }

  @Option({
    flags: '-c, --config-file [path]',
    description: 'Path to save the configuration file',
  })
  parseConfigFile(configFile: string): string {
    return configFile;
  }
} 