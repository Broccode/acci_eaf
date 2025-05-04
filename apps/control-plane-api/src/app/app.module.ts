import { Module, OnModuleInit, Inject } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { ConfigModule } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { TenantsModule } from './tenants/tenants.module';
import { AuthModule } from './auth/auth.module';
import { HealthModule } from './health/health.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import defaultConfig from './config/default.config';
import { PostgreSqlDriver } from '@mikro-orm/postgresql';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';
import { InMemoryCommandBus, InMemoryQueryBus, InMemoryEventBus } from 'infrastructure';
import { CreateTenantHandler } from './commands/handlers/create-tenant.handler';
import { UpdateTenantHandler } from './commands/handlers/update-tenant.handler';
import { DeleteTenantHandler } from './commands/handlers/delete-tenant.handler';
import { GetTenantByIdHandler } from './queries/handlers/get-tenant-by-id.handler';
import { ListTenantsHandler } from './queries/handlers/list-tenants.handler';

// Constants for dependency injection tokens
export const COMMAND_BUS = 'COMMAND_BUS';
export const QUERY_BUS = 'QUERY_BUS';
export const EVENT_BUS = 'EVENT_BUS';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [defaultConfig],
    }),
    MikroOrmModule.forRoot({
      // Use environment variables to determine database type
      driver: process.env.DB_TYPE === 'better-sqlite' ? BetterSqliteDriver : PostgreSqlDriver,
      host: process.env.DB_HOST || 'localhost',
      port: process.env.DB_PORT ? parseInt(process.env.DB_PORT, 10) : 5432,
      user: process.env.DB_USER || 'postgres',
      password: process.env.DB_PASSWORD || 'postgres',
      dbName: process.env.DB_NAME || 'acci_eaf',
      debug: process.env.NODE_ENV !== 'production',
      registerRequestContext: true, // Required for repository pattern usage
      allowGlobalContext: true,
      autoLoadEntities: true,
    }),
    TenantsModule,
    AuthModule,
    HealthModule,
  ],
  controllers: [AppController],
  providers: [
    AppService,
    // Custom CQRS buses
    {
      provide: COMMAND_BUS,
      useFactory: () => {
        return new InMemoryCommandBus();
      }
    },
    {
      provide: QUERY_BUS,
      useFactory: () => {
        return new InMemoryQueryBus();
      }
    },
    {
      provide: EVENT_BUS,
      useFactory: () => {
        return new InMemoryEventBus();
      }
    },
    CreateTenantHandler,
    UpdateTenantHandler,
    DeleteTenantHandler,
    GetTenantByIdHandler,
    ListTenantsHandler,
    {
      provide: APP_GUARD,
      useClass: JwtAuthGuard,
    },
  ],
  exports: [COMMAND_BUS, QUERY_BUS, EVENT_BUS],
})
export class AppModule implements OnModuleInit {
  constructor(
    @Inject(COMMAND_BUS) private readonly commandBus: InMemoryCommandBus,
    @Inject(QUERY_BUS) private readonly queryBus: InMemoryQueryBus,
    private readonly createTenantHandler: CreateTenantHandler,
    private readonly updateTenantHandler: UpdateTenantHandler,
    private readonly deleteTenantHandler: DeleteTenantHandler,
    private readonly getTenantByIdHandler: GetTenantByIdHandler,
    private readonly listTenantsHandler: ListTenantsHandler
  ) {}

  onModuleInit() {
    // Register command handlers with command bus
    this.commandBus.register('CreateTenantCommand', this.createTenantHandler.execute.bind(this.createTenantHandler));
    this.commandBus.register('UpdateTenantCommand', this.updateTenantHandler.execute.bind(this.updateTenantHandler));
    this.commandBus.register('DeleteTenantCommand', this.deleteTenantHandler.execute.bind(this.deleteTenantHandler));

    // Register query handlers with query bus
    this.queryBus.register('GetTenantByIdQuery', this.getTenantByIdHandler.execute.bind(this.getTenantByIdHandler));
    this.queryBus.register('ListTenantsQuery', this.listTenantsHandler.execute.bind(this.listTenantsHandler));
  }
}
