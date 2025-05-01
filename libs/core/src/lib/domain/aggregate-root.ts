import { v4 as uuidv4 } from 'uuid';
import { DomainEvent } from './domain-event';

/**
 * Base class for all Aggregate Roots in the domain model
 * An Aggregate Root is an entity that maintains its state through events
 */
export abstract class AggregateRoot {
  /**
   * Unique identifier for the aggregate
   */
  public readonly id: string;

  /**
   * Current version of the aggregate (increments with each applied event)
   */
  private _version: number = 0;

  /**
   * List of uncommitted events that have been applied to the aggregate
   * but not yet persisted to the event store
   */
  private _uncommittedEvents: DomainEvent[] = [];

  /**
   * Get the current version of the aggregate
   */
  public get version(): number {
    return this._version;
  }

  /**
   * Constructor for creating a new aggregate with a given ID or
   * automatically generating one if not provided
   */
  constructor(id?: string) {
    this.id = id || uuidv4();
  }

  /**
   * Apply an event to the aggregate
   * This updates the aggregate's state and adds the event to the uncommitted events list
   * @param event The domain event to apply
   */
  protected apply(event: DomainEvent): void {
    this.applyEvent(event);
    this._version++;
    this._uncommittedEvents.push(event);
  }

  /**
   * Abstract method that must be implemented by concrete aggregate classes
   * to apply the event-specific state changes
   * @param event The domain event to apply
   */
  protected abstract applyEvent(event: DomainEvent): void;

  /**
   * Get all uncommitted events
   * @returns Array of uncommitted domain events
   */
  public getUncommittedEvents(): DomainEvent[] {
    return [...this._uncommittedEvents];
  }

  /**
   * Clear the list of uncommitted events
   * Usually called after the events have been persisted to the event store
   */
  public clearUncommittedEvents(): void {
    this._uncommittedEvents = [];
  }

  /**
   * Rebuild the aggregate state from a series of events
   * @param events The events to replay
   */
  public loadFromHistory(events: DomainEvent[]): void {
    events.forEach(event => {
      this.applyEvent(event);
      this._version++;
    });
  }
} 