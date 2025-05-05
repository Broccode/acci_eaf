import { Controller, Get, Inject } from '@nestjs/common';
import { 
  HealthCheck, 
  HealthCheckService, 
  DiskHealthIndicator, 
  MemoryHealthIndicator 
} from '@nestjs/terminus';

interface HealthModuleOptions {
  path: string;
  enableDefaultChecks: boolean;
}

@Controller()
export class HealthController {
  constructor(
    private readonly health: HealthCheckService,
    private readonly disk: DiskHealthIndicator,
    private readonly memory: MemoryHealthIndicator,
    @Inject('HEALTH_MODULE_OPTIONS') private readonly options: HealthModuleOptions,
  ) {}

  @Get('health')
  @HealthCheck()
  check() {
    const checks = [];

    if (this.options.enableDefaultChecks) {
      // Check disk storage
      checks.push(() => 
        this.disk.checkStorage('storage', { 
          path: '/', 
          thresholdPercent: 0.9 
        })
      );

      // Check memory heap
      checks.push(() => 
        this.memory.checkHeap('memory_heap', 300 * 1024 * 1024) // 300MB
      );

      // Check memory RSS
      checks.push(() => 
        this.memory.checkRSS('memory_rss', 300 * 1024 * 1024) // 300MB
      );
    }

    return this.health.check(checks);
  }
} 