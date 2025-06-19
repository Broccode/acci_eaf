**Epic 2.5: Basic IAM & Multi-Tenancy - MVP**

- **Goal:** Establish core multi-tenancy structure, tenant self-management of local users, basic
  RBAC for a pilot application, and a streamlined single-tenant setup option.
- **User Stories:**
  1. **Story 2.5.1: SuperAdmin Tenant Provisioning via Control Plane MVP**
     - As an ACCI EAF Super Administrator, I want to use the Control Plane MVP to provision a new,
       isolated tenant and create its initial Tenant Administrator account, so that new tenants can
       be onboarded.
     - **Acceptance Criteria:**
       1. The Control Plane UI (designed by Jane) allows an authenticated SuperAdmin to input new
          Tenant Name and initial Tenant Admin email.
       2. The Control Plane Backend API securely calls the IAM Service to create the tenant entity
          and the initial Tenant Admin user entity.
       3. The IAM Service successfully creates these records in its PostgreSQL store, ensuring
          `tenant_id` uniqueness and data isolation for the new tenant.
       4. (Post-MVP integration or as part of a separate technical story) The corresponding NATS
          Account for the new tenant is provisioned dynamically based on the IAM Service action
          (e.g., via an event and a NATS provisioning service, as detailed in
          `Dynamic NATS Multi-Tenancy Provisioning`).
       5. The newly created initial Tenant Admin receives necessary credentials or an invitation
          link to set up their password and access their tenant within the Control Plane.
  2. **Story 2.5.2: Streamlined Single-Tenant EAF Setup**
     - As an Axians Operator deploying ACCI EAF for a customer who initially only needs a
       single-tenant setup, I want a simple, streamlined process (e.g., via deployment scripts or a
       Control Plane first-run wizard accessible to a bootstrap admin) to initialize the EAF with a
       default Superadmin account, a default tenant, and the Superadmin assigned as the Tenant
       Administrator for that tenant, so the system is quickly usable.
     - **Acceptance Criteria:**
       1. A documented script (e.g., shell script, Ansible playbook snippet) or a first-run UI
          wizard in the Control Plane is available.
       2. Upon execution/completion, a default SuperAdmin account is created with secure initial
          credentials.
       3. A default Tenant (e.g., \"DefaultTenant\") is created in the IAM service.
       4. The created SuperAdmin account is also configured as the initial Tenant Administrator for
          this default tenant.
  3. **Story 2.5.3: TenantAdmin Local User Management via Control Plane MVP**
     - As a Tenant Administrator, I want to use the Control Plane MVP to create and manage local
       users (e.g., set initial passwords via invite/reset flow, activate/deactivate accounts)
       exclusively for my tenant, so I can control user access to applications within my tenant.
     - **Acceptance Criteria:**
       1. A TenantAdmin, when logged into the Control Plane, can view a list of local users
          belonging only to their own tenant.
       2. The UI allows the TenantAdmin to create new local users by providing necessary details
          (e.g., email, name).
       3. The TenantAdmin can trigger actions like \"send password reset/invitation\" for users.
       4. The TenantAdmin can activate or deactivate user accounts within their tenant.
       5. The IAM Service backend APIs enforce that TenantAdmins can only perform these user
          management actions within their assigned tenant_id scope.
  4. **Story 2.5.4: EAF IAM SDK for Basic Local User Authentication & RBAC**
     - As a Backend Developer (Majlinda), I want to use the ACCI EAF IAM SDK to easily authenticate
       local users (e.g., via username/password or an EAF-issued token) against their specific
       tenant and to enforce basic Role-Based Access Control (based on a few pre-defined application
       roles like \"app_user\", \"app_admin\" manageable via Control Plane MVP) for these users, so
       that I can protect application resources within an EAF-based application.
     - **Acceptance Criteria:**
       1. The `eaf-iam-client` SDK provides a clear mechanism for EAF services (acting as OAuth2
          resource servers or using Spring Security) to validate user credentials (e.g., an
          EAF-issued JWT) against the IAM service, ensuring the validation is scoped to a specific
          `tenant_id` present in the request or token.
       2. The SDK provides a simple API (e.g., annotations like
          `@PreAuthorize(\"hasRole('tenant_app_admin')\")` or utility methods like
          `iamContext.hasRole(\"app_user\")`) for checking if an authenticated user possesses
          specific roles.
       3. The IAM service allows SuperAdmins or TenantAdmins (via Control Plane MVP) to define basic
          application-level roles (e.g., \"PILOT_APP_USER\", \"PILOT_APP_ADMIN\") and assign these
          roles to users within a tenant.
       4. A protected resource within the pilot application (Epic 2.8) successfully demonstrates
          authentication and RBAC enforcement using the EAF IAM SDK.
  5. **Story 2.5.5 (was PRD Story 2.5.5): Reliable EAF Context Propagation**
     - As an ACCI EAF service/component, I need reliable access to the current `tenant_id` and basic
       user context (like `user_id`, roles) from the IAM system (e.g., via validated tokens or an
       SDK utility), so I can enforce tenant data isolation and make context-aware decisions.
     - **Acceptance Criteria:**
       1. The EAF IAM SDK (e.g., `eaf-iam-client` integrated with Spring Security) MUST provide a
          secure and clearly defined method (e.g., through Spring Security\'s
          `SecurityContextHolder`, custom `Authentication` object, or request-scoped beans populated
          via a servlet filter/interceptor that validates an incoming EAF JWT) for EAF services to
          reliably obtain the current, authenticated `tenant_id`, `user_id`, and associated roles
          for the request.
       2. This security context, especially the `tenant_id`, MUST be consistently and securely
          propagated by other core EAF SDKs (e.g., the eventing SDK automatically adding `tenant_id`
          to NATS message metadata, the event sourcing SDK ensuring `tenant_id` is recorded with
          persisted events and used in queries) to enable robust multi-tenancy enforcement at the
          data and messaging layers.
       3. Mechanisms for context propagation must function reliably across asynchronous operations
          within a service (e.g., when using Kotlin coroutines, context must be passed to new
          coroutines; when handling events from NATS, the `tenant_id` from message metadata must be
          correctly established in the processing context).
