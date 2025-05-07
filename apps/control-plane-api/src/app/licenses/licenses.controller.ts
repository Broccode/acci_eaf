import { Controller, Post, Body, Inject } from '@nestjs/common';
import { ValidateLicenseDto, ValidateLicenseQuery } from 'licensing';
import { QUERY_BUS } from 'core';
import { InMemoryQueryBus } from 'infrastructure';

@Controller('licenses')
export class LicensesController {
  constructor(@Inject(QUERY_BUS) private readonly queryBus: InMemoryQueryBus) {}

  @Post('validate')
  async validate(@Body() dto: ValidateLicenseDto): Promise<{ valid: boolean; message?: string }> {
    return this.queryBus.execute({ ...new ValidateLicenseQuery(dto), type: 'ValidateLicenseQuery' });
  }
} 