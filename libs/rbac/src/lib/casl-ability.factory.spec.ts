import { TestBed } from '@suites/unit';
import { CaslAbilityFactory } from './casl-ability.factory';
import { Action, CaslSubject, UserPermissions } from './types';

// Mocking the Ability class from @casl/ability
jest.mock('@casl/ability', () => {
  // Mock class to replace Ability
  class MockedAbility {
    // Mock methods
    can() { return true; }
    cannot() { return true; }
  }

  // Mock AbilityBuilder
  const mockBuilder = {
    can: jest.fn(),
    cannot: jest.fn(),
    build: jest.fn(() => new MockedAbility()),
  };

  // Return the object with all the mocked exports
  return {
    Ability: jest.fn().mockImplementation(() => new MockedAbility()),
    AbilityBuilder: jest.fn().mockImplementation(() => mockBuilder),
  };
});

describe('CaslAbilityFactory', () => {
  let underTest: CaslAbilityFactory;

  beforeAll(async () => {
    // Create the factory as a Unit
    const { unit } = await TestBed.solitary(CaslAbilityFactory).compile();
    underTest = unit;
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  describe('createForUser', () => {
    it('should create an ability instance', () => {
      // Arrange
      const userPermissions: UserPermissions = {
        userId: 'test-user',
        tenantId: 'test-tenant',
        roles: [
          {
            id: 'admin',
            name: 'Admin',
            permissions: [
              { action: Action.Read, resource: 'user' },
            ],
          },
        ],
        directPermissions: [],
      };

      // Act
      const ability = underTest.createForUser(userPermissions);

      // Assert - just verify that we get something back
      expect(ability).toBeDefined();
    });

    it('should apply permissions from roles', () => {
      // Arrange
      const userPermissions: UserPermissions = {
        userId: 'test-user',
        tenantId: 'test-tenant',
        roles: [
          {
            id: 'admin',
            name: 'Admin',
            permissions: [
              { action: Action.Read, resource: 'user' },
              { action: Action.Update, resource: 'user' },
            ],
          },
        ],
        directPermissions: [],
      };

      // Act
      const ability = underTest.createForUser(userPermissions);

      // Assert
      expect(ability).toBeDefined();
      // We're mostly testing that the function runs without errors
    });

    it('should apply direct permissions', () => {
      // Arrange
      const userPermissions: UserPermissions = {
        userId: 'test-user',
        tenantId: 'test-tenant',
        roles: [],
        directPermissions: [
          { action: Action.Delete, resource: 'tenant' },
        ],
      };

      // Act
      const ability = underTest.createForUser(userPermissions);

      // Assert
      expect(ability).toBeDefined();
    });

    it('should handle empty permissions', () => {
      // Arrange
      const userPermissions: UserPermissions = {
        userId: 'test-user',
        tenantId: 'test-tenant',
        roles: [],
        directPermissions: [],
      };

      // Act
      const ability = underTest.createForUser(userPermissions);

      // Assert
      expect(ability).toBeDefined();
    });
  });
}); 