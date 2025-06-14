---
sidebar_position: 2
title: API Reference
---

# Event Sourcing SDK API Reference

Complete API documentation for the EAF Event Sourcing SDK.

## 🏬 EventStore

Core interface for storing and retrieving events.

### Methods

#### saveEvents(aggregateId: Any, expectedVersion: Long, events: List&lt;DomainEvent&gt;)

Saves events for an aggregate with optimistic concurrency control.

#### loadEvents(aggregateId: Any, fromVersion: Long): List&lt;DomainEvent&gt;

Loads events for an aggregate from a specific version.

## 📦 EventSourcedRepository

Repository interface for event sourced aggregates.

### Methods

#### load(id: ID): T

Loads an aggregate by replaying its events.

#### save(aggregate: T)

Saves uncommitted events from an aggregate.

## 🏗️ EventSourcedAggregate

Base class for event sourced aggregates.

### Methods

#### apply(event: DomainEvent)

Applies an event to the aggregate state.

#### getUncommittedEvents(): List&lt;DomainEvent&gt;

Returns events that haven't been persisted yet.

## 📸 SnapshotStore

Interface for storing aggregate snapshots.

### Methods

#### saveSnapshot(aggregate: Any)

Saves a snapshot of an aggregate.

#### loadSnapshot(aggregateId: Any): Snapshot?

Loads the latest snapshot for an aggregate.

---

_Complete API reference for the EAF Event Sourcing SDK._
