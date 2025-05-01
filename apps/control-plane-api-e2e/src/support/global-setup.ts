/* eslint-disable */
var __TEARDOWN_MESSAGE__: string;

import { execSync } from 'child_process';
import axios from 'axios';

async function waitForAPI(baseUrl: string, maxRetries = 10, retryDelay = 1000): Promise<boolean> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      await axios.get(baseUrl);
      console.log('API is available');
      return true;
    } catch (error) {
      console.log(`API not yet available (attempt ${i + 1}/${maxRetries}), retrying in ${retryDelay}ms...`);
      await new Promise(resolve => setTimeout(resolve, retryDelay));
    }
  }
  
  throw new Error('API failed to become available');
}

async function createAdminUser(baseUrl: string, bootstrapToken: string): Promise<void> {
  try {
    console.log('Creating admin user...');
    const result = await axios.post(`${baseUrl.replace('/health', '')}/auth/create-admin`, {
      username: 'admin',
      password: 'whiskey',
      adminToken: bootstrapToken
    });
    
    if (result.status === 201) {
      console.log('Admin user created successfully');
    } else {
      console.log('Unexpected response when creating admin:', result.status);
    }
  } catch (error) {
    // Admin user might already exist - we can ignore this error
    // Admin creation can either return 409 (proper conflict) or 401 (unauthorized - ambiguous error)
    if (error.response && (error.response.status === 409 || error.response.status === 401)) {
      console.log('Admin user may already exist, continuing with tests');
    } else {
      console.error('Unexpected error creating admin user:', error.message);
      throw error;
    }
  }
}

module.exports = async function () {
  // Start services that that the app needs to run (e.g. database, docker-compose, etc.).
  console.log('\nSetting up E2E test environment...\n');

  try {
    // Set up environment variables for testing
    process.env.NODE_ENV = 'test';
    process.env.DB_TYPE = 'better-sqlite';
    process.env.DB_NAME = ':memory:';
    
    // Set up bootstrap token for admin creation
    const bootstrapToken = 'test-bootstrap-token';
    process.env.BOOTSTRAP_ADMIN_TOKEN = bootstrapToken;
    
    // Start the API in a separate process
    console.log('Starting API service...');
    
    // Check if API is already running
    const host = process.env.HOST ?? 'localhost';
    const port = process.env.PORT ?? '3000';
    const apiUrl = `http://${host}:${port}/api/health`;
    
    try {
      await waitForAPI(apiUrl);
      console.log('API is already running');
    } catch (error) {
      console.log('API is not running, starting it...');
      
      // Start the API in the background
      const startApiCmd = `npx nx serve control-plane-api &`;
      execSync(startApiCmd, { 
        stdio: 'inherit',
        env: {
          ...process.env,
          // Using in-memory database for testing
          DB_TYPE: 'better-sqlite',
          DB_NAME: ':memory:',
          NODE_ENV: 'test',
          BOOTSTRAP_ADMIN_TOKEN: bootstrapToken
        }
      });
      
      // Wait for API to be available
      console.log('Waiting for API to be available...');
      await waitForAPI(apiUrl);
    }
    
    // Create admin user
    await createAdminUser(apiUrl, bootstrapToken);
    
    console.log('E2E test setup complete');
  } catch (error) {
    console.error('Error during E2E setup:', error);
    process.exit(1);
  }

  // Hint: Use `globalThis` to pass variables to global teardown.
  globalThis.__TEARDOWN_MESSAGE__ = '\nTearing down E2E test environment...\n';
};
