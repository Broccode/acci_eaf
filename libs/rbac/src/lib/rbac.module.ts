import { Module } from '@nestjs/common';
import { CaslAbilityFactory } from './casl-ability.factory';
import { PermissionsGuard } from './guards/permissions.guard';
import { UserPermissionsService } from './services/user-permissions.service';

@Module({
  controllers: [],
  providers: [CaslAbilityFactory, PermissionsGuard, UserPermissionsService],
  exports: [CaslAbilityFactory, PermissionsGuard, UserPermissionsService],
})
export class RbacModule {}
