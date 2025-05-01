import { AggregateRoot } from '../aggregate-root';
import { DomainEvent } from '../domain-event';
import { TenantStatus } from './tenant-status.enum';
import {
  TenantActivatedEvent,
  TenantCreatedEvent,
  TenantDeactivatedEvent,
  TenantSuspendedEvent,
  TenantUpdatedEvent
} from './tenant-events';

/**
 * Tenant aggregate root that maintains its state through events
 */
export class TenantAggregate extends AggregateRoot {
  private _name: string = '';
  private _description?: string;
  private _status: TenantStatus = TenantStatus.ACTIVE;
  private _configuration?: Record<string, unknown>;
  private _contactEmail?: string;
  private _createdAt: Date = new Date();
  private _updatedAt: Date = new Date();

  /**
   * Get the tenant name
   */
  get name(): string {
    return this._name;
  }

  /**
   * Get the tenant description
   */
  get description(): string | undefined {
    return this._description;
  }

  /**
   * Get the tenant status
   */
  get status(): TenantStatus {
    return this._status;
  }

  /**
   * Get the tenant configuration
   */
  get configuration(): Record<string, unknown> | undefined {
    return this._configuration;
  }

  /**
   * Get the tenant contact email
   */
  get contactEmail(): string | undefined {
    return this._contactEmail;
  }

  /**
   * Get the creation date of the tenant
   */
  get createdAt(): Date {
    return new Date(this._createdAt);
  }

  /**
   * Get the last update date of the tenant
   */
  get updatedAt(): Date {
    return new Date(this._updatedAt);
  }

  /**
   * Create a new tenant
   * @param name Tenant name
   * @param description Optional tenant description
   * @param contactEmail Optional contact email
   * @param configuration Optional configuration settings
   */
  static create(
    name: string,
    description?: string,
    contactEmail?: string,
    configuration?: Record<string, unknown>
  ): TenantAggregate {
    const tenant = new TenantAggregate();
    tenant.apply(
      new TenantCreatedEvent(
        tenant.id,
        {
          name,
          description,
          contactEmail,
          configuration
        }
      )
    );
    return tenant;
  }

  /**
   * Activate the tenant
   */
  activate(): void {
    if (this._status === TenantStatus.ACTIVE) {
      return; // Already active, do nothing
    }

    this.apply(
      new TenantActivatedEvent(
        this.id,
        {
          activatedAt: new Date()
        }
      )
    );
  }

  /**
   * Deactivate the tenant
   */
  deactivate(): void {
    if (this._status === TenantStatus.INACTIVE) {
      return; // Already inactive, do nothing
    }

    this.apply(
      new TenantDeactivatedEvent(
        this.id,
        {
          deactivatedAt: new Date()
        }
      )
    );
  }

  /**
   * Suspend the tenant
   */
  suspend(): void {
    if (this._status === TenantStatus.SUSPENDED) {
      return; // Already suspended, do nothing
    }

    this.apply(
      new TenantSuspendedEvent(
        this.id,
        {
          suspendedAt: new Date()
        }
      )
    );
  }

  /**
   * Update tenant information
   */
  update(
    name?: string,
    description?: string,
    contactEmail?: string,
    configuration?: Record<string, unknown>
  ): void {
    // Only apply event if there are actual changes
    if (
      (name === undefined || name === this._name) &&
      (description === undefined || description === this._description) &&
      (contactEmail === undefined || contactEmail === this._contactEmail) &&
      (configuration === undefined || configuration === this._configuration)
    ) {
      return; // No changes, do nothing
    }

    this.apply(
      new TenantUpdatedEvent(
        this.id,
        {
          name,
          description,
          contactEmail,
          configuration,
          updatedAt: new Date()
        }
      )
    );
  }

  /**
   * Apply domain events to update the aggregate state
   * @param event The event to apply
   */
  protected applyEvent(event: DomainEvent): void {
    switch (event.type) {
      case 'TenantCreated':
        this.applyTenantCreated(event as TenantCreatedEvent);
        break;
      case 'TenantActivated':
        this.applyTenantActivated(event as TenantActivatedEvent);
        break;
      case 'TenantDeactivated':
        this.applyTenantDeactivated(event as TenantDeactivatedEvent);
        break;
      case 'TenantSuspended':
        this.applyTenantSuspended(event as TenantSuspendedEvent);
        break;
      case 'TenantUpdated':
        this.applyTenantUpdated(event as TenantUpdatedEvent);
        break;
      default:
        throw new Error(`Unknown event type: ${event.type}`);
    }
  }

  private applyTenantCreated(event: TenantCreatedEvent): void {
    this._name = event.payload.name;
    this._description = event.payload.description;
    this._contactEmail = event.payload.contactEmail;
    this._configuration = event.payload.configuration;
    this._createdAt = event.occurredAt;
    this._updatedAt = event.occurredAt;
  }

  private applyTenantActivated(event: TenantActivatedEvent): void {
    this._status = TenantStatus.ACTIVE;
    this._updatedAt = event.occurredAt;
  }

  private applyTenantDeactivated(event: TenantDeactivatedEvent): void {
    this._status = TenantStatus.INACTIVE;
    this._updatedAt = event.occurredAt;
  }

  private applyTenantSuspended(event: TenantSuspendedEvent): void {
    this._status = TenantStatus.SUSPENDED;
    this._updatedAt = event.occurredAt;
  }

  private applyTenantUpdated(event: TenantUpdatedEvent): void {
    if (event.payload.name !== undefined) {
      this._name = event.payload.name;
    }
    if (event.payload.description !== undefined) {
      this._description = event.payload.description;
    }
    if (event.payload.contactEmail !== undefined) {
      this._contactEmail = event.payload.contactEmail;
    }
    if (event.payload.configuration !== undefined) {
      this._configuration = event.payload.configuration;
    }
    this._updatedAt = event.payload.updatedAt;
  }
} 