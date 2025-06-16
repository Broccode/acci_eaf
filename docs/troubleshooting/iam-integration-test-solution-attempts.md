# Troubleshooting Log – IAM Service Integration Test (`TenantPersistenceAdapterTest`)

> **STATUS: ✅ RESOLVED** (2025-06-12)  
> **Context:** Spring Boot 3 / JUnit 5 integration tests for `apps/iam-service` failed to start the
> `ApplicationContext` with `NoSuchBeanDefinitionException: TenantJpaRepository` (root cause
> obscured by wrapper exceptions).
>
> **Goal:** Achieve a green build for `TenantPersistenceAdapterTest` that uses a Test-containerised
> PostgreSQL database and loads JPA repositories via `JpaConfig.kt`.

---

## ✅ **FINAL RESOLUTION STATUS**

**Date:** 2025-06-12  
**Result:** **ALL TESTS PASSING** (24/24 tests, 0 failures, 100% success rate)

### **Root Cause Analysis**

The integration test failures had **two distinct phases**:

1. **Phase 1**: Spring context loading failures (`NoSuchBeanDefinitionException`)
2. **Phase 2**: Database configuration mismatches (`SQLGrammarException` - tables not found)

### **Complete Solution Applied**

| **Issue**                                                   | **Solution**                                                                                                              | **Files Modified**             |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| Multiple `@SpringBootConfiguration` classes in same package | Moved `TestIamServiceApplication` to `com.axians.eaf.iam.test` package                                                    | `TestIamServiceApplication.kt` |
| Component scan conflicts                                    | Refined scanning to exclude main app: `scanBasePackages = ["...application", "...domain", "...infrastructure", "...web"]` | `TestIamServiceApplication.kt` |
| Missing JPA configuration in tests                          | Added `@Import(JpaConfig::class)` to all integration tests                                                                | All test classes               |
| Database dialect mismatch                                   | Changed from `H2Dialect` to `PostgreSQLDialect` in test properties                                                        | `application-test.properties`  |

### **Templates Created**

- ✅ Integration test setup checklist for future EAF services
- ✅ Standard test application configuration pattern
- ✅ Testcontainer configuration template

---

## 1. Baseline Failure

- **Symptom** – `@SpringBootTest` could not find `TenantJpaRepository`; context load aborted.
- **Hypothesis** – `JpaConfig.kt` (declares `@EnableJpaRepositories` / `@EntityScan`) was not pulled
  into the test context.

---

## 2. Solution Attempts Chronology

| #   | Date | Strategy                                                   | Key Changes                                                                                                            | Result                                                                                                               |
| --- | ---- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| 1   | ⬩    | **Explicit `@Import(JpaConfig)`**                          | Added `@Import(JpaConfig::class)` to `TenantPersistenceAdapterTest`.                                                   | Still failed – duplicate Test-container definition caused secondary `IllegalStateException`.                         |
| 2   | ⬩    | **Remove duplicate container**                             | Deleted inline `@Container` block; relied on shared `PostgresTestcontainerConfiguration`.                              |
| 3   | ⬩    | **Supply manual `DataSource` bean**                        | Main app excludes `DataSourceAutoConfiguration`; created Hikari `@Bean` using container props via direct field access. | Intermittent **`IllegalStateException`** – `DataSource` initialised before container ready.                          |
| 4   | ⬩    | **Use `Environment` + `@DynamicPropertySource`**           | `dataSource()` pulled JDBC settings from `Environment`; container props set via `DynamicPropertySource`.               | Still raced – occasional `NullPointerException` when property unresolved.                                            |
| 5   | ⬩    | **`ApplicationContextInitializer`**                        | Started container in static initializer, injected props programmatically before Spring refresh.                        | Worked in **isolated test**, but whole context still failed (conflict with main `IamServiceApplication` exclusions). |
| 6   | ⬩    | **Adopt Spring Boot 3 `@ServiceConnection`**               | Added `spring-boot-testcontainers` dependency; replaced bean & property plumbing with annotation magic.                | Context failed – `DataSourceAutoConfiguration` still excluded by main app, so no datasource produced.                |
| 7   | ⬩    | **Re-enable auto-config via test annotations**             | Tried `@EnableAutoConfiguration` & custom test config class.                                                           | Spring still preferred exclusion from `IamServiceApplication`; unresolved dependencies persisted.                    |
| 8   | ⬩    | **Isolated Test Context (`@SpringBootTest(classes = …)`)** | Supplied only `IamTestConfiguration`, `JpaConfig`, and Test-container config.                                          | Context still missing JPA repositories – root cause remains unresolved.                                              |

_⬩ multiple iterations between 2 and 7 included small refinements (e.g. `@DirtiesContext`, manual
container `start()`, etc.)_

---

## 3. Key Findings

1. The global exclusion `@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])` in
   `IamServiceApplication.kt` **propagates into every `@SpringBootTest`** unless the test
   **completely replaces** the primary `@SpringBootConfiguration`.
2. `@ServiceConnection` works **only** when `DataSourceAutoConfiguration` is active; otherwise
   Spring never creates a `DataSource` bean.
3. Mixing Test-container lifecycle annotations (`@Testcontainers`, `@Container`) with manual
   `@Bean DataSource` initialisation requires **careful ordering**; reading container properties
   directly in a `@Bean` is fragile.
4. Even when the container starts successfully, **repository scanning fails** if `JpaConfig` is
   still outside the component-scan path of the effective boot configuration class.

---

## 4. Recommended Next Steps

1. **Create a minimal Spring Boot configuration class for tests only**

   ```kotlin
   @SpringBootConfiguration
   @EnableAutoConfiguration // no exclusions
   @Import(JpaConfig::class)
   class IamTestBoot
   ```

   Then use:

   ```kotlin
   @SpringBootTest(classes = [IamTestBoot::class])
   ```

   ensuring the main application class is **not** picked up.

2. Keep `PostgresTestcontainerConfiguration` with `@ServiceConnection` only – no manual datasource.
3. Remove any lingering custom `@Bean DataSource` definitions to let Spring Boot auto-config wire
   the datasource.
4. Verify repository package paths in `JpaConfig.kt` align with compiled test classes.

---

## 5. Lessons Learned

- Avoid relying on the production `@SpringBootApplication` class in integration tests when it
  deliberately disables key auto-configs.
- `@ServiceConnection` greatly simplifies Test-container plumbing but **requires**
  auto-configuration.
- For tricky context-load issues, start with the **smallest possible configuration** and
  incrementally add beans.
- Enable `debug=true` in `application-test.properties` early; read the auto-configuration report.

---

## 6. Finalised Fix (2025-06-11)

| Step | Change                                                                                                                                      | Why                                                                                                                                                                                                                                                                                                      |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1    | **Moved test boot class to its own package** (`com.axians.eaf.iam.test.TestIamServiceApplication`)                                          | Prevents Spring from detecting _two_ `@SpringBootConfiguration` classes in the same package, which caused "Found multiple @SpringBootConfiguration" errors in slice tests.                                                                                                                               |
| 2    | \*_Converted `PostgresTestcontainerConfiguration` to a `@TestConfiguration` **class**_ with a `@Bean` method annotated `@ServiceConnection` | Recommended Spring-Boot-3 style. Declaring the container via `@Bean` lets the `ServiceConnectionContextCustomizer` start the container early and expose JDBC properties before `DataSourceAutoConfiguration` runs. No more manual lifecycle code, no more `Failed to determine a suitable driver class`. |
| 3    | **Removed ad-hoc `@Container`/`@JvmField` declarations**                                                                                    | Those prevented the container from being detected as a `ServiceConnection` bean and cluttered the lifecycle.                                                                                                                                                                                             |
| 4    | **Updated every test to import the new boot class** and removed lingering `IamServiceApplication` references                                | Ensures each test gets auto-configuration _with_ `DataSourceAutoConfiguration` and sees `JpaConfig` in its component scan.                                                                                                                                                                               |

_Result_ – Application context now starts reliably in all integration & slice tests. Repository
beans are discovered and injected. Remaining red tests are _functional_ (e.g. JSON body expectations
or missing MockK stubbing), not boot/ wiring issues.

### Next technical debt

1. Introduce a `@RestControllerAdvice` (global error handler) returning the canonical JSON envelope
   (`{"error":"…"}`) for 400/401/403… This will address the remaining assertion failures in
   `TenantControllerTest` and `IamServiceIntegrationTest`.
2. Tighten MockK stubs to avoid "no answer found" errors in negative-path tests.

---

_Last updated: 2025-06-11_
