import { Global, Module, Injectable, OnModuleInit, Inject } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { APP_GUARD, APP_INTERCEPTOR } from '@nestjs/core';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { TenantsModule } from './tenants/tenants.module';
import { AuthModule } from './auth/auth.module';
import { HealthModule } from './health/health.module';
import { LicensesModule } from './licenses/licenses.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import defaultConfig from './config/default.config';
import { PostgreSqlDriver } from '@mikro-orm/postgresql';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';
import { InMemoryCommandBus, InMemoryQueryBus } from 'infrastructure';
import { CreateTenantHandler } from './commands/handlers/create-tenant.handler';
import { UpdateTenantHandler } from './commands/handlers/update-tenant.handler';
import { DeleteTenantHandler } from './commands/handlers/delete-tenant.handler';
import { GetTenantByIdHandler } from './queries/handlers/get-tenant-by-id.handler';
import { ListTenantsHandler } from './queries/handlers/list-tenants.handler';
import { CqrsModule } from './cqrs.module';
import { AuditLogInterceptor } from './audit/audit-log.interceptor';
import { AuditLogService } from './audit/audit-log.service';
import { ThrottlerModule, ThrottlerGuard } from '@nestjs/throttler';
import { ValidateLicenseQueryHandler } from 'licensing';
import { COMMAND_BUS, QUERY_BUS, EVENT_BUS } from 'core';

@Injectable()
class CqrsRegistrationService implements OnModuleInit {
  constructor(
    @Inject(COMMAND_BUS) private readonly commandBus: InMemoryCommandBus,
    @Inject(QUERY_BUS) private readonly queryBus: InMemoryQueryBus,
    private readonly createTenantHandler: CreateTenantHandler,
    private readonly updateTenantHandler: UpdateTenantHandler,
    private readonly deleteTenantHandler: DeleteTenantHandler,
    private readonly getTenantByIdHandler: GetTenantByIdHandler,
    private readonly listTenantsHandler: ListTenantsHandler,
    private readonly validateLicenseQueryHandler: ValidateLicenseQueryHandler,
  ) {}

  onModuleInit() {
    this.commandBus.register('CreateTenantCommand', this.createTenantHandler.execute.bind(this.createTenantHandler));
    this.commandBus.register('UpdateTenantCommand', this.updateTenantHandler.execute.bind(this.updateTenantHandler));
    this.commandBus.register('DeleteTenantCommand', this.deleteTenantHandler.execute.bind(this.deleteTenantHandler));
    this.queryBus.register('GetTenantByIdQuery', this.getTenantByIdHandler.execute.bind(this.getTenantByIdHandler));
    this.queryBus.register('ListTenantsQuery', this.listTenantsHandler.execute.bind(this.listTenantsHandler));
    this.queryBus.register(
      'ValidateLicenseQuery',
      this.validateLicenseQueryHandler.execute.bind(this.validateLicenseQueryHandler),
    );
  }
}

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [defaultConfig],
    }),
    MikroOrmModule.forRoot({
      // Use environment variables to determine database type
      driver: process.env['DB_TYPE'] === 'better-sqlite' ? BetterSqliteDriver : PostgreSqlDriver,
      host: process.env['DB_HOST'] || 'localhost',
      port: process.env['DB_PORT'] ? parseInt(process.env['DB_PORT'], 10) : 5432,
      user: process.env['DB_USER'] || 'postgres',
      password: process.env['DB_PASSWORD'] || 'postgres',
      dbName: process.env['DB_NAME'] || 'acci_eaf',
      debug: process.env['NODE_ENV'] !== 'production' && process.env['NODE_ENV'] !== 'test',
      registerRequestContext: true, // Required for repository pattern usage
      allowGlobalContext: true,
      autoLoadEntities: true,
    }),
    CqrsModule,
    TenantsModule,
    AuthModule,
    HealthModule,
    LicensesModule,
    ThrottlerModule.forRootAsync({
      imports: [ConfigModule],
      inject: [ConfigService],
      useFactory: (config: ConfigService) => ({
        throttlers: [
          {
            ttl: config.get<number>('THROTTLE_TTL', 60),
            limit: config.get<number>('THROTTLE_LIMIT', 10),
          },
        ],
      }),
    }),
  ],
  controllers: [AppController],
  providers: [
    AppService,
    CreateTenantHandler,
    UpdateTenantHandler,
    DeleteTenantHandler,
    GetTenantByIdHandler,
    ListTenantsHandler,
    CqrsRegistrationService,
    AuditLogService,
    {
      provide: APP_GUARD,
      useClass: JwtAuthGuard,
    },
    {
      provide: APP_INTERCEPTOR,
      useClass: AuditLogInterceptor,
    },
    {
      provide: APP_GUARD,
      useClass: ThrottlerGuard,
    },
  ],
})
export class AppModule {}
