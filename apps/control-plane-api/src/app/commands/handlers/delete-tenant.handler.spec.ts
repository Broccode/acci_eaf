import { Test, TestingModule } from '@nestjs/testing';
import { DeleteTenantHandler } from './delete-tenant.handler';
import { TenantsService } from '../../tenants/tenants.service';
import { DeleteTenantCommand } from '../impl/delete-tenant.command';

describe('DeleteTenantHandler', () => {
  let handler: DeleteTenantHandler;
  let service: TenantsService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        DeleteTenantHandler,
        {
          provide: TenantsService,
          useValue: { remove: jest.fn() },
        },
      ],
    }).compile();

    handler = module.get<DeleteTenantHandler>(DeleteTenantHandler);
    service = module.get<TenantsService>(TenantsService);
  });

  it('should call tenantsService.remove with correct id and return void', async () => {
    (service.remove as jest.Mock).mockResolvedValue(undefined);

    const command = new DeleteTenantCommand('uuid');
    const result = await handler.execute(command);

    expect(service.remove).toHaveBeenCalledWith('uuid');
    expect(result).toBeUndefined();
  });
}); 