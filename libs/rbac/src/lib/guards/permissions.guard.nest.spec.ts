import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { NestUnitTestHelper } from 'testing';
import { PermissionsGuard } from './permissions.guard';
import { CaslAbilityFactory, AppAbility } from '../casl-ability.factory';
import { UserPermissionsService } from '../services/user-permissions.service';
import { PERMISSION_CHECKER } from '../decorators/permission-checker.decorator';
import { UserPermissions, Action } from '../types';

// Helper to build an ExecutionContext mock
function createMockExecutionContext(requestPartial = {}): ExecutionContext {
  const request = {
    user: { id: 'user-1', tenantId: 'tenant-1' },
    ...requestPartial,
  };

  return {
    switchToHttp: () => ({ getRequest: () => request }),
    getHandler: () => jest.fn(),
    getClass: () => jest.fn(),
  } as unknown as ExecutionContext;
}

describe('PermissionsGuard – complex scenarios (NestUnitTestHelper)', () => {
  let guard: PermissionsGuard;
  let reflector: Reflector;
  let caslAbilityFactory: jest.Mocked<CaslAbilityFactory>;
  let userPermissionsService: jest.Mocked<UserPermissionsService>;

  beforeEach(async () => {
    // Build testing module with NestUnitTestHelper
    const { service, module } = await NestUnitTestHelper.createTestingModule({
      providers: [
        PermissionsGuard,
        Reflector,
        {
          provide: CaslAbilityFactory,
          useValue: { createForUser: jest.fn() },
        },
        {
          provide: UserPermissionsService,
          useValue: { getUserPermissions: jest.fn() },
        },
      ],
    }).compile(PermissionsGuard);

    guard = service;
    reflector = module.get(Reflector);
    caslAbilityFactory = module.get(CaslAbilityFactory);
    userPermissionsService = module.get(UserPermissionsService);
  });

  it('should allow access when ability matches complex rule', async () => {
    // Arrange
    const ability: AppAbility = { can: jest.fn().mockReturnValue(true) } as any;
    const permissionChecker = jest
      .fn()
      .mockImplementation((ab: AppAbility) => ab.can(Action.Update, 'tenant' as any));

    jest.spyOn(reflector, 'get').mockReturnValue(permissionChecker);
    (userPermissionsService.getUserPermissions as jest.Mock).mockResolvedValue({} as UserPermissions);
    (caslAbilityFactory.createForUser as jest.Mock).mockReturnValue(ability);

    const ctx = createMockExecutionContext();

    // Act + Assert
    await expect(guard.canActivate(ctx)).resolves.toBe(true);
    expect(permissionChecker).toHaveBeenCalledWith(ability, expect.objectContaining({ user: ctx.switchToHttp().getRequest().user }));
  });

  it('should deny access when ability check fails', async () => {
    // Arrange
    const ability: AppAbility = { can: jest.fn().mockReturnValue(false) } as any;
    const permissionChecker = jest.fn().mockReturnValue(false);

    jest.spyOn(reflector, 'get').mockReturnValue(permissionChecker);
    (userPermissionsService.getUserPermissions as jest.Mock).mockResolvedValue({} as UserPermissions);
    (caslAbilityFactory.createForUser as jest.Mock).mockReturnValue(ability);

    const ctx = createMockExecutionContext();

    // Act
    const result = await guard.canActivate(ctx);

    // Assert
    expect(result).toBe(false);
  });

  it('should throw ForbiddenException when request has no user (unauthenticated)', async () => {
    const permissionChecker = jest.fn();
    jest.spyOn(reflector, 'get').mockReturnValue(permissionChecker);

    const ctx = createMockExecutionContext({ user: undefined });

    await expect(guard.canActivate(ctx)).rejects.toThrow(ForbiddenException);
  });
}); 