---
sidebar_position: 2
title: API Reference
---

# Eventing SDK API Reference

Complete API documentation for the EAF Eventing SDK.

## 📡 NatsEventPublisher

Main interface for publishing events to NATS.

### Methods

#### publish(subject: String, event: Any)

Publishes an event to the specified subject.

#### publishWithMetadata(subject: String, event: Any, metadata: Map&lt;String, Any&gt;)

Publishes an event with additional metadata.

## 📥 EventHandler

Annotation for marking event handler methods.

### Parameters

- `subject`: The event subject to handle
- `queue`: Optional queue group for load balancing

## 🔧 Configuration Classes

### NatsEventingProperties

Configuration properties for NATS eventing.

### RetryPolicy

Retry configuration for failed events.

## 📋 Event Envelope

Wrapper for events with metadata and headers.

---

_Complete API reference for the EAF Eventing SDK._
