import axios from 'axios';
import * as fs from 'fs';
import * as path from 'path';
import { execSync } from 'child_process';

describe('Auth E2E Tests', () => {
  // Config for test admin user
  const testAdminConfig = {
    username: 'e2e-test-admin',
    password: 'Test@123456',
  };
  
  let adminToken: string;
  let bootstrapToken: string;
  const envConfigPath = path.join(process.cwd(), '.env.control-plane-e2e-test');
  
  // Ensure test environment is clean before running tests
  beforeAll(async () => {
    try {
      // Run bootstrap command to create test configuration
      console.log('Setting up test environment...');
      
      // Execute bootstrap command with the simple Node.js script
      execSync(`node apps/control-plane-api/bootstrap.js -u ${testAdminConfig.username} -p ${testAdminConfig.password} -c ${envConfigPath}`, { 
        stdio: 'inherit' 
      });
      
      // Read the generated bootstrap token from config file
      const envContent = fs.readFileSync(envConfigPath, 'utf8');
      const envLines = envContent.split('\n');
      
      // Extract bootstrap token from config
      const bootstrapTokenLine = envLines.find(line => line.startsWith('BOOTSTRAP_ADMIN_TOKEN='));
      if (bootstrapTokenLine) {
        bootstrapToken = bootstrapTokenLine.split('=')[1];
      } else {
        throw new Error('Bootstrap token not found in config file');
      }
      
      console.log('Test environment setup complete');
    } catch (error) {
      console.error('Error setting up test environment:', error);
      throw error;
    }
  });

  // Clean up environment after tests
  afterAll(async () => {
    try {
      // Remove test config file
      if (fs.existsSync(envConfigPath)) {
        fs.unlinkSync(envConfigPath);
        console.log('Test environment cleanup complete');
      }
    } catch (error) {
      console.error('Error cleaning up test environment:', error);
    }
  });

  describe('Authentication', () => {
    it('should login with valid admin credentials', async () => {
      const loginResponse = await axios.post('/auth/login', {
        username: testAdminConfig.username,
        password: testAdminConfig.password,
      });

      expect(loginResponse.status).toBe(201);
      expect(loginResponse.data).toHaveProperty('access_token');
      
      // Store token for subsequent tests
      adminToken = loginResponse.data.access_token;
    });

    it('should reject login with invalid credentials', async () => {
      try {
        await axios.post('/auth/login', {
          username: testAdminConfig.username,
          password: 'wrong-password',
        });
        
        // If the request succeeds, fail the test
        fail('Login with invalid credentials should have failed');
      } catch (error) {
        expect(error.response.status).toBe(401);
      }
    });

    it('should create a new admin user with valid bootstrap token', async () => {
      const createAdminResponse = await axios.post('/auth/create-admin', {
        username: 'second-admin',
        password: 'Admin@123456',
        adminToken: bootstrapToken,
      });

      expect(createAdminResponse.status).toBe(201);
      expect(createAdminResponse.data).toHaveProperty('message', 'Admin created successfully');
      expect(createAdminResponse.data).toHaveProperty('username', 'second-admin');
    });

    it('should reject admin creation with invalid bootstrap token', async () => {
      try {
        await axios.post('/auth/create-admin', {
          username: 'hacker-admin',
          password: 'Hack@123456',
          adminToken: 'invalid-token',
        });
        
        // If the request succeeds, fail the test
        fail('Admin creation with invalid token should have failed');
      } catch (error) {
        expect(error.response.status).toBe(401);
      }
    });
  });

  describe('Protected Routes', () => {
    it('should access protected routes with valid JWT token', async () => {
      const tenantsResponse = await axios.get('/tenants', {
        headers: {
          Authorization: `Bearer ${adminToken}`,
        },
      });

      expect(tenantsResponse.status).toBe(200);
      expect(Array.isArray(tenantsResponse.data)).toBe(true);
    });

    it('should reject access to protected routes without JWT token', async () => {
      try {
        await axios.get('/tenants');
        
        // If the request succeeds, fail the test
        fail('Access to protected route without token should have failed');
      } catch (error) {
        expect(error.response.status).toBe(401);
      }
    });

    it('should reject access with invalid JWT token', async () => {
      try {
        await axios.get('/tenants', {
          headers: {
            Authorization: 'Bearer invalid-token',
          },
        });
        
        // If the request succeeds, fail the test
        fail('Access with invalid token should have failed');
      } catch (error) {
        expect(error.response.status).toBe(401);
      }
    });
  });

  describe('Tenant CRUD with Authentication', () => {
    let createdTenantId: string;

    it('should create a tenant when authenticated as admin', async () => {
      const createTenantResponse = await axios.post('/tenants', 
        {
          name: 'E2E Test Tenant',
          description: 'Tenant created during E2E testing',
        },
        {
          headers: {
            Authorization: `Bearer ${adminToken}`,
          },
        }
      );

      expect(createTenantResponse.status).toBe(201);
      expect(createTenantResponse.data).toHaveProperty('id');
      expect(createTenantResponse.data).toHaveProperty('name', 'E2E Test Tenant');
      
      createdTenantId = createTenantResponse.data.id;
    });

    it('should retrieve a tenant when authenticated as admin', async () => {
      const getTenantResponse = await axios.get(`/tenants/${createdTenantId}`, {
        headers: {
          Authorization: `Bearer ${adminToken}`,
        },
      });

      expect(getTenantResponse.status).toBe(200);
      expect(getTenantResponse.data).toHaveProperty('id', createdTenantId);
      expect(getTenantResponse.data).toHaveProperty('name', 'E2E Test Tenant');
    });

    it('should update a tenant when authenticated as admin', async () => {
      const updateTenantResponse = await axios.patch(
        `/tenants/${createdTenantId}`,
        {
          description: 'Updated description during E2E testing',
        },
        {
          headers: {
            Authorization: `Bearer ${adminToken}`,
          },
        }
      );

      expect(updateTenantResponse.status).toBe(200);
      expect(updateTenantResponse.data).toHaveProperty('id', createdTenantId);
      expect(updateTenantResponse.data).toHaveProperty('description', 'Updated description during E2E testing');
    });

    it('should deactivate a tenant when authenticated as admin', async () => {
      const deactivateTenantResponse = await axios.patch(
        `/tenants/${createdTenantId}/deactivate`,
        {},
        {
          headers: {
            Authorization: `Bearer ${adminToken}`,
          },
        }
      );

      expect(deactivateTenantResponse.status).toBe(200);
      expect(deactivateTenantResponse.data).toHaveProperty('id', createdTenantId);
      expect(deactivateTenantResponse.data).toHaveProperty('active', false);
    });

    it('should delete a tenant when authenticated as admin', async () => {
      const deleteTenantResponse = await axios.delete(
        `/tenants/${createdTenantId}`,
        {
          headers: {
            Authorization: `Bearer ${adminToken}`,
          },
        }
      );

      expect(deleteTenantResponse.status).toBe(204);
    });

    it('should confirm tenant was deleted', async () => {
      try {
        await axios.get(`/tenants/${createdTenantId}`, {
          headers: {
            Authorization: `Bearer ${adminToken}`,
          },
        });
        
        // If the request succeeds, fail the test
        fail('Tenant should have been deleted');
      } catch (error) {
        expect(error.response.status).toBe(404);
      }
    });
  });
}); 