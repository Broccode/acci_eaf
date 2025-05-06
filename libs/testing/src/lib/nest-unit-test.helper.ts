/// <reference types="jest" />

import { Type, ModuleMetadata } from '@nestjs/common';

/**
 * Helper for NestJS unit testing with dependency injection.
 * Uses @nestjs/testing for creating test modules.
 */
export class NestUnitTestHelper {
  /**
   * Creates a testing module with the given providers and imports.
   * Simplifies the process of testing NestJS components with DI.
   * 
   * @param options The module options including providers and imports
   * @returns A function to create a testing module
   * 
   * @example
   * ```typescript
   * const { module, service } = await NestUnitTestHelper.createTestingModule({
   *   providers: [MyService, { provide: MyDependency, useValue: mockDependency }]
   * }).compile(MyService);
   * 
   * expect(service.someMethod()).toBe(expectedResult);
   * ```
   */
  static createTestingModule(options: Pick<ModuleMetadata, 'providers' | 'imports'>) {
    // Create a testing module without using suite() directly
    // based on @nestjs/testing patterns
    return {
      async compile<T>(targetService: Type<T>) {
        // Import dynamically to avoid direct dependency on @suites which might have a different structure
        const { Test } = await import('@nestjs/testing');
        const module = await Test.createTestingModule(options).compile();
        const service = module.get(targetService);
        
        return {
          module,
          service
        };
      }
    };
  }

  /**
   * Creates a mock for a service with all methods as Jest mock functions.
   * 
   * @param service The service class to mock
   * @returns A mocked service with all methods as Jest mock functions
   * 
   * @example
   * ```typescript
   * const mockDependency = NestUnitTestHelper.createMockService(MyDependency);
   * mockDependency.someMethod.mockReturnValue('mocked value');
   * 
   * const { service } = await NestUnitTestHelper.createTestingModule({
   *   providers: [MyService, { provide: MyDependency, useValue: mockDependency }]
   * }).compile(MyService);
   * ```
   */
  static createMockService<T extends object>(service: Type<T>): Record<string, jest.Mock> {
    const prototype = service.prototype;
    const mockService: Record<string, jest.Mock> = {};
    
    // Get all method names from the prototype
    const methodNames = Object.getOwnPropertyNames(prototype)
      .filter(prop => typeof prototype[prop] === 'function' && prop !== 'constructor');
    
    // Create a Jest mock function for each method
    methodNames.forEach(method => {
      mockService[method] = jest.fn();
    });
    
    return mockService as any;
  }
} 