import { NestE2ETestHelper } from 'testing';
import { AppModule } from '../app/app.module';
import request from 'supertest';
import { TenantsService } from '../app/tenants/tenants.service';
import { INestApplication } from '@nestjs/common';
import { Tenant } from '../app/tenants/entities/tenant.entity';

// Mock data
const tenantMock: Tenant = {
  id: 'test-id',
  name: 'TestTenant',
  description: 'mock',
  contactEmail: 'test@example.com',
  configuration: {},
  status: 'active' as any,
  createdAt: new Date(),
  updatedAt: new Date(),
} as Tenant;

// Mock TenantsService implementation
const tenantsServiceMock = {
  create: jest.fn().mockResolvedValue(tenantMock),
  findAll: jest.fn().mockResolvedValue([tenantMock]),
  findOne: jest.fn().mockResolvedValue(tenantMock),
  update: jest.fn().mockResolvedValue({ ...tenantMock, name: 'Updated' }),
  remove: jest.fn().mockResolvedValue(undefined),
};

describe('TenantsController (E2E)', () => {
  let app: INestApplication;
  let helper: NestE2ETestHelper;

  beforeAll(async () => {
    helper = await NestE2ETestHelper.createTestingApp({
      imports: [AppModule],
      providers: [
        {
          provide: TenantsService,
          useValue: tenantsServiceMock,
        },
      ],
    });
    app = helper.app;
  });

  afterAll(async () => {
    await helper.close();
  });

  it('POST /tenants should create tenant', async () => {
    await request(app.getHttpServer())
      .post('/tenants')
      .send({
        name: 'TestTenant',
        description: 'mock',
        contactEmail: 'test@example.com',
        configuration: {},
      })
      .expect(201)
      .expect(({ body }) => {
        expect(body.id).toBe(tenantMock.id);
      });
  });

  it('GET /tenants should list tenants', async () => {
    await request(app.getHttpServer())
      .get('/tenants')
      .expect(200)
      .expect(({ body }) => {
        expect(Array.isArray(body)).toBe(true);
        expect(body.length).toBe(1);
      });
  });
}); 