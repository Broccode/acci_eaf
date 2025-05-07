import { Module, OnModuleInit, Inject } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TenantsModule } from './tenants/tenants.module';
import { CqrsModule } from './cqrs.module';
import { CreateTenantHandler } from './commands/handlers/create-tenant.handler';
import { UpdateTenantHandler } from './commands/handlers/update-tenant.handler';
import { DeleteTenantHandler } from './commands/handlers/delete-tenant.handler';
import { GetTenantByIdHandler } from './queries/handlers/get-tenant-by-id.handler';
import { ListTenantsHandler } from './queries/handlers/list-tenants.handler';
import defaultConfig from './config/default.config';
import { APP_INTERCEPTOR } from '@nestjs/core';
import { AuditLogInterceptor } from './audit/audit-log.interceptor';
import { AuditLogService } from './audit/audit-log.service';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { MikroORM } from '@mikro-orm/core';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';
import { Tenant } from './tenants/entities/tenant.entity';
import { AuditLog } from 'libs/infrastructure/src/lib/persistence/entities/audit-log.entity';
import { InMemoryCommandBus, InMemoryQueryBus } from 'infrastructure';
import { COMMAND_BUS, QUERY_BUS } from 'core';

@Module({})
class CqrsRegistrationService implements OnModuleInit {
  constructor(
    @Inject(COMMAND_BUS) private readonly commandBus: InMemoryCommandBus,
    @Inject(QUERY_BUS) private readonly queryBus: InMemoryQueryBus,
    private readonly createTenantHandler: CreateTenantHandler,
    private readonly updateTenantHandler: UpdateTenantHandler,
    private readonly deleteTenantHandler: DeleteTenantHandler,
    private readonly getTenantByIdHandler: GetTenantByIdHandler,
    private readonly listTenantsHandler: ListTenantsHandler,
  ) {}

  onModuleInit() {
    this.commandBus.register('CreateTenantCommand', this.createTenantHandler.execute.bind(this.createTenantHandler));
    this.commandBus.register('UpdateTenantCommand', this.updateTenantHandler.execute.bind(this.updateTenantHandler));
    this.commandBus.register('DeleteTenantCommand', this.deleteTenantHandler.execute.bind(this.deleteTenantHandler));
    this.queryBus.register('GetTenantByIdQuery', this.getTenantByIdHandler.execute.bind(this.getTenantByIdHandler));
    this.queryBus.register('ListTenantsQuery', this.listTenantsHandler.execute.bind(this.listTenantsHandler));
  }
}

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [defaultConfig],
    }),
    MikroOrmModule.forRoot({
      driver: BetterSqliteDriver,
      dbName: ':memory:',
      entities: [Tenant, AuditLog],
      debug: true,
      allowGlobalContext: true,
    }),
    CqrsModule,
    TenantsModule,
  ],
  providers: [
    CreateTenantHandler,
    UpdateTenantHandler,
    DeleteTenantHandler,
    GetTenantByIdHandler,
    ListTenantsHandler,
    CqrsRegistrationService,
    AuditLogService,
    {
      provide: APP_INTERCEPTOR,
      useClass: AuditLogInterceptor,
    },
  ],
})
export class TestAppModuleE2E implements OnModuleInit {
  constructor(private readonly orm: MikroORM) {}

  async onModuleInit(): Promise<void> {
    await this.orm.getSchemaGenerator().ensureDatabase();
    await this.orm.getSchemaGenerator().updateSchema();
  }
} 