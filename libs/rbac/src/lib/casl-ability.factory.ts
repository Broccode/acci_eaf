import { Injectable } from '@nestjs/common';
import { Ability, AbilityBuilder, AbilityClass, ExtractSubjectType, InferSubjects } from '@casl/ability';
import { Action, CaslSubject, Permission, UserPermissions } from './types';

export type AppAbility = Ability<[Action, CaslSubject]>;

@Injectable()
export class CaslAbilityFactory {
  /**
   * Creates an Ability instance for a user based on their permissions
   * 
   * @param userPermissions The user's permissions (roles and direct permissions)
   * @returns An Ability instance that can be used to check permissions
   */
  createForUser(userPermissions: UserPermissions): AppAbility {
    const { can, cannot, build } = new AbilityBuilder<AppAbility>(
      Ability as AbilityClass<AppAbility>,
    );

    // Apply direct permissions first
    if (userPermissions.directPermissions) {
      this.applyPermissions(can, cannot, userPermissions.directPermissions);
    }

    // Then apply permissions from roles
    for (const role of userPermissions.roles) {
      this.applyPermissions(can, cannot, role.permissions);
    }

    return build({
      // Read https://casl.js.org/v6/en/guide/subject-type-detection for details
      detectSubjectType: (item) => 
        item.constructor as ExtractSubjectType<CaslSubject>,
    });
  }

  /**
   * Apply a set of permissions to an ability builder
   * 
   * @param can The 'can' function from AbilityBuilder
   * @param cannot The 'cannot' function from AbilityBuilder
   * @param permissions List of permissions to apply
   */
  private applyPermissions(
    can: any, 
    cannot: any, 
    permissions: Permission[]
  ): void {
    for (const permission of permissions) {
      // Use 'cannot' for inverted permissions, 'can' for normal permissions
      const method = permission.inverted ? cannot : can;
      
      // If there are conditions, apply them
      if (permission.condition) {
        method(permission.action, permission.resource, permission.condition);
      } 
      // If there are specific attributes, restrict to those
      else if (permission.attributes && permission.attributes.length > 0) {
        method(permission.action, permission.resource, permission.attributes);
      } 
      // Otherwise, grant the permission without restrictions
      else {
        method(permission.action, permission.resource);
      }
    }
  }
} 