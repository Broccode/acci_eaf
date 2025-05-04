import axios, { AxiosInstance } from 'axios';

describe('Control Plane API', () => {
  const baseURL = 'http://localhost:3000';
  
  describe('Health Check', () => {
    it('should return 200 for health endpoint', async () => {
      try {
        const res = await axios.get(`${baseURL}/api/health`);
        console.log('Health endpoint status:', res.status);
        expect(res.status).toBe(200);
      } catch (error) {
        console.error('Health check failed:', error.message);
        fail('Health check should not fail');
      }
    });
  });

  describe('Tenants CQRS E2E', () => {
    let tenantId: string;
    let authHeader: string;
    let client: AxiosInstance;

    beforeAll(async () => {
      const loginRes = await axios.post(`${baseURL}/api/auth/login`, {
        username: 'admin',
        password: 'whiskey',
      });
      expect(loginRes.status).toBe(201);
      authHeader = `Bearer ${loginRes.data.access_token}`;
      client = axios.create({
        baseURL: `${baseURL}/api`,
        headers: { Authorization: authHeader },
      });
    });

    const tenantDto = {
      name: 'E2ETenant',
      description: 'E2E description',
      contactEmail: 'e2e@example.com',
      configuration: {},
    };

    it('should create a new tenant', async () => {
      const res = await client.post('/tenants', tenantDto);
      expect(res.status).toBe(201);
      expect(res.data).toHaveProperty('id');
      tenantId = res.data.id;
    });

    it('should list tenants including the new one', async () => {
      const res = await client.get('/tenants');
      expect(res.status).toBe(200);
      const tenants = res.data as any[];
      expect(tenants.find(t => t.id === tenantId)).toBeDefined();
    });

    it('should retrieve the tenant by id', async () => {
      const res = await client.get(`/tenants/${tenantId}`);
      expect(res.status).toBe(200);
      expect(res.data.name).toBe(tenantDto.name);
    });

    it('should update the tenant', async () => {
      const updateDto = { ...tenantDto, name: 'UpdatedE2E' };
      const res = await client.patch(`/tenants/${tenantId}`, updateDto);
      expect(res.status).toBe(200);
      expect(res.data.name).toBe('UpdatedE2E');
    });

    it('should deactivate the tenant', async () => {
      const res = await client.patch(`/tenants/${tenantId}/deactivate`);
      expect(res.status).toBe(200);
      expect(res.data.status).toBe('inactive');
    });

    it('should activate the tenant', async () => {
      const res = await client.patch(`/tenants/${tenantId}/activate`);
      expect(res.status).toBe(200);
      expect(res.data.status).toBe('active');
    });

    it('should suspend the tenant', async () => {
      const res = await client.patch(`/tenants/${tenantId}/suspend`);
      expect(res.status).toBe(200);
      expect(res.data.status).toBe('suspended');
    });

    it('should delete the tenant', async () => {
      const res = await client.delete(`/tenants/${tenantId}`);
      expect(res.status).toBe(204);
    });

    it('should return 404 for deleted tenant', async () => {
      try {
        await client.get(`/tenants/${tenantId}`);
        fail('Expected 404');
      } catch (error: any) {
        expect(error.response.status).toBe(404);
      }
    });
  });
});
