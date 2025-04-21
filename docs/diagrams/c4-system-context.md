# C4 System Context Diagram - ACCI EAF

* **Version:** 1.0 Draft
* **Date:** 2025-04-25
* **Status:** Draft

## System Context Diagram

```mermaid
C4Context
title System Context Diagram for ACCI EAF

Person(tenant_user, "Tenant User", "A user of an application built with the EAF, belongs to a specific tenant")
Person(tenant_admin, "Tenant Administrator", "Manages users, roles and permissions within a specific tenant")
Person(system_admin, "System Administrator", "Manages tenants and global settings")
Person(developer, "Application Developer", "Develops applications using the EAF")

System_Boundary(eaf, "ACCI EAF") {
    System(tenant_app, "Tenant Application", "Enterprise application built with EAF, supports multiple tenants")
    System(control_plane, "Control Plane API", "Manages tenants and global configuration")
}

System_Ext(license_system, "Axians License System", "Validates application licenses")
System_Ext(auth_provider, "External Auth Provider", "Optional external authentication (OAuth, OIDC)")
System_Ext(monitoring, "Monitoring Systems", "Metrics, logging, and alerting")

Rel(tenant_user, tenant_app, "Uses")
Rel(tenant_admin, tenant_app, "Manages tenant-specific settings, users, and roles")
Rel(system_admin, control_plane, "Manages tenants and global settings")
Rel(developer, eaf, "Builds applications using")

Rel(tenant_app, license_system, "Validates license", "HTTPS")
Rel(tenant_app, auth_provider, "Authenticates with (optional)", "HTTPS")
Rel(tenant_app, monitoring, "Sends metrics and logs", "HTTPS")
Rel(control_plane, monitoring, "Sends metrics and logs", "HTTPS")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Container Diagram

```mermaid
C4Container
title Container Diagram for ACCI EAF

Person(tenant_user, "Tenant User", "A user of an application built with the EAF")
Person(tenant_admin, "Tenant Administrator", "Manages tenant-specific settings")
Person(system_admin, "System Administrator", "Manages tenants and global settings")

System_Boundary(eaf, "ACCI EAF") {
    Container(tenant_api, "Tenant API", "NestJS", "RESTful API for tenant applications")
    Container(control_plane_api, "Control Plane API", "NestJS", "API for tenant management")
    
    ContainerDb(event_store, "Event Store", "PostgreSQL", "Stores domain events with tenant isolation")
    ContainerDb(read_models, "Read Models", "PostgreSQL", "Optimized data models for queries")
    ContainerDb(cache, "Cache", "Redis", "Caches frequently accessed data")
    
    Container(core_libs, "Core Libraries", "TypeScript", "Domain and application layers, core business logic")
    Container(infrastructure_libs, "Infrastructure Libraries", "TypeScript", "Adapters for databases, messaging, etc.")
    Container(tenancy_lib, "Tenancy Library", "TypeScript", "Multi-tenancy implementation")
    Container(rbac_lib, "RBAC Library", "TypeScript", "Role-based access control")
    Container(licensing_lib, "Licensing Library", "TypeScript", "License validation mechanisms")
}

System_Ext(license_system, "Axians License System", "Validates application licenses")
System_Ext(auth_provider, "External Auth Provider", "Optional external authentication")
System_Ext(monitoring, "Monitoring Systems", "Metrics, logging, and alerting")

Rel(tenant_user, tenant_api, "Uses", "HTTPS")
Rel(tenant_admin, tenant_api, "Manages tenant settings", "HTTPS")
Rel(system_admin, control_plane_api, "Manages tenants", "HTTPS")

Rel(tenant_api, core_libs, "Uses")
Rel(tenant_api, infrastructure_libs, "Uses")
Rel(tenant_api, tenancy_lib, "Uses")
Rel(tenant_api, rbac_lib, "Uses")
Rel(tenant_api, licensing_lib, "Uses")

Rel(control_plane_api, core_libs, "Uses")
Rel(control_plane_api, infrastructure_libs, "Uses")

Rel(core_libs, event_store, "Stores events", "SQL")
Rel(infrastructure_libs, read_models, "Reads/writes data", "SQL")
Rel(infrastructure_libs, cache, "Caches data", "Redis Protocol")

Rel(licensing_lib, license_system, "Validates license", "HTTPS")
Rel(tenant_api, auth_provider, "Authenticates with (optional)", "HTTPS")
Rel(tenant_api, monitoring, "Sends metrics and logs", "HTTPS")
Rel(control_plane_api, monitoring, "Sends metrics and logs", "HTTPS")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

## Component Diagram (Core Libraries)

```mermaid
C4Component
title Component Diagram for ACCI EAF Core Libraries

Container_Boundary(core_libs, "Core Libraries") {
    Component(domain_layer, "Domain Layer", "TypeScript", "Aggregates, entities, value objects, domain events")
    Component(application_layer, "Application Layer", "TypeScript", "Use cases, command/query handlers")
    
    Component(command_bus, "Command Bus", "TypeScript", "Routes commands to handlers")
    Component(query_bus, "Query Bus", "TypeScript", "Routes queries to handlers")
    Component(event_bus, "Event Bus", "TypeScript", "Distributes events to handlers")
    
    Component(aggregate_base, "Aggregate Base", "TypeScript", "Base class for domain aggregates")
    Component(repository_interfaces, "Repository Interfaces", "TypeScript", "Ports for persistence")
    Component(domain_services, "Domain Services", "TypeScript", "Complex domain operations")
}

Container(tenant_api, "Tenant API", "NestJS", "RESTful API for tenant applications")
ContainerDb(event_store, "Event Store", "PostgreSQL", "Stores domain events with tenant isolation")
ContainerDb(read_models, "Read Models", "PostgreSQL", "Optimized data models for queries")

Container_Boundary(infra_libs, "Infrastructure Libraries") {
    Component(postgres_event_store, "PostgreSQL Event Store", "TypeScript", "Event storage implementation")
    Component(read_model_repositories, "Read Model Repositories", "TypeScript", "MikroORM-based repositories")
    Component(nestjs_controllers, "NestJS Controllers", "TypeScript", "API endpoints")
}

Rel(tenant_api, nestjs_controllers, "Uses")
Rel(nestjs_controllers, command_bus, "Dispatches commands")
Rel(nestjs_controllers, query_bus, "Dispatches queries")

Rel(command_bus, application_layer, "Routes to")
Rel(query_bus, application_layer, "Routes to")
Rel(event_bus, application_layer, "Routes to")

Rel(application_layer, domain_layer, "Uses")
Rel(application_layer, repository_interfaces, "Uses")
Rel(domain_layer, aggregate_base, "Extends")
Rel(domain_layer, event_bus, "Publishes events to")

Rel(postgres_event_store, repository_interfaces, "Implements")
Rel(read_model_repositories, read_models, "Reads/writes", "SQL/MikroORM")
Rel(postgres_event_store, event_store, "Stores events", "SQL/MikroORM")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Component Diagram (Multi-Tenancy)

```mermaid
C4Component
title Component Diagram for ACCI EAF Multi-Tenancy

Container_Boundary(tenancy_lib, "Tenancy Library") {
    Component(tenant_context, "Tenant Context", "TypeScript", "AsyncLocalStorage-based tenant context")
    Component(tenant_middleware, "Tenant Middleware", "TypeScript", "Extracts tenant ID from request")
    Component(tenant_provider, "Tenant Provider", "TypeScript", "Injectable service for tenant context")
    Component(tenant_entity, "Tenant Entity", "TypeScript", "Tenant data model")
    Component(tenant_service, "Tenant Service", "TypeScript", "Tenant CRUD operations")
    Component(tenant_filter, "MikroORM Tenant Filter", "TypeScript", "Applies tenant_id filtering")
}

Container(tenant_api, "Tenant API", "NestJS", "RESTful API for tenant applications")
Container(control_plane_api, "Control Plane API", "NestJS", "API for tenant management")
ContainerDb(database, "Database", "PostgreSQL", "Stores tenant-isolated data")

Rel(tenant_api, tenant_middleware, "Uses")
Rel(tenant_middleware, tenant_context, "Sets tenant ID in")
Rel(tenant_api, tenant_provider, "Injects")
Rel(tenant_provider, tenant_context, "Retrieves from")
Rel(tenant_filter, tenant_context, "Gets tenant ID from")
Rel(tenant_filter, database, "Applies WHERE tenant_id = ? to queries")
Rel(control_plane_api, tenant_service, "Uses")
Rel(tenant_service, tenant_entity, "Manages")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

*Note: These diagrams should be viewed in a Markdown editor or renderer that supports Mermaid syntax. They use the C4 model notation which provides a hierarchical way to describe software architecture at different levels of detail.*
