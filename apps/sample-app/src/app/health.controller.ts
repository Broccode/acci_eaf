import { Controller, Get } from '@nestjs/common';

/**
 * Simple health check endpoint for the Sample App.
 * Returns `{ status: 'ok' }` when the application is up and running.
 */
@Controller('health')
export class HealthController {
  @Get()
  check() {
    return { status: 'ok' };
  }
}
