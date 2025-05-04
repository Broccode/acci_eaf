import { Injectable } from '@nestjs/common';
import { CommandHandler } from '../../cqrs';
import { CreateTenantCommand } from '../impl/create-tenant.command';
import { TenantsService } from '../../tenants/tenants.service';
import { Tenant } from '../../tenants/entities/tenant.entity';
import { CreateTenantDto } from '../../tenants/dto/create-tenant.dto';

@Injectable()
export class CreateTenantHandler implements CommandHandler<CreateTenantCommand, Tenant> {
  constructor(private readonly tenantsService: TenantsService) {}

  async execute(command: CreateTenantCommand): Promise<Tenant> {
    const createTenantDto: CreateTenantDto = {
      name: command.name,
      description: command.description,
      contactEmail: command.contactEmail,
      configuration: command.configuration,
    };
    
    return this.tenantsService.create(createTenantDto);
  }
}
