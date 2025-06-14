---
sidebar_position: 4
title: Developer Tools
---

# Developer Tools

The ACCI EAF platform provides a comprehensive set of developer tools to streamline the development
process and ensure consistency across projects.

## ACCI EAF CLI

The **ACCI EAF CLI** is a command-line tool designed to help developers quickly scaffold new
services and components with proper architecture patterns, testing setup, and EAF SDK integration.

### Available Commands

- **[Service Generation](/developer-tools/acci-eaf-cli)** - Generate new Kotlin/Spring backend
  services with hexagonal architecture

### Key Features

- 🏗️ **Hexagonal Architecture** - Generates services following hexagonal architecture principles
- 🧪 **TDD Ready** - Includes unit tests, integration tests, and ArchUnit architectural tests
- 📦 **EAF SDK Integration** - Pre-configured with all EAF core libraries and SDKs
- 🎯 **Nx Monorepo** - Full integration with the Nx build system
- ✨ **Code Quality** - Spotless formatting and consistent coding standards
- 🔧 **Spring Boot** - Complete Spring Boot setup with EAF conventions

### Getting Started

The CLI is built into the monorepo and can be executed using Gradle:

```bash
./gradlew :tools:acci-eaf-cli:run --args="--help"
```

### Next Steps

- [Learn how to generate services](/developer-tools/acci-eaf-cli) with the CLI
- Explore the [architectural principles](/architecture) behind generated code
- Review [testing strategies](/architecture/testing-strategy) for generated services
