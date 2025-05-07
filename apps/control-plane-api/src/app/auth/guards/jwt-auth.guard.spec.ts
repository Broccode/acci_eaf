import { TestBed, Mocked } from '@suites/unit';
import { JwtAuthGuard } from './jwt-auth.guard';
import { Reflector } from '@nestjs/core';
import { ExecutionContext } from '@nestjs/common';
// AuthGuard might still be needed if we were spying on AuthGuard('jwt').prototype directly,
// but Object.getPrototypeOf(JwtAuthGuard.prototype) is more robust for super calls.

describe('JwtAuthGuard', () => {
  let underTest: JwtAuthGuard;
  let mockedReflector: Mocked<Reflector>;

  // Mock ExecutionContext - this helper function remains largely the same
  const mockExecutionContext = (isPublicValue?: boolean) => {
    const mockGetHandler = jest.fn();
    const mockGetClass = jest.fn();

    return {
      switchToHttp: jest.fn().mockReturnThis(),
      getRequest: jest.fn().mockReturnThis(),
      getHandler: mockGetHandler,
      getClass: mockGetClass,
    } as unknown as ExecutionContext;
  };

  beforeEach(async () => {
    // Use TestBed.solitary for JwtAuthGuard
    const { unit, unitRef } = await TestBed.solitary(JwtAuthGuard).compile();
    
    underTest = unit;
    mockedReflector = unitRef.get(Reflector); // Get the auto-mocked Reflector
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  describe('canActivate', () => {
    it('should return true if isPublic is true', () => {
      // Arrange
      const context = mockExecutionContext(true);
      // Configure the auto-mocked reflector
      mockedReflector.getAllAndOverride.mockReturnValue(true);

      // Act
      const result = underTest.canActivate(context);

      // Assert
      expect(result).toBe(true);
      expect(mockedReflector.getAllAndOverride).toHaveBeenCalledWith('isPublic', [
        context.getHandler(),
        context.getClass(),
      ]);
    });

    it('should call and return super.canActivate if isPublic is false', async () => {
      // Arrange
      const context = mockExecutionContext(false);
      mockedReflector.getAllAndOverride.mockReturnValue(false);

      const expectedSuperResult = true; // Example: parent guard allows access
      // Spy on the parent's canActivate method
      const parentCanActivateSpy = jest
        .spyOn(Object.getPrototypeOf(JwtAuthGuard.prototype), 'canActivate')
        .mockReturnValue(expectedSuperResult);

      // Act
      const result = underTest.canActivate(context);

      // Assert
      expect(mockedReflector.getAllAndOverride).toHaveBeenCalledWith('isPublic', [
        context.getHandler(),
        context.getClass(),
      ]);
      expect(parentCanActivateSpy).toHaveBeenCalledWith(context);
      
      if (typeof result === 'boolean') {
        expect(result).toBe(expectedSuperResult);
      } else if (result instanceof Promise) {
        await expect(result).resolves.toBe(expectedSuperResult);
      } else {
        // Handle Observable case if necessary
        expect(result).toBe(expectedSuperResult); 
      }
      parentCanActivateSpy.mockRestore();
    });

    it('should call and return super.canActivate when isPublic is not set (undefined)', async () => {
      // Arrange
      const context = mockExecutionContext(undefined); 
      mockedReflector.getAllAndOverride.mockReturnValue(undefined); // isPublic is not set
      
      const expectedSuperResult = false; // Example: parent guard denies access
      const parentCanActivateSpy = jest
        .spyOn(Object.getPrototypeOf(JwtAuthGuard.prototype), 'canActivate')
        .mockReturnValue(expectedSuperResult);

      // Act
      const result = underTest.canActivate(context);

      // Assert
      expect(mockedReflector.getAllAndOverride).toHaveBeenCalledWith('isPublic', [
        context.getHandler(),
        context.getClass(),
      ]);
      expect(parentCanActivateSpy).toHaveBeenCalledWith(context);
      
      if (typeof result === 'boolean') {
        expect(result).toBe(expectedSuperResult);
      } else if (result instanceof Promise) {
        await expect(result).resolves.toBe(expectedSuperResult);
      } else {
        expect(result).toBe(expectedSuperResult);
      }
      parentCanActivateSpy.mockRestore();
    });
  });
}); 