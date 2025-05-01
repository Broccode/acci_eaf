import { 
  Controller, 
  Get, 
  Post, 
  Body, 
  Param, 
  Patch, 
  Delete, 
  HttpCode, 
  HttpStatus,
  Query,
  UseGuards
} from '@nestjs/common';
import { 
  ApiTags, 
  ApiOperation, 
  ApiResponse, 
  ApiParam, 
  ApiQuery,
  ApiBearerAuth
} from '@nestjs/swagger';
import { TenantsService } from './tenants.service';
import { CreateTenantDto } from './dto/create-tenant.dto';
import { UpdateTenantDto } from './dto/update-tenant.dto';
import { Tenant } from './entities/tenant.entity';
import { AdminGuard } from '../auth/guards/admin.guard';

@ApiTags('tenants')
@Controller('tenants')
@UseGuards(AdminGuard)
@ApiBearerAuth()
export class TenantsController {
  constructor(private readonly tenantsService: TenantsService) {}

  @Post()
  @ApiOperation({ summary: 'Create a new tenant' })
  @ApiResponse({ status: 201, description: 'The tenant has been successfully created.', type: Tenant })
  create(@Body() createTenantDto: CreateTenantDto): Promise<Tenant> {
    return this.tenantsService.create(createTenantDto);
  }

  @Get()
  @ApiOperation({ summary: 'Get all tenants' })
  @ApiResponse({ status: 200, description: 'Return all tenants.', type: [Tenant] })
  @ApiQuery({ name: 'active', required: false, type: Boolean, description: 'Filter by active status' })
  findAll(@Query('active') active?: boolean): Promise<Tenant[]> {
    if (active === true) {
      return this.tenantsService.findActive();
    }
    return this.tenantsService.findAll();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get a tenant by ID' })
  @ApiResponse({ status: 200, description: 'Return the tenant.', type: Tenant })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  findOne(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.findOne(id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update a tenant' })
  @ApiResponse({ status: 200, description: 'The tenant has been successfully updated.', type: Tenant })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  update(@Param('id') id: string, @Body() updateTenantDto: UpdateTenantDto): Promise<Tenant> {
    return this.tenantsService.update(id, updateTenantDto);
  }

  @Patch(':id/activate')
  @ApiOperation({ summary: 'Activate a tenant' })
  @ApiResponse({ status: 200, description: 'The tenant has been successfully activated.', type: Tenant })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  activate(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.activate(id);
  }

  @Patch(':id/deactivate')
  @ApiOperation({ summary: 'Deactivate a tenant' })
  @ApiResponse({ status: 200, description: 'The tenant has been successfully deactivated.', type: Tenant })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  deactivate(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.deactivate(id);
  }

  @Patch(':id/suspend')
  @ApiOperation({ summary: 'Suspend a tenant' })
  @ApiResponse({ status: 200, description: 'The tenant has been successfully suspended.', type: Tenant })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  suspend(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.suspend(id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a tenant' })
  @ApiResponse({ status: 204, description: 'The tenant has been successfully deleted.' })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  remove(@Param('id') id: string): Promise<void> {
    return this.tenantsService.remove(id);
  }
} 