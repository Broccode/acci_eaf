# Epic-1 - Story-1

Monorepo and Project Template Setup

**As a** Developer using the EAF
**I want** a standardized monorepo structure with initial project scaffolding
**so that** I can quickly start building applications and libraries following the defined architecture.

## Status

Complete

## Context

This is the foundational story for the ACCI EAF project. It establishes the core development environment using Nx and sets up the initial folder structure for applications and libraries as defined in the PRD and Architecture documents (`.ai/prd.md`, `.ai/arch.md`). This structure facilitates code sharing, modularity, and adherence to the Hexagonal Architecture pattern.

## Estimation

Story Points: 1 (Estimated based on AI development time)

## Tasks

1. - [x] Set up Nx Monorepo
    1. - [x] Initialize Nx in the workspace (`nx init`).
    2. - [x] Ensure `package.json` and `tsconfig.base.json` are correctly configured.
    3. - [x] Install necessary Nx plugins (`@nx/nest`, `@nx/js`).
2. - [x] Provide Project Scaffolding
    1. - [x] Generate `apps/control-plane-api` (NestJS application).
    2. - [x] Generate `apps/sample-app` (NestJS application).
    3. - [x] Generate `libs/core` (JS/TS library).
    4. - [x] Generate `libs/infrastructure` (NestJS library).
    5. - [x] Generate `libs/tenancy` (NestJS library).
    6. - [x] Generate `libs/rbac` (NestJS library).
    7. - [x] Generate `libs/licensing` (NestJS library).
    8. - [x] Generate `libs/plugins` (JS/TS library).
    9. - [x] Generate `libs/testing` (JS/TS library).
    10. - [x] Generate `libs/shared` (JS/TS library).

## Constraints

- Use Nx for monorepo management.
- Follow the directory structure defined in `.ai/arch.md`.
- Use NestJS (`@nx/nest`) for applications and infrastructure-related libraries.
- Use plain JS/TS (`@nx/js`) for core logic and shared utilities.

## Data Models / Schema

N/A for this story.

## Structure

```text
apps/
  control-plane-api/
  control-plane-api-e2e/
  sample-app/
  sample-app-e2e/
libs/
  core/
  infrastructure/
  licensing/
  plugins/
  rbac/
  shared/
  tenancy/
  testing/
```

## Diagrams

N/A for this story.

## Dev Notes

- Initial `nx init` failed due to missing `package.json`. Resolved by running `npm init -y` first.
- Subsequent generation attempts failed due to missing `@nx/nest` plugin and `tsconfig.base.json`. Resolved by installing the plugin and creating the base config file.
- Generator command syntax required explicit `--name` flag.

## Chat Command Log

- User: lass uns nun mit der ersten story des ersten epic beginnen
- Agent: Initializing Nx... (`npx nx@latest init`)
- Agent: (Error - no node_modules) Installing dependencies... (`npm install`)
- Agent: (Error - no package.json) Creating package.json... (`npm init -y`)
- Agent: Re-initializing Nx... (`npx nx@latest init`)
- Agent: Generating apps... (`npx nx g @nx/nest:app ...`)
- Agent: (Error - unable to resolve @nx/nest, no tsconfig.base.json) Installing @nx/nest... (`npm install -D @nx/nest`)
- Agent: Creating tsconfig.base.json...
- Agent: Generating apps again... (`npx nx g @nx/nest:app ...`)
- Agent: (Error - positional argument) Correcting generation command... (`npx nx g @nx/nest:app --name ...`)
- Agent: Apps generated successfully.
- Agent: Installing @nx/js... (`npm install -D @nx/js`)
- Agent: Generating JS/TS libs... (`npx nx g @nx/js:lib ...`)
- Agent: Generating NestJS libs... (`npx nx g @nx/nest:lib ...`)
- Agent: Libraries generated successfully. Story complete.
- User: das ist wirklich toll, aber du hast keine story-datei angelegt, oder?
- Agent: (Acknowledges mistake) Reading story template... (`read_file .cursor/templates/template-story.md`)
- Agent: Creating story file `.ai/story-1.story.md`... (`edit_file .ai/story-1.story.md`)

## Examples

N/A for this story.
