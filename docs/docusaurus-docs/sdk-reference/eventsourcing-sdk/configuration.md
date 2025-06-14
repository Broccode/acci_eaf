---
sidebar_position: 3
title: Configuration
---

# Event Sourcing SDK Configuration

Detailed configuration options for the EAF Event Sourcing SDK.

## ⚙️ Basic Configuration

```yaml
eaf:
  eventsourcing:
    datasource-url: 'jdbc:postgresql://localhost:5432/eventstore'
    username: 'eventstore_user'
    password: '${DATABASE_PASSWORD}'
```

## 🏬 Event Store Configuration

### Database Settings

- `table-name`: Event store table name (default: "domain_events")
- `schema-name`: Database schema name (default: "public")
- `batch-size`: Batch size for event loading (default: 1000)

### Connection Pool

- `max-pool-size`: Maximum connection pool size
- `min-pool-size`: Minimum connection pool size
- `connection-timeout`: Connection timeout in milliseconds

## 📸 Snapshot Configuration

```yaml
eaf:
  eventsourcing:
    snapshots:
      enabled: true
      frequency: 100
      table-name: 'aggregate_snapshots'
      compression: gzip
```

## 🔧 Performance Tuning

### Event Loading

- `parallel-loading`: Enable parallel event loading
- `cache-size`: In-memory event cache size
- `prefetch-size`: Event prefetch batch size

### Serialization

- `serialization-format`: JSON or binary serialization
- `compression-enabled`: Enable event compression

---

_Complete configuration guide for the EAF Event Sourcing SDK._
