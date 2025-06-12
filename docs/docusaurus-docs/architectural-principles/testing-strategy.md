---
sidebar_position: 5
---

# Testing Strategy (Draft)

> _Status: Draft — will be expanded iteratively as the framework evolves._

ACCI EAF mandates **Test-Driven Development (TDD)** at every layer of the stack.  This page aggregates the scattered testing information and will grow into the canonical reference.

## Testing Pyramid

1. **Unit Tests**  – Pure Kotlin/JS tests, run fast, no external resources.
2. **Integration Tests** – Use Testcontainers (PostgreSQL, NATS/JetStream).
3. **End-to-End (E2E)** – Playwright (frontend) + Spring Boot Test (backend APIs).
4. **Performance / Benchmark** – k6/JMeter profiles executed in CI.

## Quick Commands

```bash
# Run all unit tests
nx run-many -t test --all

# Run enhanced integration tests (requires docker-compose infra)
nx test eaf-eventing-sdk --args="-Dnats.integration.enhanced=true"
```

## Coverage Targets

| Layer            | Target Coverage |
| ---------------- | --------------- |
| Core Libraries   | **90 %+**       |
| Service Modules  | **80 %+**       |
| Projectors       | **70 %+** (logic heavy) |

---
Need something that isn't here?  Open an issue or reach out in the `#eaf-testing` channel.
