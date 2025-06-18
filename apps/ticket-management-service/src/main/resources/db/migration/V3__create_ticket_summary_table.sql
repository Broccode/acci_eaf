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
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient queries
CREATE INDEX idx_ticket_summary_tenant_id ON ticket_summary(tenant_id);
CREATE INDEX idx_ticket_summary_status ON ticket_summary(status);
CREATE INDEX idx_ticket_summary_assignee ON ticket_summary(assignee_id);
CREATE INDEX idx_ticket_summary_priority ON ticket_summary(priority);
CREATE INDEX idx_ticket_summary_created_at ON ticket_summary(created_at);
CREATE INDEX idx_ticket_summary_tenant_status ON ticket_summary(tenant_id, status);

-- Note: processed_events table and indexes already created by EAF SDK migration V2
