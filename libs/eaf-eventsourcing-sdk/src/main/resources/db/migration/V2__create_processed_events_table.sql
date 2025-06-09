-- V2__create_processed_events_table.sql
-- EAF Processed Events Table for Projector Idempotency

-- Create the processed_events table for tracking processed events by projectors
CREATE TABLE processed_events (
    projector_name VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),

    -- Primary key ensures uniqueness per projector, event, and tenant
    PRIMARY KEY (projector_name, event_id, tenant_id)
);

-- Create indexes for efficient querying
CREATE INDEX idx_processed_events_tenant ON processed_events (tenant_id);
CREATE INDEX idx_processed_events_projector_tenant ON processed_events (projector_name, tenant_id);
CREATE INDEX idx_processed_events_timestamp ON processed_events (processed_at);

-- Add comments for documentation
COMMENT ON TABLE processed_events IS 'Tracks processed events by projectors to ensure idempotency';

COMMENT ON COLUMN processed_events.projector_name IS 'Name of the projector that processed the event';
COMMENT ON COLUMN processed_events.event_id IS 'UUID of the domain event that was processed';
COMMENT ON COLUMN processed_events.tenant_id IS 'Tenant identifier for multi-tenancy isolation';
COMMENT ON COLUMN processed_events.processed_at IS 'Timestamp when the event was processed';
