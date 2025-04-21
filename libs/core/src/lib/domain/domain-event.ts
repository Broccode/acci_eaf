export interface DomainEvent {
  /**
   * Unique identifier for the event
   */
  id: string;
  
  /**
   * Type/name of the event
   */
  type: string;
  
  /**
   * Schema version of the event
   * Increments when the structure of the event changes
   */
  version: number;
  
  /**
   * When the event occurred
   */
  occurredAt: Date;
  
  /**
   * Unique identifier of the aggregate that produced the event
   */
  aggregateId: string;
  
  /**
   * Optional metadata for the event
   */
  metadata?: EventMetadata;
  
  /**
   * Payload of the event (specific to each event type)
   */
  payload: unknown;
}

export interface EventMetadata {
  /**
   * The correlation ID for tracking event chains across the system
   */
  correlationId?: string;
  
  /**
   * The ID of the user or system that triggered the event
   */
  causedBy?: string;
  
  /**
   * Original version if this event was upcasted from an older schema
   */
  originalVersion?: number;
  
  /**
   * Any additional custom metadata
   */
  [key: string]: unknown;
} 