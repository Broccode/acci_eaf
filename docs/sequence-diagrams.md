## Core Workflow / Sequence Diagrams (Conceptual for MVP)

1. **New Tenant Provisioning (Simplified):**
    * SuperAdmin (via Control Plane UI) -> Control Plane Backend API -> IAM Service (`createTenant` command)
    * IAM Service:
        * Validates request.
        * Generates Tenant ID.
        * Creates Tenant entity in IAM DB.
        * Creates initial Tenant Admin User entity in IAM DB.
        * (Optionally) Publishes `TenantCreatedEvent` to NATS.
        * (Future) Calls NATS Provisioning logic to create NATS Account for tenant (as per `Dynamic NATS Multi-Tenancy Provisioning`).
    * Control Plane UI receives confirmation.
2. **Basic CQRS Flow (Illustrative - e.g., Product App using EAF):**
    * User (via Product App UI) -> Product App Backend API (`CreateProduct` command with product details, tenant context)
    * API Adapter -> Command Gateway (Axon)
    * Command Gateway -> `ProductAggregate` Command Handler (within Product App Backend)
    * `ProductAggregate`: Validates command, applies `ProductCreatedEvent`.
    * EAF Event Sourcing SDK (used by Axon Repository):
        * Persists `ProductCreatedEvent` to PostgreSQL Event Store (with `tenantId`, `aggregateId`, `sequenceNumber`).
    * EAF Eventing SDK (hooked into Axon's Unit of Work or via Transactional Outbox):
        * Publishes `ProductCreatedEvent` (JSON) to NATS topic (e.g., `product.tenantId.events.created`).
    * Product Read Model Projector (subscribes to NATS topic via EAF Eventing SDK):
        * Consumes `ProductCreatedEvent`.
        * Updates its PostgreSQL read model table (e.g., `products_view`) with new product details.
    * User (via Product App UI) later queries for product list.
    * Product App Backend API (`ListProducts` query) -> Query Gateway (Axon) -> Query Handler -> Reads from `products_view` read model -> Returns data to UI.
