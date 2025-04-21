import { DomainEvent } from './domain-event';
import { DefaultEventUpcasterRegistry } from './default-event-upcaster-registry';

// Example: Event schema evolution for a UserRegistered event

// Version 1 of the UserRegistered event (initial version)
interface UserRegisteredV1 extends DomainEvent {
  type: 'UserRegistered';
  version: 1;
  payload: {
    userId: string;
    email: string;
  };
}

// Version 2 adds the username field
interface UserRegisteredV2 extends DomainEvent {
  type: 'UserRegistered';
  version: 2;
  payload: {
    userId: string;
    email: string;
    username: string;
  };
}

// Version 3 adds the registrationSource field
interface UserRegisteredV3 extends DomainEvent {
  type: 'UserRegistered';
  version: 3;
  payload: {
    userId: string;
    email: string;
    username: string;
    registrationSource: 'web' | 'mobile' | 'api';
  };
}

/**
 * Example usage of event upcasting
 */
export function demoEventUpcasting(): void {
  // Create the upcaster registry
  const registry = new DefaultEventUpcasterRegistry();

  // Register upcasters from v1 to v2 (adding username)
  registry.register<UserRegisteredV1>(
    'UserRegistered',
    1,
    2,
    (event) => {
      // Extract email username as a fallback
      const username = event.payload.email.split('@')[0];
      
      return {
        ...event,
        version: 2,
        payload: {
          ...event.payload,
          username
        }
      };
    }
  );

  // Register upcasters from v2 to v3 (adding registrationSource)
  registry.register<UserRegisteredV2>(
    'UserRegistered',
    2,
    3,
    (event) => {
      return {
        ...event,
        version: 3,
        payload: {
          ...event.payload,
          registrationSource: 'web' // Default assumption for older events
        }
      };
    }
  );

  // Example usage: 
  // When we read an old event (v1) from storage, we can upcast it to the latest version
  const oldEvent: UserRegisteredV1 = {
    id: '123',
    type: 'UserRegistered',
    version: 1,
    occurredAt: new Date(),
    aggregateId: 'user-1',
    payload: {
      userId: 'user-1',
      email: 'user@example.com'
    }
  };

  // Upcast the event to the latest version
  const upcasted = registry.upcast(oldEvent);
  
  // Now we can use the event with the latest structure
  console.log('Upcasted event:', upcasted);
  // This would log an event with version 3, containing userId, email, username, and registrationSource
  
  // We can also upcast to a specific version
  const upcastedToV2 = registry.upcastToVersion(oldEvent, 2);
  console.log('Upcasted to V2:', upcastedToV2);
  // This would log an event with version 2, containing userId, email, and username
} 