import { DomainEvent, EventMetadata } from './domain-event';
import { EventUpcaster, EventUpcasterRegistry } from './event-upcaster';

/**
 * A registry for event upcasters that manages event schema evolution
 */
export class DefaultEventUpcasterRegistry implements EventUpcasterRegistry {
  private readonly upcasters: Map<string, EventUpcaster[]> = new Map();
  private readonly versionMapping: Map<string, Map<number, number>> = new Map();

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
  ): void {
    // Create the key for storing upcasters
    const key = `${eventType}:${fromVersion}`;

    // Store the upcaster in the map
    if (!this.upcasters.has(key)) {
      this.upcasters.set(key, []);
    }
    // Type cast needed to handle the generic constraint
    this.upcasters.get(key)!.push(upcaster as unknown as EventUpcaster);

    // Store the version mapping
    if (!this.versionMapping.has(eventType)) {
      this.versionMapping.set(eventType, new Map());
    }
    this.versionMapping.get(eventType)!.set(fromVersion, toVersion);
  }

  /**
   * Upcast an event to its latest version
   * @param event The event to upcast
   * @returns The upcasted event
   */
  upcast<T extends DomainEvent>(event: T): DomainEvent {
    const eventType = event.type;
    const versionMap = this.versionMapping.get(eventType);

    if (!versionMap) {
      return event; // No upcasters registered for this event type
    }

    let currentEvent = { ...event } as DomainEvent;
    let currentVersion = event.version;
    
    // Keep upcasting until we can't go any further
    while (versionMap.has(currentVersion)) {
      const targetVersion = versionMap.get(currentVersion)!;
      const key = `${eventType}:${currentVersion}`;
      const upcasters = this.upcasters.get(key) || [];

      // Apply all upcasters for this version
      for (const upcaster of upcasters) {
        // Create metadata tracking for the original version
        if (!currentEvent.metadata) {
          currentEvent.metadata = {} as EventMetadata;
        }
        
        if (!currentEvent.metadata.originalVersion) {
          currentEvent.metadata.originalVersion = event.version;
        }
        
        // Apply the upcaster
        currentEvent = upcaster(currentEvent);
        
        // Ensure the version is updated
        currentEvent.version = targetVersion;
      }

      currentVersion = targetVersion;
    }

    return currentEvent;
  }

  /**
   * Upcast an event to a specific version
   * @param event The event to upcast
   * @param targetVersion The version to upcast to
   * @returns The upcasted event in the specified version
   */
  upcastToVersion<T extends DomainEvent>(event: T, targetVersion: number): DomainEvent {
    const eventType = event.type;
    const versionMap = this.versionMapping.get(eventType);

    if (!versionMap || event.version >= targetVersion) {
      return event; // No upcasting needed
    }

    let currentEvent = { ...event } as DomainEvent;
    let currentVersion = event.version;
    
    // Keep upcasting until we reach the target version or can't go any further
    while (versionMap.has(currentVersion) && currentVersion < targetVersion) {
      const nextVersion = versionMap.get(currentVersion)!;
      
      // Don't go beyond the target version
      if (nextVersion > targetVersion) {
        break;
      }
      
      const key = `${eventType}:${currentVersion}`;
      const upcasters = this.upcasters.get(key) || [];

      // Apply all upcasters for this version
      for (const upcaster of upcasters) {
        // Create metadata tracking for the original version
        if (!currentEvent.metadata) {
          currentEvent.metadata = {} as EventMetadata;
        }
        
        if (!currentEvent.metadata.originalVersion) {
          currentEvent.metadata.originalVersion = event.version;
        }
        
        // Apply the upcaster
        currentEvent = upcaster(currentEvent);
        
        // Ensure the version is updated
        currentEvent.version = nextVersion;
      }

      currentVersion = nextVersion;
    }

    return currentEvent;
  }
} 