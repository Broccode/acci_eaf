---
sidebar_position: 1
title: Getting Started with ACCI EAF
---

# Getting Started with ACCI EAF

Welcome to the ACCI Enterprise Application Framework (EAF)! This comprehensive guide will help you
rapidly set up your development environment and build your first EAF application following our
architectural principles.

## 🎯 Quick Start Goals

By the end of this guide, you'll have:

- A complete local development environment
- A working EAF backend service with hexagonal architecture
- A React frontend component consuming your API
- Understanding of our core architectural principles: **Hexagonal Architecture**, **Domain-Driven
  Design**, and **Test-Driven Development**

**⏱️ Total Time: ~30 minutes**

## 📋 What You'll Build

We'll create a simple **User Profile Management** system that demonstrates:

### Backend (Kotlin/Spring Boot)

- **Domain Layer**: `UserProfile` aggregate with creation command and event
- **Application Layer**: Use case implementation with proper validation
- **Infrastructure Layer**: REST API and NATS event publishing
- **Testing**: TDD approach with unit and integration tests

### Frontend (React/TypeScript)

- **Component**: User profile registration form
- **API Integration**: Consuming REST endpoints
- **Error Handling**: User-friendly error states
- **State Management**: Basic async state handling

## 🗺️ Learning Path

Follow these guides in order for the best experience:

### 1. [Prerequisites](./prerequisites.md)

Set up your development tools and verify your environment is ready.

### 2. [Local Setup](./local-setup.md)

Clone the repository, start infrastructure services, and verify your setup.

### 3. [Your First Service](./first-service.md)

Use the ACCI EAF CLI to generate a properly structured service following hexagonal architecture.

### 4. [Hello World Example](./hello-world-example.md)

Implement a complete user profile feature using TDD and DDD principles.

### 5. [Frontend Integration](./frontend-integration.md)

Create a React component that interacts with your backend service.

### 6. [Development Workflow](./development-workflow.md)

Learn the day-to-day development practices and tooling.

### 7. [Troubleshooting](./troubleshooting.md)

Common issues and solutions for smooth development.

## 🏗️ Architectural Foundation

ACCI EAF is built on three core principles that guide every aspect of development:

### Hexagonal Architecture (Ports & Adapters)

- **Domain** at the center, isolated from external concerns
- **Application** layer orchestrates business logic
- **Infrastructure** adapters handle external integrations
- **Dependency inversion** ensures testability and flexibility

### Domain-Driven Design (DDD)

- **Bounded contexts** define clear service boundaries
- **Aggregates** maintain consistency and encapsulate business rules
- **Events** communicate changes across the system
- **Ubiquitous language** ensures clear communication

### Test-Driven Development (TDD)

- **Red**: Write failing tests first
- **Green**: Implement minimal code to pass
- **Refactor**: Improve code while maintaining tests
- **Confidence**: Comprehensive test coverage enables fearless refactoring

## 🚀 Key Technologies

### Backend Stack

- **Kotlin** - Concise, expressive language with excellent Spring integration
- **Spring Boot** - Production-ready framework with auto-configuration
- **NATS** - High-performance messaging for event-driven architecture
- **PostgreSQL** - Robust relational database for persistence
- **Nx** - Monorepo tooling for build optimization

### Frontend Stack

- **React** - Component-based UI library
- **TypeScript** - Type-safe JavaScript for better developer experience
- **Vite** - Fast build tool and development server
- **Testing Library** - Simple and complete testing utilities

### Development Tools

- **ACCI EAF CLI** - Code generation following architectural patterns
- **Docker Compose** - Local infrastructure orchestration
- **Gradle** - Build automation and dependency management
- **ktlint** - Code formatting and style enforcement

## 💡 Success Tips

### Start Simple

- Follow the guide step-by-step
- Don't skip the testing sections
- Understand the "why" behind each architectural decision

### Think in Layers

- **Domain**: What is the business problem?
- **Application**: How do we solve it?
- **Infrastructure**: What external tools do we need?

### Embrace TDD

- Write tests first to clarify requirements
- Use tests as living documentation
- Refactor fearlessly with test coverage

### Ask Questions

- Join our developer community channels
- Reference the architectural principles documentation
- Use the troubleshooting guide for common issues

## 🎯 Ready to Start?

Begin with the [Prerequisites](./prerequisites.md) guide to set up your development environment.

Have questions? Check our [Troubleshooting](./troubleshooting.md) section or reach out to the EAF
team.

---

**Happy coding!** 🎉

_The ACCI EAF team is here to support your journey toward building robust, scalable enterprise
applications._
