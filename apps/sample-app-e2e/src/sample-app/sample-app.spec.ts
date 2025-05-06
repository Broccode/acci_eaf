import axios from 'axios';
import { NestE2ETestHelper } from 'testing';
import { AppModule } from '../../../sample-app/src/app/app.module';

describe('GET /api', () => {
  it('should return a message', async () => {
    const res = await axios.get(`/api`);

    expect(res.status).toBe(200);
    expect(res.data).toEqual({ message: 'Hello API' });
  });
});

describe('Health Endpoint (/health)', () => {
  let helper: NestE2ETestHelper;

  beforeAll(async () => {
    helper = await NestE2ETestHelper.createTestingApp({
      imports: [AppModule],
    });
  });

  afterAll(async () => {
    await helper.close();
  });

  it('should return ok status', async () => {
    const response = await helper.getRequest('/health').expect(200);
    expect(response.body).toEqual({ status: 'ok' });
  });
});
