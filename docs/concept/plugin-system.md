# Plugin System in ACCI EAF

Version: 1.0
Date: 2025-05-07
Status: Published

## Introduction

The ACCI EAF features a plugin system designed to enhance its extensibility and modularity. This system allows developers to add new functionalities, integrate third-party services, or customize existing behaviors without modifying the core framework code. This document outlines the conceptual basis of the plugin system.

## Core Objectives

- **Extensibility:** Enable the addition of new features and capabilities to applications built on EAF.
- **Modularity:** Package distinct functionalities as self-contained plugins.
- **Maintainability:** Keep custom code separate from the core framework, simplifying updates and maintenance for both.
- **Reusability:** Allow plugins to be shared across different EAF-based applications if designed generically.

## Key Concepts

### 1. Plugin Definition

A plugin is typically a NestJS module (or a set of modules) that encapsulates a specific piece of functionality. It can include:

- NestJS Controllers (for new API endpoints)
- NestJS Services (for business logic)
- MikroORM Entities (for custom data models)
- MikroORM Migrations (for schema changes related to its entities)
- Configuration modules
- Custom providers, guards, interceptors, etc.

### 2. Plugin Interface (Conceptual)

While not necessarily a strict TypeScript interface that all plugins *must* implement (as NestJS modules themselves serve as a structural contract), plugins adhere to certain conventions for integration:

- They should be discoverable by the main application.
- They should expose a primary NestJS module that can be imported by the main application.
- They should manage their own dependencies.

### 3. Discovery and Loading

- **Mechanism:** The main application is configured to discover and load plugins. This can be achieved in several ways:
  - **Static Imports:** For tightly coupled or essential plugins, they can be directly imported into the main application module.
  - **Dynamic Imports/Convention-based Loading:** For more loosely coupled plugins, a convention-based loading mechanism can be implemented (e.g., scanning a specific directory for plugin modules during bootstrap). Nx monorepo structure can aid in organizing these plugins within `libs/`.
- **Bootstrap Phase:** Plugins are typically loaded and integrated during the application's bootstrap sequence.

### 4. Entity Discovery (MikroORM)

A crucial aspect for plugins that introduce their own data models is integration with MikroORM:

- Plugins define their MikroORM entities within their own module structure.
- The main application's MikroORM configuration (`mikro-orm.config.ts`) is set up with glob patterns that are broad enough to discover entities from these registered/loaded plugin directories (e.g., `dist/libs/plugins/**/entities/*.entity.js` or similar, depending on the build output and plugin structure).
- This allows plugin entities to be managed by the same MikroORM instance as core entities.

### 5. Database Migrations (ADR-008)

Plugins that define their own entities often require database schema migrations:

- **Plugin-Provided Migrations:** Each plugin is responsible for creating and maintaining its own MikroORM migration files within its own directory structure.
- **Centralized Execution:** The main application's migration command (e.g., `npx mikro-orm migration:up`) is configured to discover and execute migrations from all registered plugins in addition to its own core migrations.
  - This typically involves configuring the `path` and `glob` options in the MikroORM migrations configuration to include plugin migration directories.
- **Ordering:** Care must be taken if inter-plugin dependencies exist that affect migration order, though independent plugins are preferred.

Refer to ADR-008 for the detailed strategy on plugin migrations.

### 6. Configuration

- Plugins may require their own configuration.
- This can be managed using NestJS's `ConfigModule`, where plugins define their own configuration schema and load values from environment variables or dedicated configuration files, namespaced appropriately to avoid conflicts.

### 7. Interaction with Core Services

Plugins can interact with core EAF services and concepts:

- **Dependency Injection:** Plugins can inject and use services from `libs/core`, `libs/infrastructure`, `libs/tenancy`, `libs/rbac`, etc.
- **Tenancy:** Plugin entities and services should be tenant-aware if they handle tenant-specific data, integrating with the existing `tenant_id` context and RLS mechanisms.
- **RBAC:** Plugins can define their own permissions and integrate with `casl` for authorization if needed.

## Benefits

- **Clean Separation of Concerns:** Isolates custom or extended functionality.
- **Simplified Upgrades:** Core framework updates are less likely to break plugin functionality if interfaces are stable.
- **Reduced Core Bloat:** Keeps the core framework lean, with optional features implemented as plugins.
- **Ecosystem Potential:** Allows for a community or internal marketplace of reusable plugins.

## Example Use Cases for Plugins

- Integration with a specific third-party payment gateway.
- Adding a custom reporting module with its own entities and APIs.
- Implementing a specialized authentication strategy.
- Providing support for a different type of database for certain data.

## Considerations

- **Inter-Plugin Dependencies:** Managing dependencies between plugins can become complex. A clear architectural overview is needed if such dependencies are common.
- **Versioning:** Versioning of plugins and ensuring compatibility with core EAF versions is important.
- **Security:** Plugins execute within the same process as the main application. They should be sourced from trusted locations or undergo security reviews, as a malicious plugin could compromise the entire application.
- **Performance:** A large number of poorly designed plugins could potentially impact application startup time or runtime performance.

This document provides the conceptual framework for the plugin system in ACCI EAF. Specific implementation details may vary based on the chosen loading strategy and plugin complexity.
