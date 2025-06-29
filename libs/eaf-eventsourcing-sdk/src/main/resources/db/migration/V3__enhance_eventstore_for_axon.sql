-- V3__enhance_eventstore_for_axon.sql
-- Enhance EAF Event Store Schema for Axon Framework 4.11.2 Compatibility

-- Add new columns to domain_events table for Axon Framework compatibility
-- All columns are nullable initially for backward compatibility
ALTER TABLE domain_events
ADD COLUMN IF NOT EXISTS payload_type VARCHAR(255),
ADD COLUMN IF NOT EXISTS payload_revision VARCHAR(10);

-- Update existing events with default values for new columns
-- Set payload_type from event_type for backward compatibility
UPDATE domain_events
SET payload_type = event_type
WHERE payload_type IS NULL;

-- Set default payload_revision for existing events
UPDATE domain_events
SET payload_revision = '1.0'
WHERE payload_revision IS NULL;

-- Create performance optimization indexes for Axon Framework queries

-- Composite index for tenant + global sequence (TrackingEventProcessor performance)
CREATE INDEX IF NOT EXISTS idx_events_tenant_sequence
ON domain_events (tenant_id, global_sequence_id);

-- Enhanced aggregate-specific index including sequence_number
CREATE INDEX IF NOT EXISTS idx_events_aggregate_enhanced
ON domain_events (tenant_id, stream_id, sequence_number);

-- Tracking processor index for resume scenarios
CREATE INDEX IF NOT EXISTS idx_events_tracking
ON domain_events (tenant_id, global_sequence_id, timestamp_utc);

-- Payload type index for event type queries
CREATE INDEX IF NOT EXISTS idx_events_payload_type
ON domain_events (tenant_id, payload_type);

-- Optimize existing indexes by adding tenant_id where missing for multi-tenant performance
-- Drop and recreate indexes to ensure optimal ordering

-- Enhanced stream_id index with tenant isolation
DROP INDEX IF EXISTS idx_stream_id_seq;
CREATE INDEX idx_stream_id_seq_tenant
ON domain_events (tenant_id, stream_id, sequence_number);

-- Enhanced event type index (already has tenant_id, but ensure optimal column order)
DROP INDEX IF EXISTS idx_tenant_event_type;
CREATE INDEX idx_tenant_event_type_enhanced
ON domain_events (tenant_id, event_type, timestamp_utc);

-- Enhanced aggregate index (already has tenant_id, but add sequence for better performance)
DROP INDEX IF EXISTS idx_tenant_aggregate;
CREATE INDEX idx_tenant_aggregate_enhanced
ON domain_events (tenant_id, aggregate_id, sequence_number);

-- Add comments for new columns and indexes
COMMENT ON COLUMN domain_events.payload_type IS 'Event payload type for Axon Framework compatibility (typically same as event_type)';
COMMENT ON COLUMN domain_events.payload_revision IS 'Event schema version for payload evolution (Axon Framework compatibility)';

-- Add index comments for documentation
COMMENT ON INDEX idx_events_tenant_sequence IS 'Optimized for Axon TrackingEventProcessor queries by tenant and global sequence';
COMMENT ON INDEX idx_events_aggregate_enhanced IS 'Optimized for aggregate event loading with tenant isolation';
COMMENT ON INDEX idx_events_tracking IS 'Optimized for TrackingEventProcessor resume scenarios';
COMMENT ON INDEX idx_events_payload_type IS 'Optimized for event type filtering queries';
COMMENT ON INDEX idx_stream_id_seq_tenant IS 'Enhanced stream query index with tenant isolation';
COMMENT ON INDEX idx_tenant_event_type_enhanced IS 'Enhanced event type queries with timestamp ordering';
COMMENT ON INDEX idx_tenant_aggregate_enhanced IS 'Enhanced aggregate queries with sequence ordering';

-- Create partial indexes for high-performance scenarios (PostgreSQL specific)
-- Note: Partial indexes with time-based predicates are commented out as they require
-- immutable functions. These can be created manually in production if needed.

-- Example of how to create time-based partial index (manual execution required):
-- CREATE INDEX idx_events_recent
-- ON domain_events (tenant_id, global_sequence_id, timestamp_utc)
-- WHERE timestamp_utc > '2024-01-01'::timestamptz;

-- Partial index for specific event types that are frequently queried
-- This can be customized based on application-specific patterns
CREATE INDEX IF NOT EXISTS idx_events_high_frequency_types
ON domain_events (tenant_id, event_type, timestamp_utc)
WHERE event_type IN ('UserCreatedEvent', 'UserActivatedEvent', 'UserDeactivatedEvent');

-- Create trigger function to auto-populate new columns for backward compatibility
CREATE OR REPLACE FUNCTION populate_axon_columns()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto-populate payload_type from event_type if not provided
    IF NEW.payload_type IS NULL THEN
        NEW.payload_type := NEW.event_type;
    END IF;

    -- Auto-populate payload_revision with default value if not provided
    IF NEW.payload_revision IS NULL THEN
        NEW.payload_revision := '1.0';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-populate columns before insert
CREATE TRIGGER trigger_populate_axon_columns
    BEFORE INSERT ON domain_events
    FOR EACH ROW
    EXECUTE FUNCTION populate_axon_columns();

-- Add constraint to ensure payload_type consistency (after trigger is in place)
-- This helps maintain data quality for Axon Framework compatibility
ALTER TABLE domain_events
ADD CONSTRAINT chk_payload_type_not_empty
CHECK (payload_type IS NOT NULL AND LENGTH(TRIM(payload_type)) > 0);

-- Add constraint for payload_revision format
ALTER TABLE domain_events
ADD CONSTRAINT chk_payload_revision_format
CHECK (payload_revision IS NOT NULL AND payload_revision ~ '^[0-9]+\\.[0-9]+$');

-- Analyze table to update statistics for query planner
ANALYZE domain_events;

-- Log migration completion
DO $$
BEGIN
    RAISE NOTICE 'V3 Migration completed: Enhanced event store schema for Axon Framework 4.11.2 compatibility';
    RAISE NOTICE 'Added columns: payload_type, payload_revision';
    RAISE NOTICE 'Created indexes: 5 new performance indexes for multi-tenant Axon queries';
    RAISE NOTICE 'Enhanced indexes: 3 existing indexes optimized for tenant isolation';
    RAISE NOTICE 'Added constraints: payload_type and payload_revision validation';
    RAISE NOTICE 'Note: Time-based partial indexes require manual creation with specific timestamps';
END $$;
