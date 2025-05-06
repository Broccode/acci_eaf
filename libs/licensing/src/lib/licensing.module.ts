import { Module, Inject } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { ConfigModule } from '@nestjs/config';
import { LicenseValidationService } from './license-validation.service';
import { ValidateLicenseQueryHandler } from './queries/validate-license-query.handler';
import { QUERY_BUS, EVENT_BUS } from 'apps/control-plane-api/src/app/app.module';

@Module({
  imports: [HttpModule, ConfigModule],
  providers: [
    LicenseValidationService,
    {
      provide: ValidateLicenseQueryHandler,
      useFactory: (
        validationService: LicenseValidationService,
        eventBus: any,
      ) => new ValidateLicenseQueryHandler(validationService, eventBus),
      inject: [LicenseValidationService, EVENT_BUS],
    },
  ],
  exports: [LicenseValidationService],
})
export class LicensingModule {
  constructor(
    @Inject(QUERY_BUS) private readonly queryBus: any,
    @Inject(ValidateLicenseQueryHandler) private readonly handler: ValidateLicenseQueryHandler,
  ) {
    this.queryBus.register('ValidateLicenseQuery', this.handler.execute.bind(this.handler));
  }
}
