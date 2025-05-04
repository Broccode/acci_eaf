export class CreateTenantCommand {
  readonly type = 'CreateTenantCommand';

  constructor(
    public readonly name: string,
    public readonly description?: string,
    public readonly contactEmail?: string,
    public readonly configuration?: Record<string, unknown>,
  ) {}
} 