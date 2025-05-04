export class ListTenantsQuery {
  readonly type = 'ListTenantsQuery';

  constructor(public readonly activeOnly: boolean = false) {}
} 