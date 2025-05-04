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
  UseGuards,
  Inject
} from '@nestjs/common';
import { 
  ApiTags, 
  ApiOperation, 
  ApiResponse, 
  ApiParam, 
  ApiQuery,
  ApiBearerAuth,
  ApiBody
} from '@nestjs/swagger';
import { TenantsService } from './tenants.service';
import { CreateTenantDto } from './dto/create-tenant.dto';
import { UpdateTenantDto } from './dto/update-tenant.dto';
import { Tenant } from './entities/tenant.entity';
import { AdminGuard } from '../auth/guards/admin.guard';
import { CommandBus, QueryBus } from 'core';
import { COMMAND_BUS, QUERY_BUS } from '../cqrs.constants';
import { CreateTenantCommand } from '../commands/impl/create-tenant.command';
import { UpdateTenantCommand } from '../commands/impl/update-tenant.command';
import { DeleteTenantCommand } from '../commands/impl/delete-tenant.command';
import { GetTenantByIdQuery } from '../queries/impl/get-tenant-by-id.query';
import { ListTenantsQuery } from '../queries/impl/list-tenants.query';

@ApiTags('tenants')
@Controller('tenants')
@UseGuards(AdminGuard)
@ApiBearerAuth()
export class TenantsController {
  constructor(
    private readonly tenantsService: TenantsService,
    @Inject(COMMAND_BUS) private readonly commandBus: CommandBus,
    @Inject(QUERY_BUS) private readonly queryBus: QueryBus,
  ) {}

  @Post()
  @ApiOperation({ summary: 'Create a new tenant' })
  @ApiResponse({
    status: 201,
    description: 'The tenant has been successfully created.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' },
        status: 'active',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }
  })
  @ApiBody({
    schema: {
      example: {
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' }
      }
    }
  })
  async create(@Body() createTenantDto: CreateTenantDto): Promise<Tenant> {
    return this.commandBus.execute(
      new CreateTenantCommand(
        createTenantDto.name,
        createTenantDto.description,
        createTenantDto.contactEmail,
        createTenantDto.configuration,
      ),
    );
  }

  @Get()
  @ApiOperation({ summary: 'Get all tenants' })
  @ApiResponse({
    status: 200,
    description: 'Return all tenants.',
    type: [Tenant],
    schema: {
      example: [
        {
          id: '123e4567-e89b-12d3-a456-426614174000',
          name: 'ExampleTenant',
          description: 'An example tenant',
          contactEmail: 'example@domain.com',
          configuration: { plan: 'basic' },
          status: 'active',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ]
    }
  })
  @ApiQuery({ name: 'active', required: false, type: Boolean, description: 'Filter by active status' })
  async findAll(@Query('active') active?: boolean): Promise<Tenant[]> {
    return this.queryBus.execute(
      new ListTenantsQuery(active === true),
    );
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get a tenant by ID' })
  @ApiResponse({
    status: 200,
    description: 'Return the tenant.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' },
        status: 'active',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  async findOne(@Param('id') id: string): Promise<Tenant> {
    return this.queryBus.execute(
      new GetTenantByIdQuery(id),
    );
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Update a tenant' })
  @ApiResponse({
    status: 200,
    description: 'The tenant has been successfully updated.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'UpdatedTenant',
        description: 'Updated example tenant',
        contactEmail: 'updated@example.com',
        configuration: { plan: 'pro' },
        status: 'active',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    }
  })
  @ApiBody({
    schema: {
      example: {
        name: 'UpdatedTenant',
        description: 'Updated example tenant',
        contactEmail: 'updated@example.com',
        configuration: { plan: 'pro' }
      }
    }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  async update(
    @Param('id') id: string,
    @Body() updateTenantDto: UpdateTenantDto,
  ): Promise<Tenant> {
    return this.commandBus.execute(
      new UpdateTenantCommand(
        id,
        updateTenantDto.name,
        updateTenantDto.description,
        updateTenantDto.contactEmail,
        updateTenantDto.configuration,
      ),
    );
  }

  @Patch(':id/activate')
  @ApiOperation({ summary: 'Activate a tenant' })
  @ApiResponse({
    status: 200,
    description: 'The tenant has been successfully activated.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' },
        status: 'active',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  activate(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.activate(id);
  }

  @Patch(':id/deactivate')
  @ApiOperation({ summary: 'Deactivate a tenant' })
  @ApiResponse({
    status: 200,
    description: 'The tenant has been successfully deactivated.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' },
        status: 'inactive',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  deactivate(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.deactivate(id);
  }

  @Patch(':id/suspend')
  @ApiOperation({ summary: 'Suspend a tenant' })
  @ApiResponse({
    status: 200,
    description: 'The tenant has been successfully suspended.',
    type: Tenant,
    schema: {
      example: {
        id: '123e4567-e89b-12d3-a456-426614174000',
        name: 'ExampleTenant',
        description: 'An example tenant',
        contactEmail: 'example@domain.com',
        configuration: { plan: 'basic' },
        status: 'suspended',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }
    }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  suspend(@Param('id') id: string): Promise<Tenant> {
    return this.tenantsService.suspend(id);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Delete a tenant' })
  @ApiResponse({
    status: 204,
    description: 'The tenant has been successfully deleted.',
    schema: { example: null }
  })
  @ApiParam({ name: 'id', description: 'The tenant ID' })
  async remove(@Param('id') id: string): Promise<void> {
    return this.commandBus.execute(new DeleteTenantCommand(id));
  }
} 