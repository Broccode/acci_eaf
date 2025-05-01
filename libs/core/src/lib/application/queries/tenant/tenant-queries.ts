/**
 * Query to get a tenant by ID
 */
export class GetTenantByIdQuery {
  constructor(public readonly id: string) {}
}

/**
 * Query to get a tenant by name
 */
export class GetTenantByNameQuery {
  constructor(public readonly name: string) {}
}

/**
 * Query to get all tenants
 */
export class GetAllTenantsQuery {
  constructor(
    public readonly page: number = 1,
    public readonly limit: number = 10
  ) {}
} 