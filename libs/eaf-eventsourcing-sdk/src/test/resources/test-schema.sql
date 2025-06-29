-- Test schema for EAF Event Sourcing SDK integration tests
-- This mirrors the production event store schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Domain events table for storing domain events
CREATE TABLE IF NOT EXISTS domain_events (
    global_sequence_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL DEFAULT uuid_generate_v4(),
    stream_id VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    expected_version BIGINT,
    sequence_number BIGINT NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB,
    timestamp_utc TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_domain_events_aggregate_sequence UNIQUE (aggregate_id, sequence_number, tenant_id),
    CONSTRAINT uk_domain_events_event_id UNIQUE (event_id)
);

-- Aggregate snapshots table for storing aggregate snapshots
CREATE TABLE IF NOT EXISTS aggregate_snapshots (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    last_sequence_number BIGINT NOT NULL,
    snapshot_payload_jsonb JSONB NOT NULL,
    version BIGINT NOT NULL,
    timestamp_utc TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_aggregate_snapshots_tenant_aggregate UNIQUE (tenant_id, aggregate_id)
);

-- Token store table for Axon tracking tokens
CREATE TABLE IF NOT EXISTS token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment INTEGER NOT NULL,
    token BYTEA,
    token_type VARCHAR(255),
    timestamp VARCHAR(255),
    owner VARCHAR(255),

    PRIMARY KEY (processor_name, segment)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_domain_events_tenant_timestamp ON domain_events (tenant_id, timestamp_utc);
CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate_tenant ON domain_events (aggregate_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_domain_events_type_tenant ON domain_events (event_type, tenant_id);
CREATE INDEX IF NOT EXISTS idx_domain_events_global_sequence ON domain_events (global_sequence_id);

CREATE INDEX IF NOT EXISTS idx_aggregate_snapshots_tenant_timestamp ON aggregate_snapshots (tenant_id, timestamp_utc);
CREATE INDEX IF NOT EXISTS idx_aggregate_snapshots_aggregate_tenant ON aggregate_snapshots (aggregate_id, tenant_id);

-- Test data for multiple tenants
INSERT INTO domain_events (stream_id, aggregate_id, aggregate_type, expected_version, sequence_number, tenant_id, event_type, payload, metadata)
VALUES
    ('test-aggregate-1', 'test-aggregate-1', 'TestAggregate', 0, 1, 'TENANT_A', 'TestEvent', '{"message": "Test event 1"}', '{"correlation_id": "test-123"}'),
    ('test-aggregate-1', 'test-aggregate-1', 'TestAggregate', 1, 2, 'TENANT_A', 'TestEvent', '{"message": "Test event 2"}', '{"correlation_id": "test-123"}'),
    ('test-aggregate-2', 'test-aggregate-2', 'TestAggregate', 0, 1, 'TENANT_B', 'TestEvent', '{"message": "Test event for tenant B"}', '{"correlation_id": "test-456"}');

INSERT INTO aggregate_snapshots (aggregate_id, tenant_id, aggregate_type, last_sequence_number, snapshot_payload_jsonb, version)
VALUES
    ('test-aggregate-1', 'TENANT_A', 'TestAggregate', 2, '{"state": "test state"}', 2);
