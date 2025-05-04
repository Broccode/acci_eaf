import { Test, TestingModule } from '@nestjs/testing';
import { UpdateTenantHandler } from './update-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { UpdateTenantCommand } from '../impl/update-tenant.command';
import { Tenant } from '../../tenants/entities/tenant.entity';

describe('UpdateTenantHandler', () => {
  let handler: UpdateTenantHandler;
  let service: TenantsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UpdateTenantHandler,
        {
          provide: TenantsService,
          useValue: {
            update: jest.fn(),
          },
        },
      ],
    }).compile();

    handler = module.get<UpdateTenantHandler>(UpdateTenantHandler);
    service = module.get<TenantsService>(TenantsService);
  });

  it('should call tenantsService.update with correct params and return the updated tenant', async () => {
    const mockTenant = {
      id: 'uuid',
      name: 'UpdatedName',
      description: 'UpdatedDesc',
      contactEmail: 'updated@example.com',
      configuration: {},
      status: undefined,
      createdAt: new Date(),
      updatedAt: new Date(),
    } as Tenant;
    (service.update as jest.Mock).mockResolvedValue(mockTenant);

    const command = new UpdateTenantCommand(
      'uuid',
      'UpdatedName',
      'UpdatedDesc',
      'updated@example.com',
      {},
    );
    const result = await handler.execute(command);

    expect(service.update).toHaveBeenCalledWith('uuid', {
      name: 'UpdatedName',
      description: 'UpdatedDesc',
      contactEmail: 'updated@example.com',
      configuration: {},
    });
    expect(result).toEqual(mockTenant);
  });
}); 