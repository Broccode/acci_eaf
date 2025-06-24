-- V2: Create roles and permissions tables
-- Supports both platform-wide and tenant-scoped roles with hierarchical permissions

-- Permissions table (platform-wide permissions)
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Roles table (supports both platform and tenant scopes)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('PLATFORM', 'TENANT')),
    tenant_id UUID, -- NULL for platform-wide roles
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Ensure unique role names within scope (platform-wide or per tenant)
    CONSTRAINT unique_platform_role_name
        EXCLUDE (name WITH =) WHERE (scope = 'PLATFORM'),
    CONSTRAINT unique_tenant_role_name
        EXCLUDE (name WITH =, tenant_id WITH =) WHERE (scope = 'TENANT')
);

-- Role-Permission junction table
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    PRIMARY KEY (role_id, permission_id)
);

-- Indexes for performance
CREATE INDEX idx_roles_scope ON roles(scope);
CREATE INDEX idx_roles_tenant_id ON roles(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_permissions_resource_action ON permissions(resource, action);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Insert default platform permissions
INSERT INTO permissions (name, description, resource, action) VALUES
    ('SUPER_ADMIN', 'Full platform administration access', 'PLATFORM', 'ALL'),
    ('PLATFORM_ADMIN', 'Platform administration excluding system config', 'PLATFORM', 'ADMIN'),
    ('TENANT_ADMIN', 'Full tenant administration within scope', 'TENANT', 'ADMIN'),
    ('USER_MANAGER', 'Manage users within tenant', 'USER', 'MANAGE'),
    ('USER_VIEWER', 'View users within tenant', 'USER', 'READ'),
    ('ROLE_MANAGER', 'Manage roles within tenant', 'ROLE', 'MANAGE'),
    ('ROLE_VIEWER', 'View roles within tenant', 'ROLE', 'READ'),
    ('AUDIT_VIEWER', 'View audit logs', 'AUDIT', 'READ'),
    ('SYSTEM_MONITOR', 'Monitor system health and metrics', 'SYSTEM', 'MONITOR');

-- Insert default platform roles
INSERT INTO roles (name, description, scope) VALUES
    ('SUPER_ADMIN', 'System super administrator with full access', 'PLATFORM'),
    ('PLATFORM_ADMIN', 'Platform administrator with limited system access', 'PLATFORM');

-- Insert default tenant role template
INSERT INTO roles (name, description, scope) VALUES
    ('TENANT_ADMIN', 'Default tenant administrator role', 'TENANT'),
    ('USER_MANAGER', 'Can manage users within tenant', 'TENANT'),
    ('USER_VIEWER', 'Can view users within tenant', 'TENANT');

-- Assign permissions to platform roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND r.scope = 'PLATFORM';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'PLATFORM_ADMIN' AND r.scope = 'PLATFORM'
    AND p.name IN ('PLATFORM_ADMIN', 'TENANT_ADMIN', 'USER_MANAGER', 'USER_VIEWER', 'ROLE_VIEWER', 'AUDIT_VIEWER');

-- Assign permissions to tenant role templates
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'TENANT_ADMIN' AND r.scope = 'TENANT'
    AND p.name IN ('TENANT_ADMIN', 'USER_MANAGER', 'ROLE_MANAGER', 'AUDIT_VIEWER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'USER_MANAGER' AND r.scope = 'TENANT'
    AND p.name IN ('USER_MANAGER', 'USER_VIEWER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'USER_VIEWER' AND r.scope = 'TENANT'
    AND p.name = 'USER_VIEWER';
