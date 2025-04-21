import { DomainEvent } from './domain-event';

/**
 * Function type for upcasting an event from one version to another
 * The return type is any DomainEvent, as the version changes during upcasting
 */
export type EventUpcaster<T extends DomainEvent = DomainEvent> = (event: T) => DomainEvent;

/**
 * Interface for event upcaster registry that handles event schema evolution
 */
export interface EventUpcasterRegistry {
  /**
   * Register an upcaster for a specific event type and version
   * @param eventType The type of event to register an upcaster for
   * @param fromVersion The source version of the event
   * @param toVersion The target version to upcast to
   * @param upcaster Function that transforms the event from one version to another
   */
  register<T extends DomainEvent>(
    eventType: string,
    fromVersion: number,
    toVersion: number,
    upcaster: EventUpcaster<T>
  ): void;

  /**
   * Upcast an event to its latest version using the registered upcasters
   * @param event The event to upcast
   * @returns The upcasted event in its latest version
   */
  upcast<T extends DomainEvent>(event: T): DomainEvent;

  /**
   * Upcast an event to a specific version
   * @param event The event to upcast
   * @param targetVersion The version to upcast to
   * @returns The upcasted event in the specified version
   */
  upcastToVersion<T extends DomainEvent>(event: T, targetVersion: number): DomainEvent;
} 