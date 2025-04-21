import { InMemoryIdempotencyStore } from './in-memory-idempotency-store';

describe('InMemoryIdempotencyStore', () => {
  let store: InMemoryIdempotencyStore;

  beforeEach(() => {
    store = new InMemoryIdempotencyStore();
  });

  it('should mark an event as processed', async () => {
    // Arrange
    const eventId = 'test-event-1';
    const processorId = 'test-processor';
    const metadata = { some: 'data' };

    // Act
    await store.markAsProcessed(eventId, processorId, metadata);
    
    // Assert
    const processed = await store.hasBeenProcessed(eventId, processorId);
    expect(processed).toBe(true);
  });

  it('should return false for events that have not been processed', async () => {
    // Arrange
    const eventId = 'test-event-1';
    const processorId = 'test-processor';
    const otherEventId = 'test-event-2';
    const otherProcessorId = 'other-processor';

    // Act
    await store.markAsProcessed(eventId, processorId);
    
    // Assert - Different event
    const processedDiffEvent = await store.hasBeenProcessed(otherEventId, processorId);
    expect(processedDiffEvent).toBe(false);
    
    // Assert - Different processor
    const processedDiffProcessor = await store.hasBeenProcessed(eventId, otherProcessorId);
    expect(processedDiffProcessor).toBe(false);
  });

  it('should retrieve processing record with metadata', async () => {
    // Arrange
    const eventId = 'test-event-1';
    const processorId = 'test-processor';
    const metadata = { some: 'data', count: 42 };

    // Act
    await store.markAsProcessed(eventId, processorId, metadata);
    const record = await store.getProcessingRecord(eventId, processorId);
    
    // Assert
    expect(record).toBeDefined();
    expect(record?.eventId).toBe(eventId);
    expect(record?.processorId).toBe(processorId);
    expect(record?.metadata).toEqual(metadata);
    expect(record?.processedAt).toBeInstanceOf(Date);
  });

  it('should return undefined for non-existent processing records', async () => {
    // Arrange
    const eventId = 'test-event-1';
    const processorId = 'test-processor';

    // Act
    const record = await store.getProcessingRecord(eventId, processorId);
    
    // Assert
    expect(record).toBeUndefined();
  });

  it('should clear all records', async () => {
    // Arrange
    const eventId1 = 'test-event-1';
    const eventId2 = 'test-event-2';
    const processorId = 'test-processor';

    // Act
    await store.markAsProcessed(eventId1, processorId);
    await store.markAsProcessed(eventId2, processorId);
    
    // Verify both are processed
    expect(await store.hasBeenProcessed(eventId1, processorId)).toBe(true);
    expect(await store.hasBeenProcessed(eventId2, processorId)).toBe(true);
    
    // Clear the store
    store.clear();
    
    // Assert
    expect(await store.hasBeenProcessed(eventId1, processorId)).toBe(false);
    expect(await store.hasBeenProcessed(eventId2, processorId)).toBe(false);
  });
}); 