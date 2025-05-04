# Testing Guide for control-plane-api

## Introduction to the Suites Testing Framework

This project uses the [Suites Testing Framework](https://suites.dev) for unit tests. Suites is a meta testing framework that integrates jest/vitest and other test tools with dependency injection frameworks such as NestJS. Its main benefits are automatic mocking and a clear separation between "solitary" and "sociable" tests.

## Core Concepts

### Solitary Tests

- Test a single class in complete isolation
- All dependencies are automatically mocked
- Suitable for handlers, controllers, and simple services
- Focus on the logic of the class under test

```typescript
// Example of a solitary test
const { unit, unitRef } = await TestBed.solitary<MyService>(MyService).compile();
```

### Sociable Tests

- Test a class with some real and some mocked dependencies
- Allows testing the interactions between components
- Suitable for more complex services or domain logic

```typescript
// Example of a sociable test
const { unit, unitRef } = await TestBed
  .sociable<MyService>(MyService)
  .expose(DependencyToUseReal)
  .compile();
```

## Migration Checklist

- [x] Command handler tests migrated
  - [x] CreateTenantHandler
  - [x] UpdateTenantHandler
  - [x] DeleteTenantHandler
- [x] Controller tests migrated
  - [x] AppController
- [x] Service tests migrated
  - [x] AppService
  - [x] TenantsService
- [x] Query handler tests migrated
  - [x] ListTenantsHandler
  - [x] GetTenantByIdHandler
- [ ] Migrate additional controllers/services
- [ ] Evaluate integration tests (decision: temporarily keep manual setup method)

## Best Practices

### 1. Naming Conventions

- `underTest` for the class under test
- `{dependencyName}` for mocked dependencies
- AAA pattern: Arrange–Act–Assert with comments

### 2. When to Use Solitary vs. Sociable Tests

| Component | Recommended Approach | Rationale |
|-----------|---------------------|-----------|
| Controller | Solitary | Controllers should be thin and only route/transform requests |
| Command/Query Handler | Solitary | Handlers typically have a single responsibility |
| Services with little logic | Solitary | If the service logic is simple and self-contained |
| Services with complex logic | Sociable | When the service logic heavily interacts with other components |
| Repositories | Case-by-case | Depending on complexity, often tested with TestContainers |

### 3. Typical Test Structure

```typescript
describe('MyClass (Suites)', () => {
  let underTest: MyClass;
  let dependency1: Mocked<Dependency1>;
  let dependency2: Mocked<Dependency2>;

  beforeAll(async () => {
    const { unit, unitRef } = await TestBed.solitary<MyClass>(MyClass).compile();
    
    underTest = unit;
    dependency1 = unitRef.get(Dependency1);
    dependency2 = unitRef.get(Dependency2);
  });

  it('should do something specific', () => {
    // Arrange
    dependency1.method.mockReturnValue(123);
    
    // Act
    const result = underTest.methodToTest();
    
    // Assert
    expect(dependency1.method).toHaveBeenCalled();
    expect(result).toBe(123);
  });
});
```

## Common Patterns

### 1. Mocking Methods with Suites

```typescript
// Define a return value
mockService.getData.mockReturnValue({ id: 1 });
mockService.getData.mockResolvedValue({ id: 1 }); // For promises

// Override the implementation
mockService.getData.mockImplementation((id) => {
  if (id === 1) return { found: true };
  return { found: false };
});

// Throw an error
mockService.getData.mockRejectedValue(new Error('Something went wrong'));
```

### 2. Precise Verification of Calls

```typescript
// Verify that the method was called
expect(mockService.method).toHaveBeenCalled();

// Verify with specific parameters
expect(mockService.method).toHaveBeenCalledWith('param1', 'param2');

// Verify the number of calls
expect(mockService.method).toHaveBeenCalledTimes(2);
```

## Error Handling

If you encounter errors or unexpected behavior, check the following:

1. Is the type definition in `suites-typings.d.ts` correct?
2. Are the correct dependencies injected?
3. Is the appropriate test type (solitary/sociable) chosen for the use case?

## Resources and Documentation

- [Suites Official Documentation](https://suites.dev/docs)
- [NestJS Testing Documentation](https://docs.nestjs.com/fundamentals/testing)
- [Jest Documentation](https://jestjs.io/docs/getting-started)
