import { ValidateLicenseQueryHandler } from './validate-license-query.handler';
import { LicenseValidationService } from '../license-validation.service';
import { EventBus } from 'core';
import { ValidateLicenseQuery } from './validate-license.query';
import { LicenseValidatedEvent } from '../events/license-validated.event';

describe('ValidateLicenseQueryHandler', () => {
  let handler: ValidateLicenseQueryHandler;
  let validationService: jest.Mocked<LicenseValidationService>;
  let eventBus: { publish: jest.Mock };

  beforeEach(() => {
    validationService = {
      validateOnline: jest.fn(),
      validateOffline: jest.fn(),
    } as any;
    eventBus = { publish: jest.fn() };
    handler = new ValidateLicenseQueryHandler(validationService, eventBus as any);
  });

  it('should validate online and publish event', async () => {
    validationService.validateOnline.mockResolvedValue(true);
    const dto = { licenseKey: 'abc' } as any;
    const query = new ValidateLicenseQuery(dto);
    const result = await handler.execute(query);
    expect(validationService.validateOnline).toHaveBeenCalledWith('abc');
    expect(eventBus.publish).toHaveBeenCalledWith(expect.any(LicenseValidatedEvent));
    expect(result).toEqual({ valid: true, message: undefined });
  });

  it('should validate offline and publish event', async () => {
    validationService.validateOffline.mockResolvedValue(false);
    const dto = { licenseFile: 'xyz' } as any;
    const query = new ValidateLicenseQuery(dto);
    const result = await handler.execute(query);
    expect(validationService.validateOffline).toHaveBeenCalledWith('xyz');
    expect(eventBus.publish).toHaveBeenCalledWith(expect.any(LicenseValidatedEvent));
    expect(result).toEqual({ valid: false, message: undefined });
  });

  it('should handle errors and publish event', async () => {
    validationService.validateOnline.mockRejectedValue(new Error('fail'));
    const dto = { licenseKey: 'abc' } as any;
    const query = new ValidateLicenseQuery(dto);
    const result = await handler.execute(query);
    expect(result).toEqual({ valid: false, message: 'fail' });
    expect(eventBus.publish).toHaveBeenCalledWith(expect.any(LicenseValidatedEvent));
  });
}); 