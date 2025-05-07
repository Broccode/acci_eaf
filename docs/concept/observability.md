# Observability in ACCI EAF

Version: 1.0
Date: 2025-05-07
Status: Published

## Introduction

Observability is a critical aspect of modern software systems, enabling developers and operators to understand the internal state of an application, diagnose issues, and monitor performance. ACCI EAF integrates foundational observability features and provides hooks for more advanced capabilities.

This document outlines the core observability concepts and implementations within the framework for V1, with a view towards future enhancements.

## Core Observability Pillars (V1)

### 1. Structured Logging

Comprehensive and consistent logging is the cornerstone of observability.

- **Mechanism:** ACCI EAF promotes the use of structured logging (e.g., JSON format).
  - **Recommended Logger:** While not strictly enforced to a single library in `libs/core`, for NestJS applications generated with or using the framework (like `apps/control-plane-api` or `apps/sample-app`), `pino` (e.g., via `nestjs-pino`) is a highly recommended and performant choice.
- **Key Log Attributes:** Logs should include essential contextual information:
  - `timestamp`: Time of the log event.
  - `level`: Log severity (e.g., `INFO`, `WARN`, `ERROR`, `DEBUG`).
  - `message`: The log message.
  - `context` / `loggerName`: Name of the module or class emitting the log.
  - `tenant_id`: (Crucial for multi-tenant applications) The ID of the tenant associated with the request/operation.
  - `correlationId`: A unique ID to trace a single request or operation across multiple services or log entries.
  - `errorDetails`: Stack traces, error codes, etc., for error logs.
- **Integration:** Hooks and interfaces are provided in `libs/core` or `libs/infrastructure` to facilitate consistent logger instantiation and usage across different parts of the framework and consuming applications.

### 2. Health Checks

Health checks provide a simple way for orchestration platforms (like Kubernetes) or monitoring systems to determine if an application instance is alive and ready to serve traffic.

- **Mechanism:** ACCI EAF utilizes the `@nestjs/terminus` module for implementing health check endpoints.
- **Standard Endpoints:**
  - **Liveness Probe (e.g., `/health/live` or `/live`):** Indicates if the application process is running. A failure here might lead an orchestrator to restart the application instance.
  - **Readiness Probe (e.g., `/health/ready` or `/ready`):** Indicates if the application is ready to accept new traffic. This check typically verifies dependencies like database connectivity or cache availability. A failure here might lead an orchestrator to temporarily stop sending traffic to the instance.
- **Common Health Indicators:**
  - Database connectivity (e.g., can connect to PostgreSQL).
  - Cache connectivity (e.g., can connect to Redis).
  - Disk space (less common for typical EAF apps but possible).
  - Custom application-specific checks.
- **Configuration:** Health indicators are configured within the application (e.g., in `AppModule` or a dedicated health module).

## Benefits of V1 Observability

- **Improved Debugging:** Structured logs with context make it easier to pinpoint issues.
- **Basic Monitoring:** Health checks allow for basic operational monitoring and automated recovery by orchestrators.
- **Foundation for Advanced Features:** Sets the stage for more sophisticated observability tools.

## Future Considerations (Beyond V1)

While V1 focuses on logging and health checks, the following are common next steps in enhancing observability:

- **Metrics Export (e.g., Prometheus):**
  - Exposing key application and business metrics (e.g., request rates, error rates, latencies, queue lengths, business transaction counts) in a format consumable by monitoring systems like Prometheus.
  - Libraries like `prom-client` can be integrated.
- **Distributed Tracing (e.g., OpenTelemetry):**
  - Implementing distributed tracing allows tracking a single request as it flows through multiple microservices or components within a distributed system.
  - This involves instrumenting code to propagate trace context (trace IDs, span IDs) and export trace data to a tracing backend (e.g., Jaeger, Zipkin, Grafana Tempo).
  - OpenTelemetry provides a vendor-neutral set of APIs and SDKs for this purpose.
- **Alerting:** Setting up alerts based on metrics or log patterns to proactively notify teams of issues.
- **Centralized Logging Platform:** Shipping logs to a centralized platform (e.g., ELK Stack - Elasticsearch, Logstash, Kibana; Grafana Loki; Splunk) for easier searching, analysis, and visualization.

## Conclusion

ACCI EAF V1 provides essential observability features through structured logging and health checks. This foundation is crucial for operating and maintaining applications built with the framework. As projects mature and scale, further investment in metrics, distributed tracing, and centralized logging platforms is encouraged to achieve deeper insights and proactive issue management. Developers using EAF should adopt consistent logging practices and leverage the health check mechanisms from the outset.
