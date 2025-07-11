## API Reference (Conceptual for MVP)

Detailed API specifications (e.g., OpenAPI for REST, .proto for gRPC) will be developed for each
service. This section outlines conceptual APIs for key MVP services. All EAF APIs must be designed
with multi-tenancy, security (AuthN/AuthZ via IAM service), and versioning in mind.

### External APIs Consumed

- **External IdPs (SAML, OIDC, LDAP/AD):**
  - Purpose: User authentication and attribute sourcing for federation.
  - Interaction: IAM service will act as a client (SP for SAML, RP for OIDC, LDAP client).
  - Link to Official Docs: Specific to each IdP chosen by tenants/Axians.
- **DPCM `corenode` (gRPC):**
  - Purpose: Integration for DPCM product functionality.
  - Interaction: A dedicated EAF service or adapter within a DPCM product application will
    communicate with `corenode` via gRPC. API contracts TBD based on `corenode` interfaces.

### Internal APIs Provided (EAF Core Services - Conceptual)

- **IAM Service API (REST/Hilla):**
  - Purpose: Manage tenants (SuperAdmin), users within tenants (TenantAdmin), roles, permissions.
    Validate tokens.
  - Endpoints (Implemented in Story 2.5.3):
    - `POST /api/v1/tenants` (SuperAdmin: Create Tenant - Implemented in 2.5.1)
    - `POST /api/v1/users` (TenantAdmin: Create User in own tenant)
      - **Request:** `{ "tenantId": "string", "email": "string", "username": "string?" }`
      - **Response:** `{ "userId": "string", ... }`
    - `GET /api/v1/users?tenantId={tenantId}` (TenantAdmin: List users in own tenant)
      - **Response:** `{ "tenantId": "string", "users": [UserSummary] }`
    - `PUT /api/v1/users/{userId}/status` (TenantAdmin: Update user status in own tenant)
      - **Request:** `{ "newStatus": "ACTIVE|INACTIVE|SUSPENDED" }`
      - **Response:** `{ "userId": "string", "previousStatus": "string", "newStatus": "string" }`
    - `POST /api/v1/auth/token` (Conceptual: Issue EAF token)
    - `GET /api/v1/auth/introspect` (Conceptual: Validate EAF token)
- **License Management Service API (REST/Hilla):**
  - Purpose: Manage license creation, activation, validation.
  - Endpoints (Conceptual):
    - `POST /api/v1/licenses` (SuperAdmin: Create License)
    - `POST /api/v1/tenants/{tenantId}/licenses/{licenseId}/activate`
    - `GET /api/v1/tenants/{tenantId}/licenses/{licenseId}/validate`
- **Feature Flag Service API (REST):**
  - Purpose: For SDKs to fetch flag configurations and for management UI to update flags. (Based on
    `Feature Flag Management Best Practices`).
  - Endpoints (Conceptual):
    - `GET /api/v1/flags?sdkKey=...&context=...` (SDK: Evaluate flags)
    - `POST /api/v1/management/flags` (Admin: Create flag)
- **Eventing SDK (NATS - Not a direct API, but interaction patterns):**
  - Purpose: Publish and subscribe to domain events.
  - Interaction: Via `eaf-eventing-sdk` using NATS client, tenant-scoped subjects.
- **Event Store SDK (Internal - Not a direct API, but interaction patterns):**
  - Purpose: Persist and retrieve event-sourced aggregate events.
  - Interaction: Via `eaf-eventsourcing-sdk`.
