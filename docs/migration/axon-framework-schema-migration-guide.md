# Axon Framework Schema Migration Guide

## Overview

This guide covers the migration from EAF Event Store Schema V2 to V3, which adds Axon Framework
4.11.2 compatibility while maintaining backward compatibility with existing EAF SDK operations.

## Pre-Migration Checklist

### Environment Preparation

- [ ] **Backup Production Database**: Create full backup before migration
- [ ] **Test on Staging**: Run migration on staging environment first
- [ ] **Verify Dependencies**: Ensure Flyway is properly configured
- [ ] **Check Disk Space**: Ensure sufficient space for new indexes (~25% of current event store
      size)
- [ ] **Monitor Resources**: Plan for temporary increased CPU/IO during migration

### Application Preparation

- [ ] **Review Current Usage**: Identify all applications using the event store
- [ ] **Update Dependencies**: Ensure all EAF SDK versions are compatible
- [ ] **Plan Downtime**: Schedule maintenance window if required
- [ ] **Prepare Rollback**: Have rollback plan ready

## Migration Process

### Step 1: Pre-Migration Validation

```sql
-- Check current schema version
SELECT version FROM flyway_schema_history ORDER BY installed_on DESC LIMIT 1;

-- Verify data integrity
SELECT COUNT(*) as total_events,
       COUNT(DISTINCT tenant_id) as tenant_count,
       MIN(timestamp_utc) as oldest_event,
       MAX(timestamp_utc) as newest_event
FROM domain_events;

-- Check for any constraint violations that might block migration
SELECT aggregate_id, sequence_number, COUNT(*)
FROM domain_events
GROUP BY aggregate_id, sequence_number
HAVING COUNT(*) > 1;
```

### Step 2: Execute Migration

#### Using Nx (Recommended)

```bash
# Run migration via Nx
nx run eaf-eventsourcing-sdk:flyway-migrate

# Verify migration success
nx run eaf-eventsourcing-sdk:flyway-info
```

#### Manual Flyway Execution

```bash
# Navigate to eventsourcing SDK
cd libs/eaf-eventsourcing-sdk

# Run Flyway migration
flyway migrate

# Check migration status
flyway info
```

### Step 3: Post-Migration Validation

```sql
-- Verify new columns exist and are populated
SELECT
    COUNT(*) as total_events,
    COUNT(payload_type) as events_with_payload_type,
    COUNT(payload_revision) as events_with_payload_revision
FROM domain_events;

-- Check new indexes exist
SELECT indexname, tablename
FROM pg_indexes
WHERE tablename = 'domain_events'
AND indexname LIKE 'idx_events_%'
ORDER BY indexname;

-- Verify constraints are active
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'domain_events'
AND constraint_type = 'CHECK';
```

## Performance Monitoring

### Query Performance Verification

```sql
-- Test TrackingEventProcessor query performance
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM domain_events
WHERE tenant_id = 'your-tenant-id'
AND global_sequence_id > 1000
ORDER BY global_sequence_id
LIMIT 100;

-- Test aggregate loading performance
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM domain_events
WHERE tenant_id = 'your-tenant-id'
AND stream_id = 'User-user-123'
ORDER BY sequence_number;
```

### Index Usage Monitoring

```sql
-- Monitor index usage over time
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'domain_events'
ORDER BY idx_scan DESC;
```

## Application Updates

### EAF SDK Applications

**No changes required** - existing applications will continue to work unchanged.

### New Axon Framework Applications

For new applications using Axon Framework, you can now:

1. **Use Enhanced Indexes**: Queries will automatically benefit from new indexes
2. **Leverage Payload Versioning**: Use `payload_revision` for event evolution
3. **Optimize Event Type Queries**: Use `payload_type` for efficient filtering

## Troubleshooting

### Common Issues

#### Migration Fails with Constraint Violation

```sql
-- Check for invalid data that might violate new constraints
SELECT * FROM domain_events
WHERE event_type IS NULL OR LENGTH(TRIM(event_type)) = 0;

-- Fix invalid data before re-running migration
UPDATE domain_events
SET event_type = 'UnknownEvent'
WHERE event_type IS NULL OR LENGTH(TRIM(event_type)) = 0;
```

#### Index Creation Takes Too Long

```sql
-- Monitor index creation progress
SELECT
    pid,
    now() - pg_stat_activity.query_start AS duration,
    query
FROM pg_stat_activity
WHERE state = 'active'
AND query LIKE '%CREATE INDEX%';
```

#### Insufficient Disk Space

```sql
-- Check current database size
SELECT
    pg_size_pretty(pg_database_size(current_database())) as database_size,
    pg_size_pretty(pg_total_relation_size('domain_events')) as events_table_size;

-- Estimate space needed for indexes (rough calculation)
SELECT pg_size_pretty(pg_total_relation_size('domain_events') * 0.25) as estimated_index_space;
```

### Performance Issues

#### Slow Queries After Migration

```sql
-- Update table statistics
ANALYZE domain_events;

-- Check if query planner is using new indexes
EXPLAIN (ANALYZE, BUFFERS) your_query_here;
```

#### High Write Latency

```sql
-- Monitor index maintenance overhead
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'domain_events'
AND idx_scan = 0  -- Unused indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

## Rollback Procedure

### Emergency Rollback (if needed)

```sql
-- 1. Drop new constraints
ALTER TABLE domain_events DROP CONSTRAINT IF EXISTS chk_payload_type_not_empty;
ALTER TABLE domain_events DROP CONSTRAINT IF EXISTS chk_payload_revision_format;

-- 2. Drop new indexes
DROP INDEX IF EXISTS idx_events_tenant_sequence;
DROP INDEX IF EXISTS idx_events_aggregate_enhanced;
DROP INDEX IF EXISTS idx_events_tracking;
DROP INDEX IF EXISTS idx_events_payload_type;
DROP INDEX IF EXISTS idx_events_recent;
DROP INDEX IF EXISTS idx_events_high_frequency_types;

-- 3. Recreate original indexes
CREATE INDEX idx_stream_id_seq ON domain_events (stream_id, sequence_number);
CREATE INDEX idx_tenant_event_type ON domain_events (tenant_id, event_type);
CREATE INDEX idx_tenant_aggregate ON domain_events (tenant_id, aggregate_id);

-- 4. Drop new columns (optional - only if absolutely necessary)
-- ALTER TABLE domain_events DROP COLUMN IF EXISTS payload_type;
-- ALTER TABLE domain_events DROP COLUMN IF EXISTS payload_revision;

-- 5. Update Flyway schema history to mark V3 as failed
-- (This requires careful consideration and should be done with Flyway tools)
```

## Validation Scripts

### Data Integrity Check

```sql
-- Comprehensive data integrity check
WITH integrity_check AS (
    SELECT
        COUNT(*) as total_events,
        COUNT(DISTINCT tenant_id) as tenant_count,
        COUNT(DISTINCT aggregate_id) as aggregate_count,
        COUNT(payload_type) as events_with_payload_type,
        COUNT(payload_revision) as events_with_payload_revision,
        COUNT(*) - COUNT(payload_type) as missing_payload_type,
        COUNT(*) - COUNT(payload_revision) as missing_payload_revision
    FROM domain_events
)
SELECT
    *,
    CASE
        WHEN missing_payload_type = 0 AND missing_payload_revision = 0
        THEN 'PASS'
        ELSE 'FAIL'
    END as integrity_status
FROM integrity_check;
```

### Performance Baseline

```sql
-- Create performance baseline for future comparison
CREATE TABLE IF NOT EXISTS migration_performance_baseline (
    test_name VARCHAR(255),
    query_description TEXT,
    execution_time_ms NUMERIC,
    rows_examined INTEGER,
    buffers_hit INTEGER,
    buffers_read INTEGER,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Record baseline measurements
-- (Run your typical queries with EXPLAIN (ANALYZE, BUFFERS) and record results)
```

## Communication Plan

### Team Notification Template

```
Subject: EAF Event Store Schema Migration V3 - Axon Framework Compatibility

Team,

We will be performing a database schema migration to add Axon Framework 4.11.2 compatibility to the EAF Event Store.

**When**: [DATE/TIME]
**Duration**: Estimated [X] minutes
**Impact**:
- No application changes required
- Brief performance impact during migration
- Improved query performance after completion

**What's Changing**:
- New columns: payload_type, payload_revision
- Enhanced indexes for better performance
- Full backward compatibility maintained

**Action Required**:
- None for existing applications
- Monitor application performance after migration
- Report any issues immediately

**Rollback Plan**: Available if needed

Questions? Contact [CONTACT_INFO]
```

## Success Criteria

- [ ] Migration completes without errors
- [ ] All existing applications continue functioning
- [ ] New columns are properly populated
- [ ] All indexes are created successfully
- [ ] Query performance meets or exceeds baseline
- [ ] No data loss or corruption
- [ ] Constraints are properly enforced

## Monitoring Dashboard

Consider setting up monitoring for:

1. **Query Performance**: Track execution times for common queries
2. **Index Usage**: Monitor which indexes are being used
3. **Storage Growth**: Track database size changes
4. **Error Rates**: Monitor for constraint violations or query failures
5. **Application Health**: Ensure all dependent applications remain healthy

## Support Contacts

- **Database Team**: [contact info]
- **EAF Team**: [contact info]
- **On-Call Engineer**: [contact info]
- **Emergency Escalation**: [contact info]

---

_Last Updated: [DATE]_ _Migration Version: V3_ _Document Version: 1.0_
