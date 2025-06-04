## Data Models (Conceptual for MVP)

Core domain objects will be defined using Kotlin data classes, adhering to DDD principles. These are conceptual and will be refined.

### IAM Service

* **Tenant:** `id (UUID)`, `name (String)`, `status (String)`, `adminEmails (List<String>)`
* **User:** `id (UUID)`, `tenantId (UUID)`, `username (String)`, `email (String)`, `hashedPassword (String)`, `roles (List<Role>)`, `status (String)`
* **Role:** `id (UUID)`, `tenantId (UUID)`, `name (String)`, `permissions (List<Permission>)`
* **Permission:** `id (UUID)`, `name (String)` (e.g., `user:create`, `tenant:edit`)

### Event Store (Event Structure)

* **PersistedEvent:** `eventId (UUID)`, `streamId (String)` (e.g., `aggregateType-aggregateId`), `aggregateId (String)`, `aggregateType (String)`, `sequenceNumber (Long)`, `tenantId (String)`, `eventType (String)`, `eventPayload (JSONB)`, `metadata (JSONB)`, `timestamp (TimestampUTC)`

### License Management Service

* **License:** `id (UUID)`, `tenantId (UUID)`, `productId (String)`, `type (String)`, `status (String)`, `expiryDate (Date)`, `activationKey (String)`, `hardwareBindingInfo (String/JSONB)`

### Feature Flag Service

* **FeatureFlag:** `key (String)`, `name (String)`, `description (String)`, `variations (List<Variation>)`, `defaultOnVariation (String)`, `defaultOffVariation (String)`, `rules (List<TargetingRule>)`, `environments (Map<String, EnvironmentConfig>)`
* **TargetingRule:** `conditions (List<Condition>)`, `variationToServe (String)`, `priority (Int)`
* **EnvironmentConfig:** `isEnabled (Boolean)`, `specificRules (List<TargetingRule>)`
