import { Controller, Get, Module } from '@nestjs/common';
import { NestE2ETestHelper } from '../nest-e2e-test.helper';

/**
 * Example controller for demonstration
 */
@Controller('health')
class HealthController {
  @Get()
  check() {
    return { status: 'ok' };
  }
}

/**
 * Example module for demonstration
 */
@Module({
  controllers: [HealthController]
})
class AppModule {}

/**
 * Example E2E test for the HealthController
 * This is just for demonstration purposes and won't be executed
 */
describe.skip('HealthController (e2e)', () => {
  let testHelper: NestE2ETestHelper;

  beforeAll(async () => {
    testHelper = await NestE2ETestHelper.createTestingApp({
      imports: [AppModule]
    });
  });

  afterAll(async () => {
    await testHelper.close();
  });

  it('/health (GET)', async () => {
    const response = await testHelper.getRequest('/health')
      .expect(200)
      .expect('Content-Type', /json/);
    
    expect(response.body).toEqual({ status: 'ok' });
  });
});

/**
 * Example with authentication
 */
describe.skip('Authenticated routes', () => {
  let testHelper: NestE2ETestHelper;
  // Example auth token, in real tests you'd generate this
  const authToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIxMjM0NTYiLCJ0ZW5hbnRJZCI6ImRlbW8iLCJpYXQiOjE1MTYyMzkwMjJ9.fake-token';

  beforeAll(async () => {
    testHelper = await NestE2ETestHelper.createTestingApp({
      imports: [AppModule]
    });
  });

  afterAll(async () => {
    await testHelper.close();
  });

  it('should access authenticated route', async () => {
    // Example of authenticated request
    await testHelper.getRequest('/protected-route', authToken)
      .expect(200);
  });

  it('should send data to API', async () => {
    const data = { name: 'Test User', email: 'test@example.com' };
    
    // Example of authenticated POST request with data
    await testHelper.postRequest('/users', data, authToken)
      .expect(201);
  });
}); 