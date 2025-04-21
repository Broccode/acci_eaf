import { DomainEvent } from '../domain/domain-event';
import { IdempotencyStore, processEventIdempotently } from './idempotency';

// Example event handler with idempotency
export class IdempotentEventHandler {
  constructor(private readonly idempotencyStore: IdempotencyStore) {}

  /**
   * Handler ID used for idempotency tracking
   */
  private readonly handlerId = 'user-notification-handler';

  /**
   * Process a user registered event and send a welcome email
   * With idempotency guarantees to prevent duplicate emails
   */
  async handleUserRegistered(event: UserRegisteredEvent): Promise<void> {
    try {
      await processEventIdempotently(
        event,
        this.idempotencyStore,
        this.handlerId,
        async (e) => {
          // In a real application, this would send an actual email
          console.log(`Sending welcome email to ${e.payload.email}`);
          
          // Simulate some processing time
          await new Promise(resolve => setTimeout(resolve, 100));
          
          return { success: true };
        }
      );
      
      console.log(`Welcome email sent successfully to user ${event.payload.userId}`);
    } catch (error) {
      if (error instanceof Error && error.message.includes('already been processed')) {
        // This is expected if the event was already processed
        console.log(`Event ${event.id} was already processed, no duplicate email sent`);
      } else {
        // This is an unexpected error
        console.error('Error handling UserRegistered event:', error);
        throw error;
      }
    }
  }
}

// Example event type
interface UserRegisteredEvent extends DomainEvent {
  type: 'UserRegistered';
  payload: {
    userId: string;
    email: string;
    username: string;
  };
}

/**
 * Example demonstrating idempotent event processing
 */
export function demoIdempotentProcessing(idempotencyStore: IdempotencyStore): void {
  const handler = new IdempotentEventHandler(idempotencyStore);
  
  // Create a sample event
  const event: UserRegisteredEvent = {
    id: 'event-123',
    type: 'UserRegistered',
    version: 1,
    occurredAt: new Date(),
    aggregateId: 'user-456',
    payload: {
      userId: 'user-456',
      email: 'new-user@example.com',
      username: 'new-user'
    }
  };
  
  // Process the event for the first time - this should send an email
  handler.handleUserRegistered(event)
    .then(() => {
      console.log('First processing complete');
      
      // Try to process the same event again - this should be idempotent
      return handler.handleUserRegistered(event);
    })
    .then(() => {
      console.log('Second processing complete - no duplicate email should be sent');
    })
    .catch(error => {
      console.error('Error in demo:', error);
    });
} 