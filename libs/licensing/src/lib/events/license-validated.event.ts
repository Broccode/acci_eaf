import { ValidateLicenseDto } from '../dtos/validate-license.dto';

export class LicenseValidatedEvent {
  public readonly type = 'LicenseValidatedEvent';
  constructor(
    public readonly dto: ValidateLicenseDto,
    public readonly valid: boolean,
    public readonly message?: string,
  ) {}
} 