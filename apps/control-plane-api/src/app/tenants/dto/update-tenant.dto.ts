import { PartialType } from '@nestjs/swagger';
import { CreateTenantDto } from './create-tenant.dto';

/**
 * DTO for updating a tenant
 * Makes all fields from CreateTenantDto optional
 */
export class UpdateTenantDto extends PartialType(CreateTenantDto) {} 