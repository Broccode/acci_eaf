# Setup Guide

Welcome to the ACCI EAF Setup Guide. This section explains how to get the framework up and running on your local machine and in various deployment environments.

> **Status:** Draft – content placeholders marked with `TODO`.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Docker Compose Environment](#docker-compose-environment)
4. [Offline Tarball Installation](#offline-tarball-installation)
5. [Bootstrap Script](#bootstrap-script)
6. [Troubleshooting](#troubleshooting)
7. [SBOM Generation](#sbom-generation)

---

## Prerequisites

To effectively work with the ACCI EAF, ensure your development environment meets the following prerequisites:

- **Node.js:** Version 20 or higher. (Verify with `node -v`)
- **npm:** Version 9 or higher (typically comes with Node.js). (Verify with `npm -v`)
- **Docker:** Version 24 or higher. (Verify with `docker --version`)
- **Git:** For version control. (Verify with `git --version`)
- **Nx CLI (Optional but Recommended):** If you plan to use Nx commands directly, install it globally or use `npx`. (Verify with `nx --version`)

## Local Development

Follow these steps to set up the ACCI EAF for local development:

1. **Clone the Repository:**

    ```bash
    git clone <repository-url>
    cd acci-eaf
    ```

2. **Install Dependencies:**

    ```bash
    npm ci --legacy-peer-deps
    ```

    *Note: `--legacy-peer-deps` might be needed depending on the current dependency tree structure.*
3. **Environment Configuration:**
    - Copy the example environment file:

      ```bash
      cp .env.example .env
      ```

    - Adjust the variables in `.env` as needed (e.g., database credentials, ports).
4. **Start Docker Services (if not already running):**
    The framework typically relies on PostgreSQL and Redis. Ensure these are running, for example, via the provided Docker Compose setup (see next section).
5. **Run Database Migrations:**

    ```bash
    npx nx run-many --target=migration:run --all
    # Or for a specific application, e.g., control-plane-api:
    # npx nx run control-plane-api:migration:run
    ```

6. **Start an Application (e.g., `control-plane-api`):**

    ```bash
    npx nx serve control-plane-api
    ```

    The application will typically be available at `http://localhost:3000` (or as configured).

## Docker Compose Environment

The ACCI EAF includes a `docker-compose.yml` file to simplify the setup of external services like PostgreSQL and Redis.

1. **Ensure Docker is running.**
2. **Navigate to the project root directory.**
3. **Start the services:**

    ```bash
    docker-compose up -d
    ```

    This will start PostgreSQL and Redis in detached mode.
4. **To stop the services:**

    ```bash
    docker-compose down
    ```

5. **To view logs:**

    ```bash
    docker-compose logs -f <service_name> # e.g., postgresql
    ```

    Refer to the `docker-compose.yml` file for service names and configurations. The default database credentials and ports are usually defined in this file or in the referenced `.env` file.

## Offline Tarball Installation

The ACCI EAF supports an offline installation method using a tarball package, which is particularly useful for air-gapped environments. This process is aligned with FR-DEPLOY.

1. **Build the Tarball (Developer Task):**
    A CI/CD process or a manual script will generate a tarball (e.g., `acci-eaf-offline-vX.Y.Z.tar.gz`). This package includes:
    - Docker images (saved using `docker save`) for all applications and services.
    - Application bundles and dependencies.
    - Setup and update scripts.
    - The `docker-compose.yml` configured for offline use.
2. **Transfer the Tarball:**
    Securely transfer the tarball to the target offline environment.
3. **Installation Steps on Target Machine:**
    a.  **Load Docker Images:**
        ```bash
        # Example command, actual script might differ
        tar -xzf acci-eaf-offline-vX.Y.Z.tar.gz
        cd acci-eaf-offline
        ./load-images.sh # This script would use 'docker load < image.tar'
        ```
    b.  **Configure Environment:**
        Set up the `.env` file with appropriate configurations for the target environment.
    c.  **Run Setup Script:**
        An installation script (e.g., `install.sh` or `setup.sh`) will:
        - Start necessary services using `docker-compose -f docker-compose.offline.yml up -d`.
        - Execute database migrations.
        - Perform any initial data seeding or bootstrap procedures.
    Refer to the specific instructions bundled with the tarball for precise commands.

## Bootstrap Script

The Control Plane API requires an initial administrative user and potentially other core data to be set up. This is handled by a bootstrap script or process as defined in ADR-007.

- **Execution:** This script is typically run once after the initial deployment or a major update.

    ```bash
    # Example command (actual command might be an Nx target or a direct script execution)
    npx nx run control-plane-api:bootstrap
    ```

- **Functionality:**
  - Creates the first administrative user for the Control Plane.
  - May set up default roles or permissions.
  - Seeds any other essential initial data.
- **Security:** The credentials for the initial admin user are usually provided via secure environment variables or will be displayed upon script completion and should be changed immediately.

    Refer to ADR-007 and the Control Plane API documentation for more details on the bootstrap process.

## Troubleshooting

TODO: Collect common pitfalls and solutions.

## SBOM Generation

You can locally generate a Software Bill of Materials (SBOM) in CycloneDX JSON format:

```bash
npm run generate:sbom
# Output: dist/sbom.json
```

The command runs the Nx target `sbom:generate-sbom` which executes `cyclonedx-npm`. Ensure dependencies are installed (`npm ci --legacy-peer-deps`) beforehand.
