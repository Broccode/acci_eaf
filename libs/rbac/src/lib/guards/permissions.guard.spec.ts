import { ExecutionContext, ForbiddenException } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { TestBed, Mocked } from '@suites/unit';
import { PermissionsGuard } from './permissions.guard';
import { CaslAbilityFactory, AppAbility } from '../casl-ability.factory';
import { UserPermissionsService } from '../services/user-permissions.service';
import { PERMISSION_CHECKER } from '../decorators/permission-checker.decorator';
import { Action, UserPermissions } from '../types';

describe('PermissionsGuard', () => {
  let underTest: PermissionsGuard;
  let reflector: Mocked<Reflector>;
  let caslAbilityFactory: Mocked<CaslAbilityFactory>;
  let userPermissionsService: Mocked<UserPermissionsService>;

  beforeAll(async () => {
    // Create a solitary test environment for PermissionsGuard
    const { unit, unitRef } = await TestBed.solitary(PermissionsGuard).compile();

    underTest = unit;
    
    // Get access to the mocked dependencies
    reflector = unitRef.get(Reflector);
    caslAbilityFactory = unitRef.get(CaslAbilityFactory);
    userPermissionsService = unitRef.get(UserPermissionsService);
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  describe('canActivate', () => {
    it('should allow access when no permission checker is defined', async () => {
      // Arrange
      reflector.get.mockReturnValue(undefined);
      const context = createMockExecutionContext();

      // Act
      const result = await underTest.canActivate(context);

      // Assert
      expect(result).toBe(true);
      expect(reflector.get).toHaveBeenCalledWith(PERMISSION_CHECKER, expect.any(Function));
    });

    it('should throw ForbiddenException when no user is present in request', async () => {
      // Arrange
      const permissionCheckerFn = jest.fn();
      reflector.get.mockReturnValue(permissionCheckerFn);
      
      const context = createMockExecutionContext({
        user: undefined,
      });

      // Act & Assert
      await expect(underTest.canActivate(context)).rejects.toThrow(ForbiddenException);
      expect(reflector.get).toHaveBeenCalledWith(PERMISSION_CHECKER, expect.any(Function));
    });

    it('should check permissions with the provided checker function', async () => {
      // Arrange
      const mockAbility = { can: jest.fn() } as unknown as AppAbility;
      const user = { id: 'user-1', tenantId: 'tenant-1' };
      const userPermissions: UserPermissions = { 
        userId: user.id, 
        tenantId: user.tenantId, 
        roles: [] 
      };
      const permissionCheckerFn = jest.fn().mockReturnValue(true);
      
      reflector.get.mockReturnValue(permissionCheckerFn);
      userPermissionsService.getUserPermissions.mockResolvedValue(userPermissions);
      caslAbilityFactory.createForUser.mockReturnValue(mockAbility);
      
      const context = createMockExecutionContext({ user });

      // Act
      const result = await underTest.canActivate(context);

      // Assert
      expect(result).toBe(true);
      expect(reflector.get).toHaveBeenCalledWith(PERMISSION_CHECKER, expect.any(Function));
      expect(userPermissionsService.getUserPermissions).toHaveBeenCalledWith(user.id, user.tenantId);
      expect(caslAbilityFactory.createForUser).toHaveBeenCalledWith(userPermissions);
      expect(permissionCheckerFn).toHaveBeenCalledWith(mockAbility, expect.objectContaining({ user }));
    });

    it('should deny access when permission checker returns false', async () => {
      // Arrange
      const mockAbility = { can: jest.fn() } as unknown as AppAbility;
      const user = { id: 'user-1', tenantId: 'tenant-1' };
      const userPermissions: UserPermissions = { 
        userId: user.id, 
        tenantId: user.tenantId, 
        roles: [] 
      };
      const permissionCheckerFn = jest.fn().mockReturnValue(false);
      
      reflector.get.mockReturnValue(permissionCheckerFn);
      userPermissionsService.getUserPermissions.mockResolvedValue(userPermissions);
      caslAbilityFactory.createForUser.mockReturnValue(mockAbility);
      
      const context = createMockExecutionContext({ user });

      // Act
      const result = await underTest.canActivate(context);

      // Assert
      expect(result).toBe(false);
    });
  });
});

// Helper function to create a mock execution context
function createMockExecutionContext(requestPartial = {}): ExecutionContext {
  const request = {
    user: { id: 'user-id', tenantId: 'tenant-id' },
    ...requestPartial,
  };

  return {
    switchToHttp: () => ({
      getRequest: () => request,
    }),
    getHandler: () => jest.fn(),
    getClass: () => jest.fn(),
  } as unknown as ExecutionContext;
} 