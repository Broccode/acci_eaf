import { Injectable } from '@nestjs/common';
import { CommandHandler } from '../../cqrs';
import { UpdateTenantCommand } from '../impl/update-tenant.command';
import { TenantsService } from '../../tenants/tenants.service';
import { Tenant } from '../../tenants/entities/tenant.entity';

@Injectable()
export class UpdateTenantHandler implements CommandHandler<UpdateTenantCommand, Tenant> {
  constructor(private readonly tenantsService: TenantsService) {}

  async execute(command: UpdateTenantCommand): Promise<Tenant> {
    return this.tenantsService.update(command.id, {
      name: command.name,
      description: command.description,
      contactEmail: command.contactEmail,
      configuration: command.configuration,
    });
  }
}
