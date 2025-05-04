export class UpdateTenantCommand {
  readonly type = 'UpdateTenantCommand';

  constructor(
    public readonly id: string,
    public readonly name?: string,
    public readonly description?: string,
    public readonly contactEmail?: string,
    public readonly configuration?: Record<string, unknown>,
  ) {}
} 