export class DeleteTenantCommand {
  readonly type = 'DeleteTenantCommand';

  constructor(public readonly id: string) {}
} 