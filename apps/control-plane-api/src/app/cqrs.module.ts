import { Global, Module } from '@nestjs/common';
import { InMemoryCommandBus, InMemoryQueryBus, InMemoryEventBus } from 'infrastructure';
import { COMMAND_BUS, QUERY_BUS, EVENT_BUS } from 'core';

@Global()
@Module({
  providers: [
    { provide: COMMAND_BUS, useClass: InMemoryCommandBus },
    { provide: QUERY_BUS, useClass: InMemoryQueryBus },
    { provide: EVENT_BUS, useClass: InMemoryEventBus },
  ],
  exports: [COMMAND_BUS, QUERY_BUS, EVENT_BUS],
})
export class CqrsModule {}
