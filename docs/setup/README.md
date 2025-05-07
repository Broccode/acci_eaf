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

TODO: Describe required tooling (Node >= 20, **npm**, Docker ≥ 24, etc.).

## Local Development

TODO: Step-by-step instructions for cloning, installing dependencies and starting applications via Nx.

## Docker Compose Environment

TODO: Explain `docker-compose.yml`, how to spin up PostgreSQL/Redis and run the framework containers.

## Offline Tarball Installation

TODO: Show how to build and install the offline package (see FR-DEPLOY) including `docker save` output.

## Bootstrap Script

TODO: Document execution of the Control Plane bootstrap script (ADR-007).

## Troubleshooting

TODO: Collect common pitfalls and solutions.

## SBOM Generation

You can locally generate a Software Bill of Materials (SBOM) in CycloneDX JSON format:

```bash
npm run generate:sbom
# Output: dist/sbom.json
```

The command runs the Nx target `sbom:generate-sbom` which executes `cyclonedx-npm`. Ensure dependencies are installed (`npm ci --legacy-peer-deps`) beforehand.
