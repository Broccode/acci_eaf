## Frontend Testing Strategy

This complements the "Overall Testing Strategy" in the main Architecture Document and aligns with
TDD. Training module UI-H4 covers this.

- **Component Testing (Unit Tests for Components):**
  - **Scope:** Testing individual React components (both from UI Foundation Kit and
    application-specific) in isolation.
  - **Tools:** **Vitest** or **Jest** with **React Testing Library (RTL)**.
  - **Focus:** Rendering with various props, user interactions (simulated via RTL's `fireEvent` or
    `@testing-library/user-event`), event emission, basic internal state changes. Snapshot testing
    will be used sparingly and with clear justification. Mock Hilla service calls if a component
    fetches data directly.
  - **Location:** `*.test.tsx` or `*.spec.tsx` co-located alongside components.
- **Feature/Flow Testing (UI Integration Tests):**
  - **Scope:** Testing interactions between multiple components to fulfill a small user flow or
    feature within a page (e.g., a complete form submission with validation).
  - **Tools:** Same as component testing (Vitest/Jest + RTL).
  - **Focus:** Data flow between components, conditional rendering, navigation stubs, integration
    with mocked Hilla services.
- **End-to-End (E2E) UI Testing Tools & Scope:**
  - **Tools:** **Playwright** is recommended by Hilla/Vaadin documentation and aligns with Vaadin
    Copilot's test generation capabilities.
  - **Scope (Control Plane MVP):** 1. SuperAdmin Login -> View Tenant List -> Initiate Tenant
    Creation -> Fill Form -> Review -> Submit -> Verify Optimistic Update & Success Toast -> Verify
    New Tenant in List. 2. TenantAdmin Login -> View User List (own tenant) -> Initiate User
    Creation -> (similar flow). 3. Attempt unauthorized access to a SuperAdmin page as TenantAdmin
    and verify redirection/error.
  - **Test Data:** Use dedicated test accounts and mock Hilla endpoint responses at the network
    level if needed (e.g., using Playwright's network interception or tools like MSW if more complex
    mocking is required).
