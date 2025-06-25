# Migration Plan: Reverting Hilla Workaround for Issue #3443

This document outlines the steps to revert the workarounds and tracking mechanisms implemented to
address the `KotlinNullabilityPlugin` crash documented in
[Hilla GitHub issue #3443](https://github.com/vaadin/hilla/issues/3443).

## 1. Pre-requisites

- [ ] **Confirm Fix:** Verify that the Hilla team has officially released a stable version that
      includes a fix for issue #3443. Check the release notes of the new Hilla version.
- [ ] **Team Agreement:** Agree on a sprint or maintenance window to perform the Hilla version
      upgrade and cleanup.

## 2. Upgrade Process

1. **Update Hilla Version:** In the project's `build.gradle.kts` or `libs.versions.toml`, update the
   Hilla dependency to the new, stable version that contains the fix.
2. **Clean and Rebuild:** Perform a clean build of the entire project to ensure all dependencies are
   correctly resolved and new code is generated.

   ```bash
   nx run-many -t clean
   nx run-many -t build --skip-nx-cache
   ```

## 3. Reversal and Cleanup

This process involves removing the `@HillaWorkaround` annotations that were added for tracking
purposes.

1. **Find All Workarounds:** Perform a global search in the codebase for the string
   `@HillaWorkaround`.
2. **Remove Annotations:** For each file identified, remove the `@HillaWorkaround(...)` annotation
   block from the class definition.

   **Example:**

   _Before:_

   ```kotlin
   @Endpoint
   @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
   @HillaWorkaround(
       description = "Endpoint was preemptively disabled due to Hilla issue #3443, but analysis shows no DTOs were affected. Re-enabling and marking for audit."
   )
   class TenantManagementEndpoint(...)
   ```

   _After:_

   ```kotlin
   @Endpoint
   @RolesAllowed("SUPER_ADMIN", "PLATFORM_ADMIN")
   class TenantManagementEndpoint(...)
   ```

3. **Delete Annotation File:** Delete the annotation definition file itself:
   `libs/eaf-core/src/main/kotlin/com/axians/eaf/core/annotations/HillaWorkaround.kt`
4. **Clean up Documentation:**
   - Remove the "Known Issues & Workarounds" section for the Hilla bug from
     `docs/operational-guidelines.md`.
   - Mark the ADR `docs/docusaurus-docs/adr/0001-hilla-kotlin-nullability-workaround.md` as
     "Superseded" or "Deprecated".

## 4. Validation Checklist

- [ ] **All Tests Pass:** Run the complete test suite for the entire project to ensure that the
      Hilla upgrade and annotation removal have not introduced any regressions.

  ```bash
  nx run-many -t test
  ```

- [ ] **Hilla Endpoints Functional:** Manually test the re-enabled endpoints
      (`RoleManagementEndpoint`, `TenantManagementEndpoint`, `UserManagementEndpoint`) to confirm
      they are still functioning as expected.
- [ ] **OpenAPI Generation:** Verify that the Hilla OpenAPI specification (`/v3/api-docs`) generates
      correctly without any errors.
- [ ] **Code Review:** Submit a Pull Request with all the changes and get it approved by at least
      one other team member.

## 5. Monitoring

- **Monitor Hilla Issue:** Use GitHub notifications to monitor
  [issue #3443](https://github.com/vaadin/hilla/issues/3443) for updates and official fix
  announcements from the Vaadin team. This is the trigger for initiating this plan.
