import { Injectable } from '@nestjs/common';
import { CommandHandler } from '../../cqrs';
import { DeleteTenantCommand } from '../impl/delete-tenant.command';
import { TenantsService } from '../../tenants/tenants.service';

@Injectable()
export class DeleteTenantHandler implements CommandHandler<DeleteTenantCommand, void> {
  constructor(private readonly tenantsService: TenantsService) {}

  async execute(command: DeleteTenantCommand): Promise<void> {
    await this.tenantsService.remove(command.id);
  }
} 