---
sidebar_position: 8
title: Database Schema V3 - Axon Framework Enhancement
---

# Database Schema V3 - Axon Framework Enhancement

## Overview

The V3 migration enhances the EAF Event Store schema to provide full compatibility with Axon
Framework 4.11.2 while maintaining backward compatibility with existing EAF SDK operations.

## Schema Changes

### New Columns

The `domain_events` table has been enhanced with two new columns:

| Column             | Type         | Nullable | Description                                         |
| ------------------ | ------------ | -------- | --------------------------------------------------- |
| `payload_type`     | VARCHAR(255) | Yes\*    | Event payload type for Axon Framework compatibility |
| `payload_revision` | VARCHAR(10)  | Yes\*    | Event schema version for payload evolution          |

\*Initially nullable for backward compatibility, but populated with constraints

### Column Mapping

The new columns are mapped as follows for backward compatibility:

- `payload_type` ← `event_type` (for existing events)
- `payload_revision` ← `'1.0'` (default for existing events)

## Performance Optimizations

### New Indexes

The migration creates several new indexes optimized for Axon Framework query patterns:

#### Primary Performance Indexes

1. **`idx_events_tenant_sequence`**

   - Columns: `(tenant_id, global_sequence_id)`
   - Purpose: Optimized for TrackingEventProcessor queries
   - Usage: High-performance event streaming by global sequence

2. **`idx_events_aggregate_enhanced`**

   - Columns: `(tenant_id, stream_id, sequence_number)`
   - Purpose: Fast aggregate event loading with tenant isolation
   - Usage: Aggregate reconstruction and event replay

3. **`idx_events_tracking`**

   - Columns: `(tenant_id, global_sequence_id, timestamp_utc)`
   - Purpose: TrackingEventProcessor resume scenarios
   - Usage: Event processor position tracking and recovery

4. **`idx_events_payload_type`**
   - Columns: `(tenant_id, payload_type)`
   - Purpose: Event type filtering queries
   - Usage: Event type-specific queries and projections

#### Enhanced Existing Indexes

The migration also optimizes existing indexes for better multi-tenant performance:

1. **`idx_stream_id_seq_tenant`** (replaces `idx_stream_id_seq`)

   - Columns: `(tenant_id, stream_id, sequence_number)`
   - Enhancement: Added tenant isolation

2. **`idx_tenant_event_type_enhanced`** (replaces `idx_tenant_event_type`)

   - Columns: `(tenant_id, event_type, timestamp_utc)`
   - Enhancement: Added timestamp ordering

3. **`idx_tenant_aggregate_enhanced`** (replaces `idx_tenant_aggregate`)
   - Columns: `(tenant_id, aggregate_id, sequence_number)`
   - Enhancement: Added sequence ordering

#### Specialized Indexes

1. **`idx_events_recent`** (Partial Index)

   - Columns: `(tenant_id, global_sequence_id, timestamp_utc)`
   - Condition: `timestamp_utc > (CURRENT_TIMESTAMP - INTERVAL '30 days')`
   - Purpose: Optimized access to recent events (commonly accessed data)

2. **`idx_events_high_frequency_types`** (Partial Index)
   - Columns: `(tenant_id, event_type, timestamp_utc)`
   - Condition: Specific high-frequency event types
   - Purpose: Optimized queries for commonly accessed event types

## Data Integrity Constraints

### Payload Type Constraint

```sql
ALTER TABLE domain_events
ADD CONSTRAINT chk_payload_type_not_empty
CHECK (payload_type IS NOT NULL AND LENGTH(TRIM(payload_type)) > 0);
```

### Payload Revision Format Constraint

```sql
ALTER TABLE domain_events
ADD CONSTRAINT chk_payload_revision_format
CHECK (payload_revision IS NOT NULL AND payload_revision ~ '^[0-9]+\.[0-9]+$');
```

## Axon Framework Compatibility

### Event Storage Engine Requirements

The enhanced schema meets all Axon Framework 4.11.2 requirements:

1. **Aggregate Identifier**: Uses existing `aggregate_id` column
2. **Sequence Number**: Uses existing `sequence_number` column
3. **Event Type**: Maps `payload_type` to Axon's event type requirements
4. **Payload**: Uses existing `payload` JSONB column
5. **Metadata**: Uses existing `metadata` JSONB column
6. **Timestamp**: Uses existing `timestamp_utc` column
7. **Global Sequence**: Uses existing `global_sequence_id` column

### Multi-Tenant Support

All indexes include `tenant_id` as the first column to ensure:

- Efficient partition pruning
- Tenant data isolation
- Optimal query performance in multi-tenant scenarios

## Migration Safety

### Backward Compatibility

- All existing EAF SDK operations continue to work unchanged
- New columns are initially nullable to prevent breaking existing applications
- Database trigger automatically populates new columns when they are not provided
- Constraints are added after trigger is in place to ensure consistency
- Existing indexes are enhanced rather than completely replaced where possible

### Data Migration Strategy

1. **Add new columns** as nullable
2. **Populate existing data** with appropriate default values
3. **Create trigger function** to auto-populate new columns for backward compatibility
4. **Add constraints** after trigger is in place
5. **Create new indexes** for performance optimization
6. **Update statistics** for query planner optimization

### Rollback Support

The migration is designed to be reversible:

- New columns can be dropped without affecting existing functionality
- New indexes can be removed without breaking existing queries
- Constraints can be removed if needed

## Performance Impact

### Write Performance

- Minimal impact due to efficient index design
- New columns add negligible storage overhead
- Constraints provide fast validation

### Read Performance

- Significant improvement for Axon Framework queries
- Enhanced multi-tenant query performance
- Optimized aggregate loading and event streaming

### Storage Impact

- New columns: ~20 bytes per event (typical)
- New indexes: ~15-25% increase in total index storage
- Partial indexes minimize storage overhead for specialized queries

## Usage Examples

### Axon Framework Queries

```sql
-- TrackingEventProcessor query (optimized by idx_events_tenant_sequence)
SELECT * FROM domain_events
WHERE tenant_id = 'tenant-1'
AND global_sequence_id > 1000
ORDER BY global_sequence_id
LIMIT 100;

-- Aggregate loading (optimized by idx_events_aggregate_enhanced)
SELECT * FROM domain_events
WHERE tenant_id = 'tenant-1'
AND stream_id = 'User-user-123'
ORDER BY sequence_number;
```

### EAF SDK Queries (still supported)

```sql
-- Existing EAF SDK pattern continues to work
SELECT * FROM domain_events
WHERE tenant_id = 'tenant-1'
AND aggregate_id = 'user-123'
ORDER BY sequence_number;
```

## Monitoring and Maintenance

### Index Usage Monitoring

```sql
-- Monitor index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'domain_events'
ORDER BY idx_scan DESC;
```

### Performance Analysis

```sql
-- Analyze query performance
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM domain_events
WHERE tenant_id = 'tenant-1'
AND global_sequence_id > 1000;
```

### Maintenance Tasks

- Regular `ANALYZE domain_events` to update statistics
- Monitor partial index effectiveness
- Consider index maintenance during low-traffic periods

## Migration Checklist

- [ ] Backup database before migration
- [ ] Test migration on staging environment
- [ ] Verify application compatibility
- [ ] Monitor performance after migration
- [ ] Update application documentation
- [ ] Train team on new capabilities

## See Also

- [Axon Framework 4.11.2 Documentation](https://docs.axoniq.io/axon-framework-reference/4.11/)
- [PostgreSQL Index Optimization](https://www.postgresql.org/docs/current/indexes.html)
- [EAF Event Sourcing SDK](../sdk-reference/eventsourcing-sdk/getting-started.md)
