---
sidebar_position: 3
title: Configuration
---

# Eventing SDK Configuration

Detailed configuration options for the EAF Eventing SDK.

## ⚙️ Basic Configuration

```yaml
eaf:
  eventing:
    nats-url: 'nats://localhost:4222'
    cluster-id: 'eaf-cluster'
    client-id: 'my-service'
```

## 🔧 Advanced Configuration

### Connection Settings

- `connection-timeout`: Connection timeout in milliseconds
- `max-reconnects`: Maximum reconnection attempts
- `reconnect-wait`: Wait time between reconnections

### Publishing Settings

- `publish-timeout`: Timeout for publish operations
- `async-publish`: Enable asynchronous publishing

### Retry Configuration

- `retry-attempts`: Number of retry attempts
- `retry-backoff`: Backoff strategy for retries

## 🌍 Environment Variables

Override configuration using environment variables:

- `NATS_URL`
- `NATS_CLUSTER_ID`
- `NATS_CLIENT_ID`

---

_Complete configuration guide for the EAF Eventing SDK._
