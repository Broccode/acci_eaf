-- Create ticket_summary read model table for optimized queries
-- This table is populated by the TicketSummaryProjector from domain events

CREATE TABLE ticket_summary (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    assignee_id VARCHAR(100),
    resolution TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,

    -- Metadata for projector tracking
    last_event_sequence_number BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Indexes for efficient querying
    CONSTRAINT fk_ticket_summary_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Create indexes for efficient queries
CREATE INDEX idx_ticket_summary_tenant_id ON ticket_summary(tenant_id);
CREATE INDEX idx_ticket_summary_status ON ticket_summary(status);
CREATE INDEX idx_ticket_summary_assignee ON ticket_summary(assignee_id);
CREATE INDEX idx_ticket_summary_priority ON ticket_summary(priority);
CREATE INDEX idx_ticket_summary_created_at ON ticket_summary(created_at);
CREATE INDEX idx_ticket_summary_tenant_status ON ticket_summary(tenant_id, status);

-- Create processed_events table for projector idempotency
-- This tracks which events each projector has already processed
CREATE TABLE IF NOT EXISTS processed_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projector_name VARCHAR(200) NOT NULL,
    event_id UUID NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Ensure we don't process the same event twice for the same projector
    CONSTRAINT uk_processed_events_projector_event UNIQUE (projector_name, event_id)
);

-- Index for efficient lookups during projector processing
CREATE INDEX idx_processed_events_projector_event ON processed_events(projector_name, event_id);
CREATE INDEX idx_processed_events_tenant ON processed_events(tenant_id);
