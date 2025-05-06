export default () => ({
  // JWT Configuration
  jwt: {
    secret: process.env['JWT_SECRET'] || 'devSecretDoNotUseInProduction',
    expiresIn: process.env['JWT_EXPIRES_IN'] || '1h',
  },
  
  // Admin Configuration
  admin: {
    username: process.env['ADMIN_USERNAME'],
    password: process.env['ADMIN_PASSWORD'],
  },
  
  // Bootstrap Configuration
  bootstrap: {
    token: process.env['BOOTSTRAP_ADMIN_TOKEN'],
  },
  
  // Server Configuration
  server: {
    port: parseInt(process.env['PORT'] || '3000', 10),
    host: process.env['HOST'] || '0.0.0.0',
  },
}); 