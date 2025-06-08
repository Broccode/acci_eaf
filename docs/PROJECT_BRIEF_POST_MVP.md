# Project Brief: ACCI EAF - Phase 2: Operational Excellence

## Introduction / Problem Statement

Following the successful establishment of the ACCI EAF MVP, the next challenge is to enhance the framework with critical operational capabilities. This not only ensures the long-term stability and maintainability of the products built upon it, but also creates the necessary trust with enterprise customers through demonstrable performance and deep system insights.

## Vision & Goals

* **Vision:** To elevate the ACCI EAF from a functional MVP to a trusted, enterprise-grade platform by embedding deep operational insights and proactive quality assurance into its core, making it the benchmark for reliability and maintainability at Axians.

* **Primary Goals:**
    1. Implement a comprehensive observability solution (Metrics, Logs, Traces) for all EAF-based applications.
    2. Establish an automated performance benchmarking framework within the CI/CD pipeline to prevent performance regressions.
    3. Define and implement the core components of the deferred **Notification Service** and **Scheduled Tasks/Job Runner**.
    4. Provide a foundational **Business Logic/Workflow Engine** to enable the orchestration of complex, multi-step business processes within EAF applications.

* **Success Metrics:**
    1. **For the Observability Goal:** The Mean Time to Resolution (MTTR) for a simulated failure in a test environment is reduced by 50% compared to the state without the new tools. All critical services report their metrics (CPU, latency, etc.) to the central dashboard.
    2. **For the Benchmarking Goal:** The CI/CD pipeline automatically fails the build if a new commit degrades the performance of a core process (login, list loading) by more than 10% (against the versioned benchmark). A traceable performance benchmark report is available for every release candidate.
    3. **For the new Services (Notification & Job Runner):** The Notification Service can successfully deliver 1000 notifications per minute with a maximum latency of X milliseconds. The Job Runner executes scheduled tasks with 99.9% reliability.
    4. **For the Workflow Engine:** A defined test workflow with at least 5 steps (including a conditional branch) can be successfully and traceably executed automatically 100 times without errors.

## Target Audience / Users

For the post-MVP features, there are two clearly defined target audiences:

1. **Primary Target Audience (Internal): Axians Development & Operations Teams**
    * **Who:** The developers (like the personas Michael, Majlinda), architects, and operators at Axians.
    * **Needs & Benefits:**
        * They benefit from **all** features, especially **Observability** and **Benchmarking**, to develop and operate high-quality, stable, and performant software.
        * They use the new services (**Notification, Jobs, Workflow**) to easily and uniformly extend the feature set of their products for end customers.

2. **Secondary Target Audience (External): End Customers of EAF Products**
    * **Who:** The companies and their users who license and use a product built on the ACCI EAF (like DPCM, ZEWSSP, etc.).
    * **Needs & Benefits:**
        * They benefit **directly** from new services like the Notification Service (e.g., receiving system notifications), the Job Runner (their background processes run reliably), and the Workflow Engine (their complex business procedures are mapped).
        * They benefit **indirectly** from Observability and Benchmarking through higher product stability, better performance, and faster troubleshooting.
        * They could potentially gain access to certain monitoring dashboards themselves to monitor the "health" and functionality of their purchased product.

## Key Features / Scope (Post-MVP Enhancements)

1. **Comprehensive Observability Platform:** Implementation of an observability solution following the "Best-of-Breed" approach, consisting of:
    * a. **Metrics:** Central collection and visualization of technical and business metrics.
    * b. **Logging:** Centralized, searchable aggregation of all application logs.
    * c. **Tracing:** System-wide request tracing using a Correlation ID.

2. **Automated Performance Benchmarking:** Introduction of an automated benchmark framework into the CI/CD pipeline with configurable thresholds to prevent performance regressions.

3. **Core Service Enhancements:** Implementation of the previously deferred core services from the original project scope:
    * a. Notification Service
    * b. Scheduled Tasks / Job Runner
    * c. Business Logic / Workflow Engine

## Post MVP Features / Scope and Ideas

(Ideas for a potential "Phase 3" after this enhancement package is complete)

* **Single Sign-On (SSO)** across all EAF-based applications.
* A **graphical editor** for creating and managing workflows in the Workflow Engine.

## Known Technical Constraints or Preferences

* **Constraints:**
  * `ppc64le` compatibility must be ensured for all new components.
  * Solutions must be operable in "less cloud-native" environments (Docker, Ansible).
  * A commitment to the cost-effective "Best-of-Breed" approach for observability tools, which excludes commercial all-in-one suites.

* **Initial Architectural Preferences:**
  * The new core services (Notification, Job Runner, Workflow Engine) are to be developed as independent microservices.
  * For their use by other applications, corresponding, EAF-compliant SDKs must be provided, following the established pattern of the framework.

* **Risks:**
    1. The chosen "Best-of-Breed" approach for observability may require a higher initial integration and configuration effort than a commercial all-in-one solution.
    2. The new services, especially the Workflow Engine, have significant potential, creating a risk of "scope creep" that could delay completion.
    3. The introduction of detailed tracing and metric collection could create a slight performance overhead for applications that must be carefully monitored.
