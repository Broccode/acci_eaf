import { TestBed, Mocked } from '@suites/unit';
import { HealthController } from './health.controller';
import { HealthCheckService, DiskHealthIndicator, MemoryHealthIndicator, HealthCheckResult } from '@nestjs/terminus';

describe('HealthController with Suites', () => {
  let underTest: HealthController;
  let healthCheckService: any;
  let diskHealthIndicator: any;
  let memoryHealthIndicator: any;

  beforeEach(async () => {
    // Mock the dependencies manually
    healthCheckService = {
      check: jest.fn(),
      // Add required properties to match HealthCheckService interface
      healthCheckExecutor: {},
      errorLogger: jest.fn(),
      logger: { error: jest.fn(), log: jest.fn() },
    };
    
    diskHealthIndicator = {
      checkStorage: jest.fn(),
    };
    
    memoryHealthIndicator = {
      checkHeap: jest.fn(),
      checkRSS: jest.fn(),
    };
    
    // Create a simple instance with our mocked dependencies
    underTest = new HealthController(
      healthCheckService as HealthCheckService,
      diskHealthIndicator as DiskHealthIndicator,
      memoryHealthIndicator as MemoryHealthIndicator,
      { path: 'health', enableDefaultChecks: true }
    );

    // Setup core mock behaviors
    healthCheckService.check.mockResolvedValue({ 
      status: 'ok',
      info: {
        storage: { status: 'up' },
        memory_heap: { status: 'up' },
        memory_rss: { status: 'up' }
      },
      error: {},
      details: {
        storage: { status: 'up' },
        memory_heap: { status: 'up' },
        memory_rss: { status: 'up' }
      }
    } as HealthCheckResult);
    
    diskHealthIndicator.checkStorage.mockResolvedValue({
      storage: { status: 'up' },
    });
    
    memoryHealthIndicator.checkHeap.mockResolvedValue({
      memory_heap: { status: 'up' },
    });
    
    memoryHealthIndicator.checkRSS.mockResolvedValue({
      memory_rss: { status: 'up' },
    });
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  it('should check health with all default checks when enabled', async () => {
    await underTest.check();

    // Verify health check service was called
    expect(healthCheckService.check).toHaveBeenCalled();
    
    // Get the array of check functions passed to healthCheckService.check
    const checkFunctions = healthCheckService.check.mock.calls[0][0];
    expect(checkFunctions.length).toBe(3); // 3 default checks

    // Call each check function to verify they call the right indicators
    await checkFunctions[0]();
    await checkFunctions[1]();
    await checkFunctions[2]();

    // Verify disk check was called correctly
    expect(diskHealthIndicator.checkStorage).toHaveBeenCalledWith(
      'storage',
      expect.objectContaining({
        path: '/',
        thresholdPercent: 0.9,
      })
    );

    // Verify memory heap check was called correctly
    expect(memoryHealthIndicator.checkHeap).toHaveBeenCalledWith(
      'memory_heap',
      300 * 1024 * 1024 // 300MB
    );

    // Verify memory RSS check was called correctly
    expect(memoryHealthIndicator.checkRSS).toHaveBeenCalledWith(
      'memory_rss',
      300 * 1024 * 1024 // 300MB
    );
  });

  it('should check health with no default checks when disabled', async () => {
    // Create a new controller with disabled default checks
    const controllerWithDisabledChecks = new HealthController(
      healthCheckService as HealthCheckService,
      diskHealthIndicator as DiskHealthIndicator,
      memoryHealthIndicator as MemoryHealthIndicator,
      { path: 'health', enableDefaultChecks: false }
    );

    await controllerWithDisabledChecks.check();

    // Verify health check service was called with empty array
    expect(healthCheckService.check).toHaveBeenCalled();
    const checkFunctions = healthCheckService.check.mock.calls[0][0];
    expect(checkFunctions.length).toBe(0); // No checks
  });
}); 