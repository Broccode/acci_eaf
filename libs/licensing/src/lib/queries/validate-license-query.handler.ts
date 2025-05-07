import { ValidateLicenseQuery } from './validate-license.query';
import { LicenseValidationService } from '../license-validation.service';
import { LicenseValidatedEvent } from '../events/license-validated.event';
import { EventBus, EVENT_BUS } from 'core';
import { Inject } from '@nestjs/common';

export class ValidateLicenseQueryHandler {
  constructor(
    private readonly validationService: LicenseValidationService,
    @Inject(EVENT_BUS) private readonly eventBus: EventBus,
  ) {}

  async execute(query: ValidateLicenseQuery): Promise<{ valid: boolean; message?: string }> {
    let valid = false;
    let message = undefined;
    try {
      if (query.dto.licenseKey) {
        valid = await this.validationService.validateOnline(query.dto.licenseKey);
      } else if (query.dto.licenseFile) {
        valid = await this.validationService.validateOffline(query.dto.licenseFile);
      } else {
        message = 'No license data provided';
      }
    } catch (err) {
      if (err instanceof Error) {
        message = err.message;
      } else {
        message = 'Unknown error';
      }
    }
    await this.eventBus.publish(new LicenseValidatedEvent(query.dto, valid, message));
    return { valid, message };
  }
} 