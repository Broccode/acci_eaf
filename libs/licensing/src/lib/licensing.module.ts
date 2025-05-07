import { Module, Inject } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { ConfigModule } from '@nestjs/config';
import { LicenseValidationService } from './license-validation.service';
import { ValidateLicenseQueryHandler } from './queries/validate-license-query.handler';

@Module({
  imports: [HttpModule, ConfigModule],
  providers: [LicenseValidationService, ValidateLicenseQueryHandler],
  exports: [LicenseValidationService, ValidateLicenseQueryHandler],
})
export class LicensingModule {}
