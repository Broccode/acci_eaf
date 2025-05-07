import { Test, TestingModule } from '@nestjs/testing';
import { NestE2ETestHelper } from 'testing';
import { TestAppModuleE2E } from '../app/test-app-module.e2e';
import request from 'supertest';
import { TenantsService } from '../app/tenants/tenants.service';
import { INestApplication } from '@nestjs/common';
import { Tenant } from '../app/tenants/entities/tenant.entity';
import { AdminGuard } from '../app/auth/guards/admin.guard';
import { ExecutionContext } from '@nestjs/common';

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

describe('TenantsController (E2E) with TestAppModuleE2E', () => {
  let app: INestApplication;
  let helper: NestE2ETestHelper;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [TestAppModuleE2E],
      providers: [
        {
          provide: TenantsService,
          useValue: tenantsServiceMock,
        },
      ],
    })
    .overrideGuard(AdminGuard)
    .useValue({
      canActivate: (context: ExecutionContext) => {
        return true;
      }
    })
    .compile();

    app = moduleFixture.createNestApplication();
    await app.init();
    
    helper = new NestE2ETestHelper();
    helper.app = app;
    helper.testingModule = moduleFixture;
  });

  afterAll(async () => {
    await app.close();
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
        expect(body.id).toEqual(expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i));
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