import { MikroOrmModule } from '@mikro-orm/nestjs';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';
import { CreateTenantCommand } from '../commands/impl/create-tenant.command';
import { ListTenantsQuery } from '../queries/impl/list-tenants.query';
import { GetTenantByIdQuery } from '../queries/impl/get-tenant-by-id.query';
import { InMemoryCommandBus, InMemoryQueryBus } from 'infrastructure';
import { MikroORM, EntityManager } from '@mikro-orm/core';
import { Tenant } from '../tenants/entities/tenant.entity';
import { CreateTenantHandler } from '../commands/handlers/create-tenant.handler';
import { ListTenantsHandler } from '../queries/handlers/list-tenants.handler';
import { GetTenantByIdHandler } from '../queries/handlers/get-tenant-by-id.handler';
import { TenantRepository } from '../tenants/entities/tenant.repository';
import { TenantsService } from '../tenants/tenants.service';

describe('CQRS Integration Flow', () => {
  let orm: MikroORM;
  let em: EntityManager;
  let tenantRepository: TenantRepository;
  let tenantService: TenantsService;
  let commandBus: InMemoryCommandBus;
  let queryBus: InMemoryQueryBus;
  let createTenantHandler: CreateTenantHandler;
  let listTenantsHandler: ListTenantsHandler;
  let getTenantByIdHandler: GetTenantByIdHandler;

  beforeAll(async () => {
    // Manuell Setup ohne NestJS
    orm = await MikroORM.init({
      driver: BetterSqliteDriver,
      dbName: ':memory:',
      entities: [Tenant],
    });
    
    // Schema erstellen
    const generator = orm.getSchemaGenerator();
    await generator.createSchema();
    
    // EntityManager und Repository erstellen
    em = orm.em.fork();
    tenantRepository = new TenantRepository(em);
    tenantService = new TenantsService(tenantRepository, em);
    
    // Handlers erstellen
    createTenantHandler = new CreateTenantHandler(tenantService);
    listTenantsHandler = new ListTenantsHandler(tenantService);
    getTenantByIdHandler = new GetTenantByIdHandler(tenantService);
    
    // Command und Query Busse erstellen
    commandBus = new InMemoryCommandBus();
    queryBus = new InMemoryQueryBus();
    
    // Handler registrieren
    commandBus.register('CreateTenantCommand', createTenantHandler.execute.bind(createTenantHandler));
    queryBus.register('ListTenantsQuery', listTenantsHandler.execute.bind(listTenantsHandler));
    queryBus.register('GetTenantByIdQuery', getTenantByIdHandler.execute.bind(getTenantByIdHandler));
  });

  afterAll(async () => {
    await orm.close(true);
  });

  it('should create and retrieve a tenant via CQRS', async () => {
    await commandBus.execute(
      new CreateTenantCommand('IntegrationName', 'IntegrationDesc', 'int@example.com', {}),
    );

    const tenants = await queryBus.execute(new ListTenantsQuery(false));
    expect(tenants).toHaveLength(1);
    const tenant = tenants[0];
    expect(tenant.name).toBe('IntegrationName');

    const fetched = await queryBus.execute(new GetTenantByIdQuery(tenant.id));
    expect(fetched.id).toBe(tenant.id);
  });
});
