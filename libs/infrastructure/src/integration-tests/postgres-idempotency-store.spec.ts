import { Test } from '@nestjs/testing';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { EntityManager } from '@mikro-orm/core';
import { v4 as uuidv4 } from 'uuid';
import { PostgresIdempotencyStore } from '../lib/idempotency/postgres-idempotency-store';
import { ProcessedEventRecord } from '../lib/persistence/entities/processed-event.entity';
import { BetterSqliteDriver } from '@mikro-orm/better-sqlite';

describe('PostgresIdempotencyStore Integration Test', () => {
  let store: PostgresIdempotencyStore;
  let entityManager: EntityManager;

  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [
        MikroOrmModule.forRoot({
          driver: BetterSqliteDriver,
          dbName: ':memory:',
          entities: [ProcessedEventRecord],
          autoLoadEntities: true,
          registerRequestContext: true,
          allowGlobalContext: true,
        }),
        MikroOrmModule.forFeature([ProcessedEventRecord]),
      ],
      providers: [PostgresIdempotencyStore],
    }).compile();

    store = moduleRef.get<PostgresIdempotencyStore>(PostgresIdempotencyStore);
    entityManager = moduleRef.get<EntityManager>(EntityManager);

    // Create schema for the in-memory database
    const driver = entityManager.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.createSchema();
  });

  afterEach(async () => {
    // Drop schema after each test
    const driver = entityManager.getDriver();
    const generator = driver.getPlatform().getSchemaGenerator(driver);
    await generator.dropSchema();
  });

  it('should mark an event as processed and detect it', async () => {
    // Arrange
    const eventId = uuidv4();
    const processorId = 'test-processor';

    // Act
    await store.markAsProcessed(eventId, processorId);
    const processed = await store.hasBeenProcessed(eventId, processorId);

    // Assert
    expect(processed).toBe(true);
  });

  it('should retrieve a processing record including metadata', async () => {
    // Arrange
    const eventId = uuidv4();
    const processorId = 'meta-processor';
    const metadata = { answer: 42 };

    // Act
    await store.markAsProcessed(eventId, processorId, metadata);
    const record = await store.getProcessingRecord(eventId, processorId);

    // Assert
    expect(record).toBeDefined();
    expect(record?.eventId).toBe(eventId);
    expect(record?.processorId).toBe(processorId);
    expect(record?.metadata).toEqual(metadata);
  });

  it('should not report processed for different event or processor', async () => {
    // Arrange
    const eventId = uuidv4();
    const processorId = 'unique-processor';
    await store.markAsProcessed(eventId, processorId);

    // Act
    const otherEventProcessed = await store.hasBeenProcessed(uuidv4(), processorId);
    const otherProcessorProcessed = await store.hasBeenProcessed(eventId, 'other-processor');

    // Assert
    expect(otherEventProcessed).toBe(false);
    expect(otherProcessorProcessed).toBe(false);
  });

  it('should enforce uniqueness for (eventId, processorId) combination', async () => {
    // Arrange
    const eventId = uuidv4();
    const processorId = 'dup-processor';
    await store.markAsProcessed(eventId, processorId);

    // Act & Assert
    await expect(store.markAsProcessed(eventId, processorId)).rejects.toBeDefined();
  });
}); 