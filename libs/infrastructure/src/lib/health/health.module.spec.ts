import { TestBed } from '@suites/unit';
import { HealthModule } from './health.module';
import { HttpModule } from '@nestjs/axios';

// Mock the entire health controller to avoid decorator issues
jest.mock('./health.controller', () => {
  return {
    HealthController: class MockHealthController {},
  };
});

// Mock terminus and axios
jest.mock('@nestjs/terminus', () => ({
  TerminusModule: {
    forRoot: jest.fn(),
  }
}));

jest.mock('@nestjs/axios', () => ({
  HttpModule: {},
}));

// Import after mocking to use mocked version
import { HealthController } from './health.controller';
import { TerminusModule } from '@nestjs/terminus';

// Note: Similar to ObservabilityModule, HealthModule is a NestJS module with static methods
// rather than a class with dependencies, so we test the static methods directly.
// Suites is more useful for testing classes with dependencies that need to be mocked.
describe('HealthModule', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should be defined', () => {
    expect(HealthModule).toBeDefined();
  });

  it('should create module with default options', () => {
    const module = HealthModule.forRoot();
    
    expect(module).toBeDefined();
    expect(module.imports).toContainEqual(TerminusModule);
    expect(module.imports).toContainEqual(HttpModule);
    expect(module.controllers).toContain(HealthController);
    
    // Check if providers exist before accessing
    expect(module.providers).toBeDefined();
    if (!module.providers) return;
    
    // Check provider for health module options
    const optionsProvider = module.providers.find(
      (provider) => (provider as any).provide === 'HEALTH_MODULE_OPTIONS'
    );
    expect(optionsProvider).toBeDefined();
    expect((optionsProvider as any).useValue).toEqual({
      path: 'health',
      enableDefaultChecks: true,
    });
  });

  it('should include custom path in options', () => {
    const module = HealthModule.forRoot({ path: 'custom-health' });
    
    // Check if providers exist before accessing
    expect(module.providers).toBeDefined();
    if (!module.providers) return;
    
    const optionsProvider = module.providers.find(
      (provider) => (provider as any).provide === 'HEALTH_MODULE_OPTIONS'
    );
    expect((optionsProvider as any).useValue.path).toBe('custom-health');
  });

  it('should disable default checks when configured', () => {
    const module = HealthModule.forRoot({ enableDefaultChecks: false });
    
    // Check if providers exist before accessing
    expect(module.providers).toBeDefined();
    if (!module.providers) return;
    
    const optionsProvider = module.providers.find(
      (provider) => (provider as any).provide === 'HEALTH_MODULE_OPTIONS'
    );
    expect((optionsProvider as any).useValue.enableDefaultChecks).toBe(false);
  });

  it('should not include controller when health endpoint is disabled', () => {
    const module = HealthModule.forRoot({ disableHealthEndpoint: true });
    
    expect(module.controllers).toEqual([]);
  });

  it('should export TerminusModule', () => {
    const module = HealthModule.forRoot();
    
    expect(module.exports).toContain(TerminusModule);
  });
}); 