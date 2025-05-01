import { CommandFactory } from 'nest-commander';
import { CliModule } from './app/admin/cli/cli.module';

async function bootstrap() {
  await CommandFactory.run(CliModule, ['log', 'error', 'warn']);
}

bootstrap(); 