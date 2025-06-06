-- V1__create_eventstore_tables.sql
-- EAF Event Store Tables for PostgreSQL

-- Create the domain_events table for storing event-sourced events
CREATE TABLE domain_events (
    global_sequence_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    stream_id VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    expected_version BIGINT,
    sequence_number BIGINT NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB,
    timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),

    -- Unique constraint to ensure optimistic concurrency control
    CONSTRAINT uk_tenant_aggregate_sequence UNIQUE (tenant_id, aggregate_id, sequence_number)
);

-- Create indexes for efficient querying
CREATE INDEX idx_stream_id_seq ON domain_events (stream_id, sequence_number);
CREATE INDEX idx_tenant_event_type ON domain_events (tenant_id, event_type);
CREATE INDEX idx_tenant_aggregate ON domain_events (tenant_id, aggregate_id);
CREATE INDEX idx_timestamp_utc ON domain_events (timestamp_utc);

-- Create the aggregate_snapshots table for storing aggregate snapshots
CREATE TABLE aggregate_snapshots (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    last_sequence_number BIGINT NOT NULL,
    snapshot_payload_jsonb JSONB NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    timestamp_utc TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),

    -- Unique constraint to ensure only one snapshot per aggregate per tenant
    CONSTRAINT uk_tenant_aggregate_snapshot UNIQUE (tenant_id, aggregate_id)
);

-- Create indexes for efficient snapshot querying
CREATE INDEX idx_snapshot_tenant_aggregate ON aggregate_snapshots (tenant_id, aggregate_id);
CREATE INDEX idx_snapshot_timestamp ON aggregate_snapshots (timestamp_utc);

-- Add comments for documentation
COMMENT ON TABLE domain_events IS 'Stores domain events for event sourcing with tenant isolation';
COMMENT ON TABLE aggregate_snapshots IS 'Stores aggregate snapshots for performance optimization';

COMMENT ON COLUMN domain_events.global_sequence_id IS 'Global sequence ID for all events across all tenants';
COMMENT ON COLUMN domain_events.event_id IS 'Unique identifier for each domain event';
COMMENT ON COLUMN domain_events.stream_id IS 'Stream identifier (typically aggregateType-aggregateId)';
COMMENT ON COLUMN domain_events.aggregate_id IS 'Identifier of the aggregate that produced this event';
COMMENT ON COLUMN domain_events.aggregate_type IS 'Type/class name of the aggregate';
COMMENT ON COLUMN domain_events.expected_version IS 'Expected version for optimistic concurrency control';
COMMENT ON COLUMN domain_events.sequence_number IS 'Sequence number within the aggregate stream';
COMMENT ON COLUMN domain_events.tenant_id IS 'Tenant identifier for multi-tenancy';
COMMENT ON COLUMN domain_events.event_type IS 'Type/class name of the domain event';
COMMENT ON COLUMN domain_events.payload IS 'JSON payload of the domain event';
COMMENT ON COLUMN domain_events.metadata IS 'Additional metadata for the event';

COMMENT ON COLUMN aggregate_snapshots.last_sequence_number IS 'Last sequence number included in this snapshot';
COMMENT ON COLUMN aggregate_snapshots.snapshot_payload_jsonb IS 'JSON representation of the aggregate state';
COMMENT ON COLUMN aggregate_snapshots.version IS 'Version of the snapshot for schema evolution';
