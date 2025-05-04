import { Request } from 'express';
import { AppAbility } from '../casl-ability.factory';

/**
 * Function type for checking permissions in route handlers
 * Used with the @CheckPermissions decorator
 * 
 * @param ability The CASL ability instance for the current user
 * @param request The HTTP request object
 * @returns true if the request should be allowed, false otherwise
 */
export type PermissionCheckerFn = (
  ability: AppAbility,
  request: Request,
) => boolean; 