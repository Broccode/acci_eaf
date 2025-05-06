import { InMemoryQueryBus, InMemoryEventBus } from 'infrastructure';
import { ValidateLicenseQueryHandler } from './validate-license-query.handler';
import { LicenseValidationService } from '../license-validation.service';
import { ValidateLicenseQuery } from './validate-license.query';
import { LicenseValidatedEvent } from '../events/license-validated.event';

describe('ValidateLicenseQueryHandler Integration', () => {
  it('should execute handler via QueryBus and publish event', async () => {
    const validationService = {
      validateOnline: jest.fn().mockResolvedValue(true),
      validateOffline: jest.fn(),
    } as any;
    const eventBus = new InMemoryEventBus();
    const handler = new ValidateLicenseQueryHandler(validationService, eventBus);

    // Event-Listener für Test
    const published: LicenseValidatedEvent[] = [];
    eventBus.register('LicenseValidatedEvent', async (event) => {
      published.push(event as LicenseValidatedEvent);
    });

    // QueryBus Setup
    const queryBus = new InMemoryQueryBus();
    queryBus.register('ValidateLicenseQuery', handler.execute.bind(handler));

    // Query ausführen
    const dto = { licenseKey: 'abc' } as any;
    const result = await queryBus.execute({ ...new ValidateLicenseQuery(dto), type: 'ValidateLicenseQuery' });

    expect(result).toEqual({ valid: true, message: undefined });
    expect(published.length).toBe(1);
    expect(published[0]).toBeInstanceOf(LicenseValidatedEvent);
    expect(published[0].valid).toBe(true);
  });
}); 