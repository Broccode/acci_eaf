import { Injectable, CanActivate, ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { PERMISSION_CHECKER } from '../decorators/permission-checker.decorator';
import { CaslAbilityFactory } from '../casl-ability.factory';
import { UserPermissionsService } from '../services/user-permissions.service';
import { PermissionCheckerFn } from '../types/permission-checker.type';

@Injectable()
export class PermissionsGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    private caslAbilityFactory: CaslAbilityFactory,
    private userPermissionsService: UserPermissionsService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    // Get the permission checker function from the handler/controller
    const permissionChecker = this.reflector.get<PermissionCheckerFn>(
      PERMISSION_CHECKER,
      context.getHandler(),
    );

    // If no permission checker is defined, allow access
    if (!permissionChecker) {
      return true;
    }

    // Get the request from the context
    const request = context.switchToHttp().getRequest();
    const user = request.user;

    // If no user is present, deny access
    if (!user) {
      throw new ForbiddenException('No user found in request');
    }

    // Get user permissions
    const userPermissions = await this.userPermissionsService.getUserPermissions(
      user.id,
      user.tenantId,
    );

    // Create the ability for this user
    const ability = this.caslAbilityFactory.createForUser(userPermissions);

    // Use the permission checker to determine if access is allowed
    return permissionChecker(ability, request);
  }
} 