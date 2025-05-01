import { v4 as uuidv4 } from 'uuid';
import { DomainEvent, EventMetadata } from '../domain-event';
import { TenantStatus } from './tenant-status.enum';

/**
 * Event emitted when a new tenant is created
 */
export class TenantCreatedEvent implements DomainEvent {
  readonly id: string = uuidv4();
  readonly type: string = 'TenantCreated';
  readonly version: number = 1;
  readonly occurredAt: Date = new Date();

  constructor(
    public readonly aggregateId: string,
    public readonly payload: {
      name: string;
      description?: string;
      contactEmail?: string;
      configuration?: Record<string, unknown>;
    },
    public readonly metadata?: EventMetadata
  ) {}
}

/**
 * Event emitted when a tenant is activated
 */
export class TenantActivatedEvent implements DomainEvent {
  readonly id: string = uuidv4();
  readonly type: string = 'TenantActivated';
  readonly version: number = 1;
  readonly occurredAt: Date = new Date();

  constructor(
    public readonly aggregateId: string,
    public readonly payload: {
      activatedAt: Date;
    },
    public readonly metadata?: EventMetadata
  ) {}
}

/**
 * Event emitted when a tenant is deactivated
 */
export class TenantDeactivatedEvent implements DomainEvent {
  readonly id: string = uuidv4();
  readonly type: string = 'TenantDeactivated';
  readonly version: number = 1;
  readonly occurredAt: Date = new Date();

  constructor(
    public readonly aggregateId: string,
    public readonly payload: {
      deactivatedAt: Date;
    },
    public readonly metadata?: EventMetadata
  ) {}
}

/**
 * Event emitted when a tenant is suspended
 */
export class TenantSuspendedEvent implements DomainEvent {
  readonly id: string = uuidv4();
  readonly type: string = 'TenantSuspended';
  readonly version: number = 1;
  readonly occurredAt: Date = new Date();

  constructor(
    public readonly aggregateId: string,
    public readonly payload: {
      suspendedAt: Date;
    },
    public readonly metadata?: EventMetadata
  ) {}
}

/**
 * Event emitted when tenant information is updated
 */
export class TenantUpdatedEvent implements DomainEvent {
  readonly id: string = uuidv4();
  readonly type: string = 'TenantUpdated';
  readonly version: number = 1;
  readonly occurredAt: Date = new Date();

  constructor(
    public readonly aggregateId: string,
    public readonly payload: {
      name?: string;
      description?: string;
      contactEmail?: string;
      configuration?: Record<string, unknown>;
      updatedAt: Date;
    },
    public readonly metadata?: EventMetadata
  ) {}
} 