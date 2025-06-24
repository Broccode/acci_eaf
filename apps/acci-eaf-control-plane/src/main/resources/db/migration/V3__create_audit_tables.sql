-- Migration V3: Create comprehensive audit trail tables
-- This migration creates the audit_entries table with proper indexing for performance

-- Create audit_entries table for storing all audit trail entries
CREATE TABLE audit_entries (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    performed_by VARCHAR(36) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id VARCHAR(255) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}',
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    session_id VARCHAR(255),
    correlation_id VARCHAR(255)
);

-- Add check constraints for data integrity
ALTER TABLE audit_entries ADD CONSTRAINT check_audit_severity
    CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL'));

ALTER TABLE audit_entries ADD CONSTRAINT check_audit_action_not_empty
    CHECK (action <> '');

ALTER TABLE audit_entries ADD CONSTRAINT check_audit_target_type_not_empty
    CHECK (target_type <> '');

ALTER TABLE audit_entries ADD CONSTRAINT check_audit_target_id_not_empty
    CHECK (target_id <> '');

ALTER TABLE audit_entries ADD CONSTRAINT check_audit_ip_address_not_empty
    CHECK (ip_address <> '');

ALTER TABLE audit_entries ADD CONSTRAINT check_audit_user_agent_not_empty
    CHECK (user_agent <> '');

-- Add constraint to ensure failed actions have error messages
ALTER TABLE audit_entries ADD CONSTRAINT check_audit_error_message
    CHECK (success = true OR (success = false AND error_message IS NOT NULL));

-- Create indexes for performance optimization

-- Primary query patterns: by tenant, by time, by action
CREATE INDEX idx_audit_entries_tenant_timestamp
    ON audit_entries (tenant_id, timestamp DESC);

CREATE INDEX idx_audit_entries_timestamp
    ON audit_entries (timestamp DESC);

CREATE INDEX idx_audit_entries_action
    ON audit_entries (action);

CREATE INDEX idx_audit_entries_target
    ON audit_entries (target_type, target_id);

CREATE INDEX idx_audit_entries_performed_by
    ON audit_entries (performed_by);

-- Security and monitoring focused indexes
CREATE INDEX idx_audit_entries_severity
    ON audit_entries (severity)
    WHERE severity IN ('ERROR', 'CRITICAL');

CREATE INDEX idx_audit_entries_failed
    ON audit_entries (success, timestamp DESC)
    WHERE success = false;

CREATE INDEX idx_audit_entries_security_events
    ON audit_entries (action, timestamp DESC)
    WHERE action IN ('LOGIN_SUCCESSFUL', 'LOGIN_FAILED', 'LOGOUT', 'ACCESS_DENIED', 'PERMISSION_ESCALATION_ATTEMPTED');

-- Session and correlation tracking
CREATE INDEX idx_audit_entries_session_id
    ON audit_entries (session_id)
    WHERE session_id IS NOT NULL;

CREATE INDEX idx_audit_entries_correlation_id
    ON audit_entries (correlation_id)
    WHERE correlation_id IS NOT NULL;

-- IP address tracking for security analysis
CREATE INDEX idx_audit_entries_ip_address
    ON audit_entries (ip_address, timestamp DESC);

-- Composite index for tenant-scoped queries with filtering
CREATE INDEX idx_audit_entries_tenant_action_timestamp
    ON audit_entries (tenant_id, action, timestamp DESC);

CREATE INDEX idx_audit_entries_tenant_target_timestamp
    ON audit_entries (tenant_id, target_type, target_id, timestamp DESC);

-- JSON details index for advanced querying (PostgreSQL specific)
CREATE INDEX idx_audit_entries_details_gin
    ON audit_entries USING GIN (details);

-- Add comments for documentation
COMMENT ON TABLE audit_entries IS 'Comprehensive audit trail for all system operations';
COMMENT ON COLUMN audit_entries.id IS 'Unique identifier for the audit entry';
COMMENT ON COLUMN audit_entries.tenant_id IS 'Tenant context for multi-tenancy support';
COMMENT ON COLUMN audit_entries.performed_by IS 'User who performed the action';
COMMENT ON COLUMN audit_entries.action IS 'Type of action performed (enum value)';
COMMENT ON COLUMN audit_entries.target_type IS 'Type of entity that was acted upon';
COMMENT ON COLUMN audit_entries.target_id IS 'Identifier of the specific entity';
COMMENT ON COLUMN audit_entries.details IS 'Additional context and metadata as JSON';
COMMENT ON COLUMN audit_entries.timestamp IS 'When the action occurred';
COMMENT ON COLUMN audit_entries.ip_address IS 'IP address from which action was performed';
COMMENT ON COLUMN audit_entries.user_agent IS 'User agent string from the request';
COMMENT ON COLUMN audit_entries.severity IS 'Severity level: INFO, WARNING, ERROR, CRITICAL';
COMMENT ON COLUMN audit_entries.success IS 'Whether the action completed successfully';
COMMENT ON COLUMN audit_entries.error_message IS 'Error details if action failed';
COMMENT ON COLUMN audit_entries.session_id IS 'Session identifier for tracking user sessions';
COMMENT ON COLUMN audit_entries.correlation_id IS 'Request correlation ID for distributed tracing';

-- Create a view for security events for easier monitoring
CREATE VIEW security_audit_events AS
SELECT
    id,
    tenant_id,
    performed_by,
    action,
    target_type,
    target_id,
    timestamp,
    ip_address,
    user_agent,
    severity,
    success,
    error_message,
    session_id,
    correlation_id,
    details
FROM audit_entries
WHERE action IN (
    'LOGIN_SUCCESSFUL',
    'LOGIN_FAILED',
    'LOGOUT',
    'ACCESS_DENIED',
    'PERMISSION_ESCALATION_ATTEMPTED'
) OR severity IN ('ERROR', 'CRITICAL');

COMMENT ON VIEW security_audit_events IS 'Filtered view of security-related audit events for monitoring';

-- Create a view for high-risk actions
CREATE VIEW high_risk_audit_events AS
SELECT
    id,
    tenant_id,
    performed_by,
    action,
    target_type,
    target_id,
    timestamp,
    ip_address,
    severity,
    success,
    error_message,
    details
FROM audit_entries
WHERE action IN (
    'TENANT_ARCHIVED',
    'USER_BULK_OPERATION',
    'ROLE_DELETED',
    'PERMISSION_DELETED',
    'CONFIGURATION_CHANGED',
    'BULK_DATA_IMPORT',
    'BULK_DATA_EXPORT'
) OR severity = 'CRITICAL';

COMMENT ON VIEW high_risk_audit_events IS 'High-risk administrative actions requiring attention';

-- Insert initial system audit entry to mark audit system activation
INSERT INTO audit_entries (
    id,
    tenant_id,
    performed_by,
    action,
    target_type,
    target_id,
    details,
    timestamp,
    ip_address,
    user_agent,
    severity,
    success
) VALUES (
    gen_random_uuid()::text,
    'system',
    'system',
    'SYSTEM_MAINTENANCE',
    'audit_system',
    'audit_tables_created',
    '{"migration": "V3", "description": "Audit system initialized"}',
    CURRENT_TIMESTAMP,
    '127.0.0.1',
    'Flyway Migration',
    'INFO',
    true
);
