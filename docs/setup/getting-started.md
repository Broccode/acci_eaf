# ACCI EAF - Setup Guide

## 1. Introduction

    * Purpose of this document (Installation & local setup of EAF/Sample App).
    * Overview of the ACCI EAF and the monorepo structure.
    * Target audience (Developers at Axians).

## 2. Prerequisites

    * **Software:**
        * Node.js (Specify specific LTS version, e.g., >= 18.x).
        * Package Manager: npm / yarn / pnpm (Specify preferred manager and version).
        * Docker & Docker Compose (Current versions).
        * Git Client.
        * (Optional) PostgreSQL Client (e.g., psql, DBeaver, pgAdmin) for database inspection.
        * (Optional) Redis Client (e.g., redis-cli) for cache inspection.
        * (Optional) Nx CLI globally installed (`npm install -g nx`) or use via `npx nx ...`.
    * **Hardware:** (Minimum requirements for RAM, CPU if relevant).
    * **Access Rights:** Potentially access to the Axians Git repository, private NPM registry (if used).

## 3. Getting Started

    * Clone the Repository (`git clone <repository-url>`).
    * Change into the project directory (`cd acci-eaf-monorepo`).
    * Install Dependencies (Root `package.json`, command: `npm install` or `yarn install` etc.).
    * Brief explanation of the main directories (`apps/`, `libs/`, `docs/`, `tools/`).

## 4. Configuration

    * `.env` Files:
        * Create local `.env` file(s) from `.env.example` (e.g., in root, in `apps/control-plane-api`, `apps/sample-app`).
        * Explanation of key environment variables:
            * `DATABASE_URL` (for MikroORM).
            * `REDIS_URL`.
            * `JWT_SECRET`, `JWT_EXPIRATION`.
            * Logging level (`LOG_LEVEL`).
            * Ports for the applications (e.g., `CONTROL_PLANE_PORT`, `SAMPLE_APP_PORT`).
            * (Potentially variables for license file path, AuthN providers etc.).
    * Reference to the `@nestjs/config` module.

## 5. Running the Local Development Environment

    * **Start Dependencies (Docker Compose):**
        * Command: `docker-compose up -d` (starts PostgreSQL, Redis etc. from the `docker-compose.yml` in the root).
        * Verify containers are running (`docker ps`).
    * **Run Database Migrations:**
        * Command: `npx nx run infrastructure-persistence:migration:up` (Example, actual command depends on Nx config and MikroORM CLI) or `npm run migration:up`. Explain that this initializes/updates the schema.
    * **Start Applications (Nx):**
        * Control Plane API: `npx nx serve control-plane-api`.
        * Sample Application: `npx nx serve sample-app`.
        * Expected ports and log output.
    * **First Admin / Bootstrap (Control Plane):**
        * Execute the CLI command (ADR-007): `npx nx run control-plane-api:cli --args="setup-admin --email admin@example.com --password VERYsecurePASSWORD"` (Example).
    * **Test Access:**
        * Example `curl` calls or Postman requests to health checks or simple endpoints.

## 6. Running Tests

    * Run all tests: `npx nx run-many --target=test --all`.
    * Run tests for a specific project: `npx nx test <project-name>` (e.g., `nx test core-domain`).
    * Run only unit tests / integration tests (if separate targets are configured or via filename patterns).
    * Run E2E tests: `npx nx e2e <app-name>-e2e`.
    * Viewing Code Coverage reports (command and report location).

## 7. Building Applications

    * Build a specific application: `npx nx build <app-name>`.
    * Build all affected projects: `npx nx affected:build`.
    * Explanation of the `dist/` directory.

## 8. Deployment (Offline / Tarball - For Product Deployments)

    * **8.1 Creating the Package (CI/CD Step):**
        * Brief explanation of how the tarball is built in CI/CD (`docker save`, scripts etc.). (This is more informative for developers).
    * **8.2 Installation on Target VM (Customer Scenario):**
        * Prerequisites on the VM (Docker, Docker Compose).
        * Transfer & extract tarball.
        * Load Docker Images (`docker load -i ...`).
        * Configure `.env` file.
        * Execute setup script (`setup.sh`) (what does it do? incl. migrations).
        * Start with `docker-compose up -d`.
    * **8.3 Updating on Target VM (Customer Scenario):**
        * Transfer & extract new tarball.
        * Execute update script (`update.sh`) (what does it do? `docker load`, `docker-compose down`, backup note, **run migrations**, `docker-compose up -d`).
    * **8.4 Running Database Migrations (Manual/Script):**
        * Detailed command to run migrations inside the container (e.g., `docker-compose run --rm <app_service_name> npm run migration:up`).
    * **8.5 Backup & Restore (Recommendations):**
        * Note on the importance of backing up Docker volumes (esp. Postgres).
        * Recommended strategies or reference to external tools/documentation.

## 9. Troubleshooting

    * Common setup or startup problems and their solutions (e.g., port conflicts, DB connection errors, migration issues).
    * How to retrieve logs from Docker Compose (`docker-compose logs`).
    * Where to get further help (internal contacts, channels).
