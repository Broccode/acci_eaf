import { Module } from '@nestjs/common';
import { LicensingModule } from 'licensing';
import { LicensesController } from './licenses.controller';

@Module({
  imports: [LicensingModule],
  controllers: [LicensesController],
})
export class LicensesModule {} 