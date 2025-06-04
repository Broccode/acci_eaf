## Infrastructure and Deployment Overview

* **Cloud Provider(s):** ACCI EAF is designed to be deployable in \"less cloud-native\" customer environments, implying flexibility beyond specific cloud providers. Primary deployment targets are customer-managed VMs or infrastructure where Docker and potentially Ansible can be used.
* **Core Services Used (Self-Hosted/Managed):**
  * NATS/JetStream Cluster (for Eventing Bus).
  * PostgreSQL Cluster (for Event Store, Read Models, IAM data, License data, etc.).
  * EAF Core Services (IAM, Licensing, Feature Flags, Control Plane Backend, etc.) deployed as Docker containers.
* **Infrastructure as Code (IaC):**
  * Ansible will be used for EAF infrastructure management and setup in target environments where applicable.
  * Dockerfiles and Docker Compose files will be provided for all EAF services and for setting up local/dev environments.
* **Deployment Strategy:**
  * EAF services will be packaged as Docker images.
  * Deployment to target environments (VMs) will likely involve Docker Compose for simpler setups or custom scripts orchestrating Docker container deployments, potentially managed by Ansible.
  * CI/CD pipelines (e.g., GitHub Actions as per MVP scope) will build Docker images and manage deployment artifacts.
* **Environments:** Standard Development, Staging (or QA), and Production environments are anticipated for EAF itself and for products built upon it. Configuration management will handle environment-specific settings.
* **Environment Promotion:** Artifacts (Docker images) promoted through environments via CI/CD, with appropriate testing and approval gates.
* **Rollback Strategy:** For containerized deployments, rollback typically involves redeploying a previous stable Docker image version. Database schema migrations (if any) will require careful forward-compatible design and potentially separate rollback scripts/procedures. Event Sourced systems have specific considerations for handling \"bad\" events, possibly involving corrective events or re-processing.
