import { Test, TestingModule } from '@nestjs/testing';
import { CreateTenantHandler } from './create-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { CreateTenantCommand } from '../impl/create-tenant.command';
import { Tenant } from '../../tenants/entities/tenant.entity';

describe('CreateTenantHandler', () => {
  let handler: CreateTenantHandler;
  let service: TenantsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CreateTenantHandler,
        {
          provide: TenantsService,
          useValue: {
            create: jest.fn(),
          },
        },
      ],
    }).compile();

    handler = module.get<CreateTenantHandler>(CreateTenantHandler);
    service = module.get<TenantsService>(TenantsService);
  });

  it('should call tenantsService.create with correct params and return the created tenant', async () => {
    const mockTenant = {
      id: 'uuid',
      name: 'TestTenant',
      description: 'TestDescription',
      contactEmail: 'test@example.com',
      configuration: {},
      status: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
    } as Tenant;
    (service.create as jest.Mock).mockResolvedValue(mockTenant);

    const command = new CreateTenantCommand('TestTenant', 'TestDescription', 'test@example.com', {});
    const result = await handler.execute(command);

    expect(service.create).toHaveBeenCalledWith({
      name: 'TestTenant',
      description: 'TestDescription',
      contactEmail: 'test@example.com',
      configuration: {},
    });
    expect(result).toEqual(mockTenant);
  });
}); 