import axios from 'axios';

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
});
