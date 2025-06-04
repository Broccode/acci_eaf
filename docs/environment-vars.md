## Environment Variables

Specific environment variables for ACCI EAF services are managed by the EAF's configuration management system and are defined per service as needed.

Key considerations for environment variables include:

* **Secrets Management:** Environment variables are a primary method for supplying secrets (e.g., database credentials, API keys, signing keys) to EAF services, especially in containerized environments. These should not be hardcoded in application source or configuration files checked into version control.
* **Service Configuration:** General operational parameters for services (e.g., connection strings for NATS, PostgreSQL if not covered by secrets, log levels, feature flag SDK keys) may also be supplied via environment variables or a configuration service.

Refer to individual service documentation and deployment guides for specific environment variables required by each component. The EAF's scaffolding CLI and standard deployment configurations will provide defaults or placeholders where appropriate.
