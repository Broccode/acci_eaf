import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BootstrapCommand } from './bootstrap.command';

@Module({
  imports: [ConfigModule.forRoot()],
  providers: [BootstrapCommand],
  exports: [BootstrapCommand],
})
export class CliModule {} 