import { SetMetadata } from '@nestjs/common';
import { PermissionCheckerFn } from '../types/permission-checker.type';

/**
 * Key for the permission checker metadata
 */
export const PERMISSION_CHECKER = 'permission_checker';

/**
 * Decorator for checking permissions in route handlers
 * 
 * @example
 * ```typescript
 * @CheckPermissions((ability) => ability.can(Action.Read, 'tenant'))
 * @Get()
 * findAll() {
 *   return this.tenantsService.findAll();
 * }
 * ```
 * 
 * @param permissionCheckerFn Function that checks if the user has the required permissions
 */
export const CheckPermissions = (permissionCheckerFn: PermissionCheckerFn) =>
  SetMetadata(PERMISSION_CHECKER, permissionCheckerFn); 