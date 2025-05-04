/**
 * Base interface for all subjects (entities) that can have permissions applied to them
 */
export interface CaslSubject {
  [key: string]: any;
}

/**
 * Definition of all possible actions that can be performed on resources
 */
export enum Action {
  Manage = 'manage', // Wildcard - can do any action
  Create = 'create',
  Read = 'read',
  Update = 'update',
  Delete = 'delete',
  Execute = 'execute', // For operations
}

/**
 * Resource is any entity in the system that can be protected
 * This type should be extended when new resources are added
 */
export type Resource = 
  | 'tenant'
  | 'user'
  | 'role'
  | 'permission'
  | 'all'; // 'all' is a special resource that represents all resources

/**
 * Ownership context to be used for ownership-based permissions
 */
export interface OwnershipContext {
  userId: string;
  tenantId: string;
}

/**
 * Permission model - represents a single permission
 */
export interface Permission {
  id?: string;
  action: Action | string;
  resource: Resource | string;
  attributes?: string[]; // Specific attributes that can be accessed
  condition?: any; // Condition under which this permission applies
  inverted?: boolean; // If true, this permission denies rather than allows
}

/**
 * Role model - represents a collection of permissions
 */
export interface Role {
  id?: string;
  name: string;
  description?: string;
  permissions: Permission[];
  tenantId?: string; // Multi-tenant awareness
}

/**
 * User permission set - represents all permissions a user has
 */
export interface UserPermissions {
  userId: string;
  tenantId?: string;
  roles: Role[]; // Roles assigned to the user
  directPermissions?: Permission[]; // Permissions assigned directly to the user
} 