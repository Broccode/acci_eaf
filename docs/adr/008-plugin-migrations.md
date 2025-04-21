# ADR-008: Handling Plugin Database Migrations (Offline Deployment)

* **Status:** Accepted
* **Date:** 2025-04-21
* **Stakeholders:** [Names or roles]

## Context and Problem Statement

Plugins can define their own MikroORM entities. In the offline deployment scenario (tarball), database migrations affecting both core and plugin entities must be executable reliably without requiring code generation or external tools at runtime.

## Considered Options

1. **Plugins Provide Own Migration Scripts:** Each plugin ships SQL scripts that must be run manually.
    * *Pros:* Isolated.
    * *Cons:* Order unclear, no central history, extremely error-prone, incompatible with ORM migration tools.
2. **Central Migrations via MikroORM CLI:** All entities (Core + Plugins) are discovered by the central MikroORM config. Migrations are generated centrally *during development* (`mikro-orm migration:create`). These migration files become part of the build and are shipped in the tarball. A script in the tarball then executes `mikro-orm migration:up` inside the container.
    * *Pros:* Uses standard MikroORM tooling, single consistent migration history, automatic application of all necessary changes.
    * *Cons:* Requires the central MikroORM config to reliably find *all* potential plugin entities via glob patterns at build time. Process for developers must be clear.

## Decision

We choose **Option 2: Central Migrations via MikroORM CLI**. The MikroORM configuration will be designed to discover all entities in the project. Migrations will be generated and versioned centrally. The `update.sh`/`setup.sh` script in the tarball will be responsible for running `npm run migration:up` (or the equivalent MikroORM CLI command) inside the application container to bring the database up to date.

## Consequences

* Configuration of Entity Discovery (`entitiesDirs` in MikroORM config) must be robust and cover all relevant paths (including in `libs/plugins` or `apps`) after the build (`dist/...`).
* Developers must generate and commit migrations centrally.
* The update script in the tarball gains an additional critical task. Error handling for migrations within the script is important.
