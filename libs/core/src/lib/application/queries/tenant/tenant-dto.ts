import { TenantStatus } from '../../../domain/tenant/tenant-status.enum';

/**
 * Data transfer object for tenant information
 */
export interface TenantDto {
  id: string;
  name: string;
  description?: string;
  status: TenantStatus;
  configuration?: Record<string, unknown>;
  contactEmail?: string;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Response for paginated tenant queries
 */
export interface PaginatedTenantsResponse {
  items: TenantDto[];
  total: number;
  page: number;
  limit: number;
  pageCount: number;
} 