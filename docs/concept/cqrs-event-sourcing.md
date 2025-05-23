# CQRS and Event Sourcing in ACCI EAF

* **Version:** 1.1 Reviewed
* **Date:** 2025-05-05
* **Status:** Final 🎉

## Table of Contents 📑

1. [Introduction](#introduction)
   2. [CQRS Overview 🔄](#cqrs-overview-🔄)
   3. [Event Sourcing Overview 📜](#event-sourcing-overview-📜)
   4. [Implementation in ACCI EAF 🏗️](#implementation-in-acci-eaf-🏗️)
   5. [Multi-Tenancy Integration 🌐](#multi-tenancy-integration-🌐)
   6. [Implementation Guidelines 🛠️](#implementation-guidelines-🛠️)
   7. [Advanced Topics 🚀](#advanced-topics-🚀)
   8. [Testing Strategies ✅](#testing-strategies-✅)
   9. [Conclusion 🎯](#conclusion-🎯)
10. [References 📚](#references-📚)

## Introduction 📘

Welcome to the comprehensive guide on CQRS and Event Sourcing in ACCI EAF! 🚀

This document provides an in-depth exploration of:

* The core principles of Command Query Responsibility Segregation (CQRS) and Event Sourcing
* Detailed implementation strategies within the ACCI EAF architecture
* Multi-tenancy considerations and isolation patterns
* Best practices for error handling, testing, and performance optimization
* Advanced topics such as snapshots and schema evolution
* Strategies for maintaining a complete audit trail and achieving eventual consistency ✅

## CQRS Overview 🔄

CQRS (Command Query Responsibility Segregation) is an architectural pattern that separates read and write operations in an application:

* **Commands** represent intentions to change the state of the system
* **Queries** represent requests for information without changing state

By separating these concerns, we can optimize each path independently and address their different requirements:

| Aspect | Command Path | Query Path |
|--------|--------------|------------|
| Purpose | Change state | Retrieve data |
| Complexity | Business rules, validation | Filtering, projection |
| Performance Needs | Consistency, reliability | Speed, scalability |
| Scaling Pattern | Vertical (typically) | Horizontal (easily) |
| Caching | Limited use | Extensively used |

* **Why CQRS? 🤔**
*
* * Enables independent scaling of read/write workloads ✔️
* * Improves maintainability by decoupling command logic from query logic ✔️
* * Facilitates clear separation of concerns and simpler authorization rules ✔️
* * Provides a foundation for complex business workflows and auditing ✔️

## Event Sourcing Overview 📜

Event Sourcing is a complementary pattern where:

* System state changes are captured as a sequence of events
* Events are immutable facts about what happened
* The current state is derived by replaying events
* Events serve as the single source of truth

Benefits include:

* Complete audit trail and temporal queries
* Ability to reconstruct state at any point in time
* Natural fit for domain-driven design
* Foundation for event-driven architectures

* **Why Event Sourcing? 🤔**
*
* * Guarantees a full history of changes for compliance and debugging ✨
* * Simplifies temporal queries and snapshots for performance ✨
* * Enables event-driven integrations and reactive architectures ✨
* * Supports immutability and traceability of domain changes ✨

## Implementation in ACCI EAF 🏗️

### Core Components

#### 1. Commands & Command Handlers

Commands represent user intentions and are processed by command handlers:

```typescript
// Command Definition
export class CreateUserCommand implements ICommand {
  constructor(
    public readonly tenantId: string,
    public readonly email: string,
    public readonly fullName: string,
    public readonly initialRole: string
  ) {}
}

// Command Handler
@CommandHandler(CreateUserCommand)
export class CreateUserCommandHandler implements ICommandHandler<CreateUserCommand> {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly eventBus: EventBus,
  ) {}

  async execute(command: CreateUserCommand): Promise<void> {
    // Create user aggregate and apply business rules
    const user = User.create(
      command.tenantId,
      command.email,
      command.fullName,
      command.initialRole
    );
    
    // Save the aggregate (which saves events)
    await this.userRepository.save(user);
  }
}
```

#### 2. Events & Event Store

* In ACCI EAF, the event store is implemented using MikroORM's `EntityManager`, providing robust transactional support and flexible APIs. 🎉

```typescript
import { EntityManager } from '@mikro-orm/core';
import { Injectable } from '@nestjs/common';
import { DomainEvent, EventStore } from 'core';
import { EventRecord } from './entities/event-record.entity';

@Injectable()
export class PostgresEventStore implements EventStore {
  constructor(private readonly em: EntityManager) {}

  async save(event: DomainEvent): Promise<void> {
    const record = this.createEventRecord(event);
    await this.em.persistAndFlush(record);
  }

  async saveMany(events: DomainEvent[]): Promise<void> {
    const records = events.map(e => this.createEventRecord(e));
    await this.em.transactional(async tx => {
      records.forEach(r => tx.persist(r));
      await tx.flush();
    });
  }

  // Additional methods (loadEvents, getNextVersion, etc.)
}
```

#### 3. Aggregates & Repositories

Aggregates encapsulate business rules and generate events:

```typescript
export class User extends Aggregate {
  private _email: string;
  private _fullName: string;
  private _role: string;
  private _tenantId: string;
  
  // Constructor and getters
  
  public static create(
    tenantId: string,
    email: string,
    fullName: string,
    role: string
  ): User {
    const user = new User(uuidv4());
    
    // Apply business rules, validations
    
    // Record the event
    user.apply(
      new UserCreatedEvent(
        user.id,
        tenantId,
        email,
        fullName,
        role,
        new Date()
      )
    );
    
    return user;
  }
  
  // When event is applied, update state
  protected onUserCreatedEvent(event: UserCreatedEvent): void {
    this._tenantId = event.tenantId;
    this._email = event.email;
    this._fullName = event.fullName;
    this._role = event.role;
  }
}

// Repository loads and saves aggregates via events
@Injectable()
export class UserRepository {
  constructor(private readonly eventStore: EventStore) {}
  
  async save(user: User): Promise<void> {
    await this.eventStore.save(user.getUncommittedEvents());
    user.clearUncommittedEvents();
  }
  
  async findById(id: string, tenantId: string): Promise<User> {
    const events = await this.eventStore.getStream(id);
    
    // Check tenant isolation
    if (events.length > 0 && events[0].tenantId !== tenantId) {
      throw new Error('Access denied to entity from another tenant');
    }
    
    // Reconstruct aggregate from events
    const user = new User(id);
    user.loadFromHistory(events);
    return user;
  }
}
```

#### 4. Queries & Query Handlers

Queries retrieve data from optimized read models:

```typescript
// Query Definition
export class GetUserByEmailQuery implements IQuery<UserDto | null> {
  constructor(
    public readonly tenantId: string,
    public readonly email: string
  ) {}
}

// Query Handler
@QueryHandler(GetUserByEmailQuery)
export class GetUserByEmailQueryHandler implements IQueryHandler<GetUserByEmailQuery, UserDto | null> {
  constructor(
    @InjectRepository(UserReadModel)
    private readonly userReadModelRepository: Repository<UserReadModel>
  ) {}

  async execute(query: GetUserByEmailQuery): Promise<UserDto | null> {
    const userReadModel = await this.userReadModelRepository.findOne({
      where: {
        tenantId: query.tenantId,
        email: query.email
      }
    });
    
    if (!userReadModel) {
      return null;
    }
    
    return this.mapToDto(userReadModel);
  }
  
  private mapToDto(userReadModel: UserReadModel): UserDto {
    // Mapping logic
  }
}
```

#### 5. Projections (Event Handlers)

Projections transform events into read models:

```typescript
@EventHandler(UserCreatedEvent)
export class UserProjection implements IEventHandler<UserCreatedEvent> {
  constructor(
    @InjectRepository(UserReadModel)
    private readonly userReadModelRepository: Repository<UserReadModel>
  ) {}

  async handle(event: UserCreatedEvent): Promise<void> {
    const userReadModel = new UserReadModel();
    userReadModel.id = event.streamId;
    userReadModel.tenantId = event.tenantId;
    userReadModel.email = event.email;
    userReadModel.fullName = event.fullName;
    userReadModel.role = event.role;
    userReadModel.createdAt = event.timestamp;
    
    await this.userReadModelRepository.save(userReadModel);
  }
}
```

### Flow Diagrams

#### Command Flow

```
User Request → API Controller → Command Bus → Command Handler → 
  Aggregate (applies business rules) → Events generated → 
  Event Store (saves events) → Event Bus → Event Handlers (update read models)
```

#### Query Flow

```
User Request → API Controller → Query Bus → Query Handler → 
  Read Model Repository → Database → Response
```

## Multi-Tenancy Integration 🌐

**Why Multi-Tenancy? 🌍**

* Enforces strict data isolation between tenants 🔒
* Simplifies provisioning and scaling per tenant 📈
* Integrates with MikroORM filters for RLS support 🛡️
* Supports flexible tenant-specific configurations ⚙️

In the ACCI EAF, multi-tenancy is integrated with CQRS/ES at several points:

1. **Commands** always include the tenant ID
2. **Events** store the tenant ID as part of their data
3. **Event Store** enforces tenant isolation during retrieval
4. **Repositories** verify tenant context when loading aggregates
5. **Query Handlers** always filter by tenant ID
6. **Read Models** contain tenant ID columns (and rely on RLS via MikroORM Filters)

This ensures that data remains properly isolated between tenants throughout the system.

## Implementation Guidelines 🛠️

### When to Use CQRS/ES

CQRS and Event Sourcing are powerful but add complexity. Consider their use when:

* Business requirements include audit trails or temporal queries
* Complex domain with sophisticated business rules
* Need for scalability, especially for read operations
* High value in traceability of changes

For simpler domains or CRUD-like operations, a simplified approach may be preferred.

### Best Practices

**Key Best Practices 📝**

* Validate commands before execution ✔️
* Keep aggregates focused and small 🎯
* Model events as immutable versioned facts 📜
* Design projections for read performance 🚀
* Handle errors with retry and dead-letter strategies 🔄

1. **Command Validation**
   * Validate commands before processing
   * Return meaningful errors early

2. **Aggregate Design**
   * Keep aggregates small and focused
   * Enforce business rules consistently
   * Think carefully about aggregate boundaries

3. **Event Design**
   * Events should be named in past tense (e.g., `UserCreated`)
   * Include all relevant information needed by projections
   * Make events immutable and versioned
   * Consider future evolution needs

4. **Projection Efficiency**
   * Design read models for query performance
   * Consider indexing strategies carefully
   * Make projections idempotent (can be replayed safely)

5. **Error Handling**
   * Plan for event handling failures (retry, dead letter)
   * Consider eventual consistency implications
   * Design for recovering from projection failures

## Advanced Topics 🚀

### Snapshots

For aggregates with many events, snapshots can improve loading performance:

```typescript
@Injectable()
export class SnapshotService {
  constructor(
    @InjectRepository(SnapshotEntity)
    private readonly snapshotRepository: Repository<SnapshotEntity>
  ) {}

  async getLatestSnapshot(streamId: string): Promise<Snapshot | null> {
    // Implementation
  }

  async saveSnapshot(aggregate: Aggregate): Promise<void> {
    // Implementation
  }
}
```

Snapshots are taken periodically based on event count thresholds.

### Event Schema Evolution

As the system evolves, event schemas may need to change. Strategies include:

1. **Versioned Events** - Include version number in event metadata
2. **Upcasting** - Transform old event versions to new format during loading
3. **Lazy Migration** - Migrate events when they're accessed
4. **Event Transformation** - Apply transformations in projections

### Event Replay

For rebuilding read models or recovering from failures:

```typescript
@Injectable()
export class ProjectionManager {
  constructor(
    private readonly eventStore: EventStore,
    private readonly eventBus: EventBus
  ) {}

  async replayProjections(
    projectionTypes: string[],
    startDate?: Date,
    endDate?: Date
  ): Promise<void> {
    // Implementation for replaying specific projections
  }
}
```

### What to Explore Next? 🔭

* Snapshots for faster aggregate reconstruction 🗄️

* Upcasting strategies for event evolution
* Event replay and projection management 🔄
* Integration events and external system communication 🌐

## Testing Strategies ✅

### Unit Testing Aggregates

```typescript
describe('User Aggregate', () => {
  it('should create a user and apply UserCreatedEvent', () => {
    // Arrange
    const tenantId = 'tenant-1';
    const email = 'user@example.com';
    const fullName = 'Test User';
    const role = 'user';
    
    // Act
    const user = User.create(tenantId, email, fullName, role);
    const uncommittedEvents = user.getUncommittedEvents();
    
    // Assert
    expect(uncommittedEvents.length).toBe(1);
    expect(uncommittedEvents[0]).toBeInstanceOf(UserCreatedEvent);
    expect(uncommittedEvents[0].email).toBe(email);
    // More assertions
  });
});
```

### Testing Command Handlers

```typescript
describe('CreateUserCommandHandler', () => {
  let handler: CreateUserCommandHandler;
  let mockUserRepository: MockType<UserRepository>;
  let mockEventBus: MockType<EventBus>;
  
  beforeEach(() => {
    // Setup mocks
    
    handler = new CreateUserCommandHandler(
      mockUserRepository,
      mockEventBus
    );
  });
  
  it('should create and save a user', async () => {
    // Arrange
    const command = new CreateUserCommand(
      'tenant-1',
      'user@example.com',
      'Test User',
      'user'
    );
    
    // Act
    await handler.execute(command);
    
    // Assert
    expect(mockUserRepository.save).toHaveBeenCalled();
    // More assertions
  });
});
```

### Testing Event Handlers (Projections)

```typescript
describe('UserProjection', () => {
  let projection: UserProjection;
  let mockRepository: MockType<Repository<UserReadModel>>;
  
  beforeEach(() => {
    // Setup mocks
    
    projection = new UserProjection(mockRepository);
  });
  
  it('should create a read model from UserCreatedEvent', async () => {
    // Arrange
    const event = new UserCreatedEvent(
      'user-1',
      'tenant-1',
      'user@example.com',
      'Test User',
      'user',
      new Date()
    );
    
    // Act
    await projection.handle(event);
    
    // Assert
    expect(mockRepository.save).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'user-1',
        email: 'user@example.com',
        // More properties
      })
    );
  });
});
```

### Testing Goals 🎯

* Ensure aggregates produce correct events 🧪

* Validate command handlers in isolation ⚔️
* Cover projections and read models with integration tests 🔍
* Use idempotency tests to guarantee safe replays 🔄

## Conclusion 🎯

Thank you for exploring CQRS and Event Sourcing in ACCI EAF! 🌟

CQRS and Event Sourcing provide a powerful foundation for building enterprise applications in the ACCI EAF. By understanding these patterns and following the implementation guidelines, developers can create scalable, maintainable, and traceable applications that meet complex business requirements.

## References 📚

* [Martin Fowler on CQRS](https://martinfowler.com/bliki/CQRS.html)
* [Event Sourcing Pattern](https://docs.microsoft.com/en-us/azure/architecture/patterns/event-sourcing)
* [Greg Young - CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf)
* [Axon Framework Reference Guide](https://docs.axoniq.io/reference-guide/)
* [NestJS CQRS Module](https://docs.nestjs.com/recipes/cqrs)
