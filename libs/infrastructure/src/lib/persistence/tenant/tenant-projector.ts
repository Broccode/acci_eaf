import { EntityManager } from '@mikro-orm/core';
import { Injectable, Inject } from '@nestjs/common';
import {
  DomainEvent,
  IdempotencyStore,
  TenantCreatedEvent,
  TenantActivatedEvent,
  TenantDeactivatedEvent,
  TenantSuspendedEvent,
  TenantUpdatedEvent,
  TenantStatus
} from 'core';
import { TenantReadModel } from '../entities/tenant-read-model.entity';

/**
 * Projector that handles tenant events and updates the read model
 */
@Injectable()
export class TenantProjector {
  private readonly processorId = 'TenantProjector';

  constructor(
    private readonly em: EntityManager,
    @Inject('IdempotencyStore')
    private readonly idempotencyStore: IdempotencyStore
  ) {}

  /**
   * Process a tenant event
   */
  async processEvent(event: DomainEvent): Promise<void> {
    // Check if this event has already been processed
    const processed = await this.idempotencyStore.hasBeenProcessed(
      event.id,
      this.processorId
    );

    if (processed) {
      return; // Skip processing if already processed
    }

    try {
      // Process the event based on its type
      switch (event.type) {
        case 'TenantCreated':
          await this.handleTenantCreated(event as TenantCreatedEvent);
          break;
        case 'TenantActivated':
          await this.handleTenantActivated(event as TenantActivatedEvent);
          break;
        case 'TenantDeactivated':
          await this.handleTenantDeactivated(event as TenantDeactivatedEvent);
          break;
        case 'TenantSuspended':
          await this.handleTenantSuspended(event as TenantSuspendedEvent);
          break;
        case 'TenantUpdated':
          await this.handleTenantUpdated(event as TenantUpdatedEvent);
          break;
      }

      // Mark the event as processed
      await this.idempotencyStore.markAsProcessed(event.id, this.processorId, {
        processedAt: new Date(),
        eventType: event.type
      });
    } catch (error) {
      // Log the error but don't re-throw it
      console.error(`Error processing tenant event ${event.type}:`, error);
    }
  }

  /**
   * Handle TenantCreated event
   */
  private async handleTenantCreated(event: TenantCreatedEvent): Promise<void> {
    const readModel = new TenantReadModel();
    readModel.id = event.aggregateId;
    readModel.name = event.payload.name;
    readModel.description = event.payload.description;
    readModel.contactEmail = event.payload.contactEmail;
    readModel.configuration = event.payload.configuration;
    readModel.status = TenantStatus.ACTIVE;  // Default to active
    readModel.createdAt = event.occurredAt;
    readModel.updatedAt = event.occurredAt;

    await this.em.persistAndFlush(readModel);
  }

  /**
   * Handle TenantActivated event
   */
  private async handleTenantActivated(event: TenantActivatedEvent): Promise<void> {
    const readModel = await this.em.findOne(TenantReadModel, { id: event.aggregateId });
    if (!readModel) {
      throw new Error(`Tenant read model not found for ID ${event.aggregateId}`);
    }

    readModel.status = TenantStatus.ACTIVE;
    readModel.updatedAt = event.occurredAt;

    await this.em.flush();
  }

  /**
   * Handle TenantDeactivated event
   */
  private async handleTenantDeactivated(event: TenantDeactivatedEvent): Promise<void> {
    const readModel = await this.em.findOne(TenantReadModel, { id: event.aggregateId });
    if (!readModel) {
      throw new Error(`Tenant read model not found for ID ${event.aggregateId}`);
    }

    readModel.status = TenantStatus.INACTIVE;
    readModel.updatedAt = event.occurredAt;

    await this.em.flush();
  }

  /**
   * Handle TenantSuspended event
   */
  private async handleTenantSuspended(event: TenantSuspendedEvent): Promise<void> {
    const readModel = await this.em.findOne(TenantReadModel, { id: event.aggregateId });
    if (!readModel) {
      throw new Error(`Tenant read model not found for ID ${event.aggregateId}`);
    }

    readModel.status = TenantStatus.SUSPENDED;
    readModel.updatedAt = event.occurredAt;

    await this.em.flush();
  }

  /**
   * Handle TenantUpdated event
   */
  private async handleTenantUpdated(event: TenantUpdatedEvent): Promise<void> {
    const readModel = await this.em.findOne(TenantReadModel, { id: event.aggregateId });
    if (!readModel) {
      throw new Error(`Tenant read model not found for ID ${event.aggregateId}`);
    }

    if (event.payload.name !== undefined) {
      readModel.name = event.payload.name;
    }
    if (event.payload.description !== undefined) {
      readModel.description = event.payload.description;
    }
    if (event.payload.contactEmail !== undefined) {
      readModel.contactEmail = event.payload.contactEmail;
    }
    if (event.payload.configuration !== undefined) {
      readModel.configuration = event.payload.configuration;
    }
    readModel.updatedAt = event.payload.updatedAt;

    await this.em.flush();
  }
} 