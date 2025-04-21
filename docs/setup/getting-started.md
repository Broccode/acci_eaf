# ACCI EAF - Getting Started Guide

* **Version:** 1.0 Draft
* **Date:** 2025-04-25
* **Status:** Draft

## Introduction

This guide will help you set up a development environment for working with the ACCI EAF (Axians Competence Center Infrastructure Enterprise Application Framework). It covers the initial setup, running the sample application, and creating a new application based on the EAF.

## Prerequisites

Before you begin, make sure you have the following installed:

* **Node.js** (v18 or later)
* **npm** (v8 or later)
* **Docker** and **Docker Compose** (for running PostgreSQL and Redis)
* **Git**
* A code editor (we recommend **Visual Studio Code** with the following extensions):
  * ESLint
  * Prettier
  * NestJS
  * TypeScript Hero
  * MongoDB for VS Code (if using MongoDB for some adapters)

## Setting Up the Development Environment

### 1. Clone the Repository

```bash
git clone https://github.com/axians/acci-eaf.git
cd acci-eaf
```

### 2. Install Dependencies

```bash
npm install
```

This will install all dependencies in the monorepo using Nx's capabilities.

### 3. Set Up Environment Variables

Copy the example environment file:

```bash
cp .env.example .env
```

Edit the `.env` file to configure:

* Database connection details
* Redis connection details
* JWT secrets
* Optional license validation settings

### 4. Start the Infrastructure Services

```bash
docker-compose up -d
```

This will start PostgreSQL and Redis instances configured for local development.

### 5. Run Database Migrations

```bash
npm run migration:run
```

This will create the necessary database schema for the EAF core and sample application.

## Running the Sample Application

The EAF comes with a sample application that demonstrates key features.

### 1. Start the Control Plane API

```bash
npm run start:control-plane
```

This will start the Control Plane API on <http://localhost:3000> by default.

### 2. Create a Tenant

```bash
curl -X POST http://localhost:3000/api/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{"name": "Sample Tenant", "identifier": "sample"}'
```

Replace `<admin-token>` with a valid JWT token for the Control Plane. For development, you can use the preconfigured admin token from the `.env` file.

### 3. Start the Sample Application

```bash
npm run start:sample-app
```

This will start the sample application on <http://localhost:3001> by default.

### 4. Access the Sample Application

You can now access the sample application at <http://localhost:3001/api>, using the tenant identifier in the `X-Tenant-ID` header or in the JWT token.

## Creating a New Application Based on EAF

### 1. Generate a New Application

```bash
npm run generate:app -- --name=my-new-app
```

This will create a new application in the `apps/my-new-app` directory, with the necessary configuration to use the EAF libraries.

### 2. Configure Your Application

Edit the following files in your new application:

* `apps/my-new-app/.env` - Environment configuration
* `apps/my-new-app/src/app.module.ts` - Module configuration
* `apps/my-new-app/src/main.ts` - Application bootstrap

### 3. Add Your Domain Logic

Create your domain models, commands, queries, and events:

* Domain entities in `libs/core/domain/src/lib/my-new-app/`
* Application services in `libs/core/application/src/lib/my-new-app/`
* Infrastructure adapters in `libs/infrastructure/src/lib/my-new-app/`

### 4. Run Your Application

```bash
npm run start:my-new-app
```

## Development Workflow

### Running Tests

```bash
# Run all tests
npm test

# Run tests for a specific project
npm test -- my-new-app

# Run tests with coverage
npm test -- --coverage
```

### Linting and Formatting

```bash
# Lint all code
npm run lint

# Format all code
npm run format
```

### Building for Production

```bash
# Build all applications
npm run build

# Build a specific application
npm run build -- my-new-app
```

## Common Development Tasks

### Adding a New Library

```bash
npm run generate:lib -- --name=my-lib --directory=core
```

### Adding a New Entity

1. Create the entity class in the appropriate domain directory
2. Configure MikroORM discovery to include your entity
3. Generate and run migrations

### Implementing CQRS for a Feature

1. Define commands, queries, and events in the domain layer
2. Create handlers in the application layer
3. Wire up controllers and infrastructure in the adapter layer

### Adding a New Tenant

Use the Control Plane API to create and manage tenants.

## Troubleshooting

### Common Issues

#### Database Connection Problems

* Check that PostgreSQL is running (`docker ps`)
* Verify database connection settings in `.env`
* Check for migration errors in the logs

#### Authentication Issues

* Ensure JWT secrets are properly configured
* Check token expiration
* Verify that tenant context is properly set

#### MikroORM Entity Discovery Issues

* Check glob patterns in the configuration
* Verify that entity filenames match the expected pattern
* Run with debug logging enabled: `DEBUG=mikro-orm:discovery npm start`

## Next Steps

* Explore the architecture documentation in `docs/ARCH.md`
* Review the detailed functional requirements in `docs/fr-spec/FR-SPEC.md`
* Check the Architecture Decision Records in `docs/adr/` to understand key design decisions

## Support

For internal support, please contact the ACCI team at <acci-support@axians.com>.

For bug reports and feature requests, please create an issue in the GitHub repository.
