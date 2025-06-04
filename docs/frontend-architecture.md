**{Project Name} Frontend Architecture Document** (Project Name: ACCI EAF)

## Table of Contents

- Introduction
- Overall Frontend Philosophy & Patterns
- Detailed Frontend Directory Structure
- Component Breakdown & Implementation Details
  - Component Naming & Organization
  - Template for Component Specification
- State Management In-Depth
  - Decision Guide for State Location
  - Store Structure / Slices (If Applicable for Global UI State)
  - Key Selectors (If Applicable)
  - Key Actions / Reducers / Thunks (If Applicable)
- API Interaction Layer
  - Client/Service Structure (Hilla Endpoints)
  - Error Handling & Retries (Frontend for Hilla Calls)
- Routing Strategy
  - Routing Library (Hilla File-Based Routing)
  - Route Definitions (ACCI EAF Control Plane MVP)
  - Route Guards / Protection
- Build, Bundling, and Deployment
  - Build Process & Scripts (Hilla/Vite via Gradle/Nx)
  - Environment Configuration Management
  - Key Bundling Optimizations
  - Deployment to CDN/Hosting (as part of Spring Boot JAR)
- Frontend Testing Strategy
  - Component Testing
  - Feature/Flow Testing (UI Integration)
  - End-to-End UI Testing Tools & Scope
- Accessibility (AX) Implementation Details
- Performance Considerations
- Internationalization (i18n) and Localization (l10n) Strategy
- Feature Flag Management (Frontend SDK Usage)
- Frontend Security Considerations
- Browser Support and Progressive Enhancement
- Change Log

## Introduction

This document details the technical architecture specifically for the frontend of applications built
using the Axians Competence Center Infrastructure - Enterprise Application Framework (ACCI EAF),
with an initial focus on the **ACCI EAF Control Plane MVP** and the principles guiding the **ACCI
EAF UI Foundation Kit**. It complements the main ACCI EAF Architecture Document and the UI/UX
Specification for the Control Plane. The chosen frontend technology stack is **Vaadin with Hilla,
utilizing React and TypeScript**. This document builds upon the foundational decisions (e.g.,
overall tech stack, CI/CD, primary testing tools) defined in the main ACCI EAF Architecture
Document. The goal is to provide a clear blueprint for frontend development, ensuring consistency,
maintainability, performance, accessibility, and alignment with the overall system design and user
experience goals.

- **Link to Main Architecture Document (REQUIRED):** `docs/architecture.md` (as produced by Fred)
- **Link to UI/UX Specification (REQUIRED if exists):** `docs/front-end-spec.md` (being populated
  based on our discussions with Jane)
- **Link to Primary Design Files (Figma, Sketch, etc.) (REQUIRED if exists):** TBD (To be developed
  based on UI/UX Specification)
- **Link to Deployed Storybook / Component Showcase (if applicable):** TBD (This will point to the
  ACCI EAF UI Foundation Kit's Storybook once developed)

## Overall Frontend Philosophy & Patterns

The frontend architecture for ACCI EAF applications will adhere to the following philosophy and
patterns:

- **Framework & Core Libraries:** **Vaadin with Hilla, using React and TypeScript**. This provides
  end-to-end type safety from the Kotlin/Spring backend to the frontend and simplifies client-server
  communication.
- **Component Architecture:** The UI Foundation Kit will be based on **standard Vaadin UI
  components** (from `@vaadin/react-components`) as the primary building blocks, styled according to
  Axians' branding. **Custom React components** will be developed for specific needs not met by
  standard Vaadin components, adhering to the same quality and documentation standards. All reusable
  components will be documented in Storybook.
- **State Management Strategy:** Hilla's direct backend service calls significantly reduce the need
  for complex global client-side state management for server data.
  - **Server Cache & Data Synchronization:** Primarily handled by Hilla's mechanisms and patterns
    for calling backend services. React Query or SWR are generally not needed for data fetching from
    Hilla endpoints.
  - **UI State:** For client-side UI state (e.g., modal visibility, form input state before
    submission, theme selection), we will prioritize **React's local component state (`useState`,
    `useReducer`)** and **React Context API** for state that needs to be shared across a component
    subtree without prop drilling. Complex global state managers (like Redux or Zustand) will be
    avoided unless a clear, compelling need arises that cannot be efficiently addressed by Hilla and
    React's built-in capabilities.
- **Data Flow:** Primarily unidirectional, with UI components invoking Hilla services (which are
  type-safe TypeScript clients generated from backend Kotlin services) and updating their state
  based on the responses.
- **Styling Approach:** Vaadin's Lumo theme will be customized to reflect Axians' branding (colors,
  typography, spacing) using CSS custom properties. Custom React components will utilize **CSS
  Modules** for scoped styling to prevent conflicts and ensure maintainability, aligning with Hilla
  project structures. Utility classes may be used sparingly if a utility CSS framework is adopted
  alongside.
- **Key Design Patterns Used:**
  - React Hooks (`useState`, `useEffect`, `useContext`, custom hooks) for managing component logic
    and state.
  - File-based routing provided by Hilla.
  - Type-safe service invocation patterns enabled by Hilla.
  - Composition over inheritance for UI components.

## Detailed Frontend Directory Structure

The frontend code for ACCI EAF applications (like the Control Plane) and the UI Foundation Kit will
reside within the Nx monorepo, following Hilla conventions within their respective Gradle modules/Nx
projects.

**Example for `apps/acci-eaf-control-plane/frontend/` (Hilla frontend part of the Control Plane
Spring Boot module):** Based on `Hilla, Spring Boot, Gradle-Projekt` (sources: 1916-1917, 1920-1921,
1963-1965).

```plaintext
apps/acci-eaf-control-plane/ # Nx project root for the Control Plane app
└── backend/                   # Spring Boot module root (also Gradle project root for this app)
    └── src/main/frontend/     # Root for all Hilla/React frontend code for this app
        ├── views/             # Contains .tsx files for Hilla file-based routes/views
        │   ├── MainLayout.tsx # Example root layout for authenticated views
        │   ├── LoginView.tsx
        │   ├── dashboard/
        │   │   └── DashboardView.tsx
        │   ├── tenants/         # Tenant Management section for SuperAdmins
        │   │   ├── TenantListView.tsx
        │   │   └── (CreateTenantModal.tsx - could be here or in components/tenants)
        │   └── users/           # User Management section for TenantAdmins
        │       └── UserListView.tsx
        ├── components/          # Reusable React components specific to this application
        │   ├── shared/          # General shared components (e.g., specific headers, footers)
        │   └── tenants/         # Components related to tenant management features
        ├── themes/              # Application-specific theming (e.g., acci-eaf-control-plane theme)
        │   └── acci-eaf-control-plane/
        │       ├── styles.css
        │       └── theme.json   # (or theme.ts if using JS for theming)
        ├── generated/           # Auto-generated by Hilla: TypeScript clients for backend Endpoints & DTOs
        │                         # (This directory is typically in .gitignore)
        ├── store/               # Optional: For React Context API providers if needed
        ├── services/            # Optional: Wrappers around Hilla generated services if complex logic is needed before calling
        ├── config/              # Frontend specific configurations (e.g. i18n init)
        ├── public/              # Static assets not processed by Vite (e.g. favicons)
        ├── index.html           # Main HTML entry point (managed by Hilla/Vite)
        ├── index.ts             # Main TypeScript entry point (managed by Hilla/Vite)
        ├── vite.config.ts       # Vite configuration (managed by Hilla, can be customized)
        └── tsconfig.json        # TypeScript configuration for the frontend app
```

**Example for `libs/ui-foundation-kit/` (Nx project, potentially a separate package):**

```plaintext
libs/ui-foundation-kit/
├── src/                     # Source for reusable UI Foundation Kit components
│   ├── components/          # Core reusable components
│   │   ├── Button/
│   │   │   ├── Button.tsx
│   │   │   └── Button.stories.tsx
│   │   ├── TextField/
│   │   │   └── ...
│   │   └── layout/
│   │       └── ...
│   ├── themes/              # Base Axians theme for Vaadin components
│   │   └── axians-base-theme/
│   │       ├── styles.css
│   │       └── theme.json
│   ├── i18n/                # Base i18n utilities or common translations for the kit
│   └── index.ts             # Main export file for the UI Foundation Kit
├── storybook/               # Storybook configuration and global setup
│   ├── main.ts
│   └── preview.ts
├── package.json             # Manages dependencies for the UI Foundation Kit (React, Vaadin components, etc.)
└── tsconfig.json            # TypeScript configuration for the UI Foundation Kit
```

### Notes on Frontend Structure

- The Hilla application (e.g., Control Plane) will have its frontend code co-located within its
  Spring Boot module's `src/main/frontend` directory, as per Hilla conventions.
- The `ui-foundation-kit` will be a separate library in the Nx monorepo, publishable and consumable
  by any EAF frontend application.
- `generated/` directory in Hilla applications is critical as it contains the bridge to the backend
  and should not be manually edited.
- AI Agent (Developer) MUST adhere to this defined structure strictly. New files MUST be placed in
  the appropriate directory.

## Component Breakdown & Implementation Details

Detailed specification for most feature-specific components will emerge as user stories are
implemented. Components within the `ui-foundation-kit` will be extensively documented in Storybook.
All components, whether standard Vaadin components used from the kit or custom-built ones, must
adhere to the UI/UX principles, accessibility standards, and i18n strategies defined.

### Component Naming & Organization

- **Component Naming Convention:** **PascalCase** for React component files and named exports (e.g.,
  `UserProfileCard.tsx`, `export function UserProfileCard(...)`).
- **Organization:** Globally reusable components reside in `libs/ui-foundation-kit/src/components/`.
  Application-specific components reside within the application's frontend structure (e.g.,
  `apps/acci-eaf-control-plane/backend/src/main/frontend/components/`).

### Template for Component Specification

(This template is for when new components are designed and specified for the UI Foundation Kit or
for application-specific needs. It will be used to document them, especially in Storybook or
detailed design docs.)

#### Component: `{ComponentName}` (e.g., `TenantCreationForm`, `AuditLogTable`)

- **Purpose:** {Briefly describe what this component does and its role in the UI. MUST be clear and
  concise.}
- **Source File(s):** {e.g., `libs/ui-foundation-kit/src/components/forms/TenantCreationForm.tsx`.
  MUST be the exact path.}
- **Visual Reference:** {Link to specific Figma frame/component, Storybook page, or detailed
  wireframe. REQUIRED.}
- **Props (Properties):**

  | Prop Name    | Type                                                                                            | Required? | Default Value | Description                                                                                     |
  | :----------- | :---------------------------------------------------------------------------------------------- | :-------- | :------------ | :---------------------------------------------------------------------------------------------- |
  | `{propName}` | `{Type (e.g., string, number, boolean, () => void, MyDto from 'Frontend/generated/endpoints')}` | {Yes/No}  | {If any}      | {MUST clearly state the prop's purpose and any constraints, e.g., 'Must be a valid Tenant ID.'} |

- **Internal State (if any):**

  | State Variable | Type     | Initial Value | Description                                                                                                   |
  | :------------- | :------- | :------------ | :------------------------------------------------------------------------------------------------------------ |
  | `{stateName}`  | `{type}` | `{value}`     | {Description of state variable and its purpose. Only list state not derived from props or global UI context.} |

- **Key UI Elements / Structure (Pseudo-JSX/HTML):**

  ```html
  {/* Describe the primary DOM structure and key conditional rendering logic. Example:
  <div>
    <TextField
      label="{props.nameLabel}"
      value="{internalName}"
      onValueChanged="{handleNameChange}"
    />
    {props.showAdvanced && <AdvancedOptionsComponent />}
    <button onClick="{props.onSubmit}">Submit</button>
  </div>
  */}
  ```

- **Events Handled / Emitted (Callbacks):**
  - **Handles:** {e.g., `onClick` on a submit button (triggers `props.onSubmit`).}
  - **Emits (via props):** {e.g., `onSubmit: (data: FormData) => void`, `onCancel: () => void`.}
- **Actions Triggered (Side Effects):**
  - **Hilla Endpoint Calls:** {Specify which Hilla service method from
    `Frontend/generated/endpoints.js` is called. e.g., "Calls `TenantService.createTenant(formData)`
    on submit. Handles success/error responses."}
  - **UI Context/Store Updates:** {If using React Context for UI state, e.g., "Dispatches
    `uiContext.showNotification('Tenant created')`."}
- **Styling Notes:**
  - {MUST reference specific Vaadin components used (e.g., `<Button theme='primary'>`). Specify
    custom CSS Module class names applied (e.g., `className={styles.customCard}`). Note any dynamic
    styling based on props or state. Refer to Axians theme variables.}
- **Accessibility Notes:**
  - {MUST list specific ARIA attributes and their values if not handled by Vaadin components (e.g.,
    `aria-label`, `role`). Required keyboard navigation behavior. Focus management needs.}

---

## _Repeat the above template for each significant custom component in the UI Foundation Kit or application._

## State Management In-Depth

- **Chosen Solution & Philosophy:** As Hilla provides direct, type-safe methods to call backend
  services and manage server data, the need for complex client-side global state management
  solutions (like Redux or Zustand) for server cache is significantly reduced.
  - Server data will primarily be fetched on demand via Hilla endpoint calls within components or
    custom React Hooks.
  - Client-side UI state will be managed as close to the components as possible.
- **Decision Guide for State Location:**
  - **Global UI State (React Context API):** For UI-specific state that needs to be shared across
    many unrelated components or deep component trees, but is not server data. Examples:
    - Current theme (e.g., light/dark, if implemented).
    - User session information relevant to UI behavior (e.g., user's display name, basic permissions
      for UI elements - though Hilla endpoints themselves are secured).
    - Application-wide notification state (e.g., messages for global toast notifications).
    - _Usage:_ Create specific React Context providers (e.g., `ThemeContext`, `SessionContext`,
      `NotificationContext`) and consume them via `useContext` hook.
  - **Local Component State (`useState`, `useReducer`):** This will be the default and most common
    approach for managing UI-specific state that is not needed outside a component or its direct
    children. Examples:
    - Form input values before submission.
    - Toggle states (e.g., modal open/closed, dropdown expanded).
    - Data specific to a component's rendering logic, fetched via Hilla.
- **Store Structure / Slices (If Applicable for Global UI State via Context):**
  - If React Context is used for global UI state, context providers will be defined (e.g., in
    `apps/acci-eaf-control-plane/backend/src/main/frontend/store/` or
    `libs/ui-foundation-kit/src/store/`).
  - Example `SessionContext` (conceptual):
    - **Purpose:** To hold basic, non-sensitive user information for UI display (e.g., username,
      preferred language) after login. Sensitive session management is handled by Spring Security
      and HTTP-only cookies.
    - **State Shape:**
      `{ currentUser: { name: string; email: string; uiLanguage: string; } | null; isAuthenticated: boolean; }`
    - **Actions:** `login(userData)`, `logout()`, `setLanguage(lang)`.
- **Key Selectors / Actions (for Context):** Simple functions to update or retrieve data from the
  context.

## API Interaction Layer (Hilla Endpoints)

- **Client/Service Structure:**

  - All backend interactions from the frontend will primarily use the **type-safe TypeScript client
    modules automatically generated by Hilla** from Kotlin `@BrowserCallable` services. These are
    located in `Frontend/generated/endpoints.js` (or individual service files like
    `Frontend/generated/MyService.js`) within each Hilla application's frontend directory.
  - No separate `apiClient.ts` (like an Axios instance) is generally needed for Hilla backend calls,
    as Hilla handles the communication protocol.
  - Frontend components or custom hooks will directly import and call methods on these generated
    Hilla services. Example:

        ```typescript
        import { TenantService } from 'Frontend/generated/endpoints'; // Or specific service
        // ...
        const newTenant = await TenantService.createTenant({ name: 'NewCo', adminEmail: 'admin@newco.com' });
        ```

- **Error Handling & Retries (Frontend for Hilla Calls):**
  - Hilla service calls return Promises. Errors from the backend (including
    `EndpointValidationException` for business rule violations or Spring Security access denied
    exceptions) will cause the Promise to reject.
  - Frontend code **MUST** use `try/catch` blocks or `.catch()` on Promises to handle these errors
    gracefully.
  - Validation errors (from `EndpointValidationException`) often contain structured data about which
    parameters failed validation, which can be used to display user-friendly messages next to form
    fields.
  - Generic network errors or server unavailability will also result in rejected Promises. The UI
    should display appropriate feedback (e.g., "Could not connect to server. Please try again
    later.").
  - Client-side retry logic for Hilla calls is generally **not recommended** by default unless for
    specific idempotent read operations. Command operations should typically not be retried
    automatically by the client without user confirmation due to potential side effects. The backend
    services themselves should be designed for idempotency where appropriate for retries at that
    level.

## Routing Strategy

- **Routing Library:** **Hilla's built-in file-based routing mechanism** will be used for defining
  views and navigation within ACCI EAF frontend applications. This mechanism automatically maps
  `.tsx` files in the `src/main/frontend/views/` directory to URL routes.
- **Route Definitions (ACCI EAF Control Plane MVP - Conceptual):**

  | Path Pattern        | Component/Page (`src/main/frontend/views/...`) | Protection Level            | Notes                                                                    |
  | :------------------ | :--------------------------------------------- | :-------------------------- | :----------------------------------------------------------------------- |
  | `/login`            | `LoginView.tsx`                                | Public (Anonymous)          | Redirects to `/dashboard` if already authenticated.                      |
  | `/` or `/dashboard` | `dashboard/DashboardView.tsx`                  | Authenticated               | Default view after login. Content may vary by role (Super/Tenant Admin). |
  | `/tenants`          | `tenants/TenantListView.tsx`                   | Authenticated (SuperAdmin)  | Displays list of tenants; entry to create new tenants.                   |
  | `/users`            | `users/UserListView.tsx`                       | Authenticated (TenantAdmin) | Displays list of users for the current tenant admin's tenant.            |
  | `/licenses`         | `licenses/LicenseOverview.tsx` (Basic MVP)     | Authenticated               | Basic license information view.                                          |
  | `/profile`          | `profile/UserProfileView.tsx`                  | Authenticated               | User's own profile settings.                                             |
  | `*` (Not Found)     | `NotFoundView.tsx`                             | Public                      | Standard 404 page.                                                       |

- **Route Guards / Protection:**

  - Hilla integrates with Spring Security on the backend. Access to Hilla endpoints
    (`@BrowserCallable` services) is controlled by Spring Security annotations (e.g.,
    `@AnonymousAllowed`, `@PermitAll`, `@RolesAllowed` from `com.vaadin.flow.server.auth`).
  - If an unauthenticated user attempts to call a secured endpoint, Hilla will typically trigger a
    401 error, which the frontend client should handle (e.g., by redirecting to `/login`).
  - Client-side routing itself can be augmented with checks. Hilla's file-based router allows views
    to export a `config` object which can include access control rules. This can be used to redirect
    users or prevent rendering of views if they don't have the required roles (obtained after
    authentication). An example of such a config:

        ```typescript
        // Example in a view: someAdminView.tsx
        // import { ViewConfig } from '@vaadin/hilla-file-router/types.js';
        // // Assume some way to check roles, e.g., via a client-side session state or a Hilla call
        // import { userHasRole } from 'Frontend/auth'; // Placeholder for auth check
        //
        // export const config: ViewConfig = {
        //   access: async (context, commands) => {
        //     if (!await userHasRole('ROLE_SUPERADMIN')) { // Perform check
        //       return commands.redirect('/login'); // Or to a 'forbidden' page
        //     }
        //     return undefined; // Allows access
        //   }
        // };
        ```

  - A root layout component (e.g., `MainLayout.tsx` for authenticated routes) can also perform
    checks upon loading to redirect unauthenticated users.

## Build, Bundling, and Deployment

- **Build Process & Scripts (Hilla/Vite via Gradle/Nx):**
  - The frontend build is managed by Hilla, which uses **Vite** internally.
  - The **Gradle `build` task** for the Hilla application module (e.g.,
    `apps/acci-eaf-control-plane/backend/`), when run with `-Pvaadin.productionMode=true`, will
    trigger Hilla's production frontend build. This includes TypeScript compilation, Vite bundling,
    minification, tree-shaking, etc..
  - Within Nx, this Gradle build task will be orchestrated via an Nx target (e.g.,
    `nx build acci-eaf-control-plane-backend --configuration=production`).
  - Development mode (`./gradlew :apps:acci-eaf-control-plane:backend:bootRun` or
    `nx serve acci-eaf-control-plane-backend`) uses Vite's dev server for HMR and rapid updates.
- **Environment Configuration Management:**
  - Frontend-specific environment variables (e.g., feature flags client keys) can be managed using
    Vite's standard `.env` file mechanism (`.env`, `.env.development`, `.env.production`) located in
    the frontend root (`src/main/frontend/`). Variables must be prefixed with `VITE_` (or Hilla's
    configured prefix, typically `HILLA_` or `VAADIN_`) to be exposed to client-side code.
- **Key Bundling Optimizations:**
  - **Code Splitting:** Vite (used by Hilla) automatically performs route-based code splitting.
    Dynamic imports (`import('./MyComponent')`) can be used for further component-level splitting.
  - **Tree Shaking, Minification:** Handled by Vite during the production build. Compression (Gzip,
    Brotli) is typically handled by the hosting platform/web server serving the Spring Boot JAR.
  - **Lazy Loading:** React's `React.lazy` with `Suspense` can be used for lazy-loading components.
    Images should use `loading='lazy'` or a component like Vaadin's `<vaadin-image>` that supports
    lazy loading.
- **Deployment (as part of Spring Boot JAR):**
  - In a Hilla application, the production frontend build (static assets) is packaged into the
    Spring Boot application JAR (usually under `META-INF/resources/frontend/`).
  - The Spring Boot application then serves these static assets. This simplifies deployment for
    "less cloud-native" environments, as it's a single deployable artifact.
  - Caching of these static assets will be managed by standard HTTP cache headers set by Spring Boot
    or any reverse proxy/CDN in front of it.

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

## Accessibility (AX) Implementation Details

Aligned with our previous discussion and WCAG 2.1 Level AA target. Training module UI-H5 covers
this.

- **Semantic HTML:** Prioritize correct HTML5 elements. Vaadin components generally output semantic
  HTML. Custom components must follow suit.
- **ARIA Implementation:** Leverage Vaadin components' built-in ARIA attributes. For custom
  components, ensure appropriate ARIA roles, states, and properties are used as per WAI-ARIA
  Authoring Practices. Document in Storybook.
- **Keyboard Navigation:** All interactive elements must be focusable and operable via keyboard.
  Logical focus order. Vaadin components usually handle this well. Custom components must implement
  correct keyboard patterns.
- **Focus Management:** Critical for modals, dynamic content. Focus should be trapped in modals and
  returned appropriately on close. Route changes should ideally move focus to the new page's main
  content or H1.
- **Testing Tools for AX:**
  - **Automated:** Integrate **Axe-core** with Vitest/Jest (e.g., `jest-axe`) for component tests
    and with Playwright for E2E tests. Aim to fail builds on new WCAG AA violations.
  - **Developer Tools:** Encourage use of browser extensions like Axe DevTools.
  - **Vaadin Copilot:** Utilize its integrated accessibility checker during development.
  - **Manual:** Keyboard-only testing and basic screen reader checks (NVDA, VoiceOver) for critical
    flows.

## Performance Considerations

- **Image Optimization:** Use Vaadin's `<vaadin-image>` component (if available and suitable for
  React context) or standard HTML `<img>` with `loading="lazy"` and `srcset` where appropriate.
  Optimize image assets (WebP if feasible).
- **Code Splitting & Lazy Loading:** Hilla/Vite provides route-based code splitting. Use
  `React.lazy` and `Suspense` for component-level lazy loading of non-critical, heavy components.
- **Minimizing Re-renders (React):** Use `React.memo` for presentational components. Optimize
  `useEffect` dependencies. Avoid passing new object/array literals or inline functions as props in
  render methods where it causes unnecessary re-renders.
- **Debouncing/Throttling:** For event handlers like search input or window resize, use utilities if
  needed.
- **Virtualization:** For long lists or large data sets, Vaadin Grid (`<vaadin-grid>`) provides
  built-in virtualization. Leverage this for performance.
- **Hilla/Vite Build Optimizations:** Rely on Hilla's production build process for minification,
  tree-shaking, and bundling optimizations.
- **Performance Monitoring Tools:** Use browser DevTools (Performance tab, Lighthouse), WebPageTest
  for profiling.

## Internationalization (i18n) and Localization (l10n) Strategy

Based on our previous discussion and PRD requirements for strong i18n support.

- **Chosen i18n Library/Framework:** Align with Hilla's recommended approach, likely using
  **`i18next`** or a similar mature library well-suited for React, integrated with Vaadin component
  i18n capabilities.
- **Translation File Structure & Format:** **JSON files per language** (e.g., `en.json`, `de.json`).
  Organized within `src/main/frontend/i18n/` or `src/main/frontend/public/locales/`, potentially
  namespaced by feature/module. The ACCI EAF CLI will scaffold basic i18n setup.
- **Translation Key Naming Convention:** Consistent pattern (e.g., `viewName.section.label` or
  `common.button.save`).
- **Component Design for i18n:** All UI Foundation Kit components and application components MUST be
  i18n-ready (no hardcoded text, flexible layouts).
- **Developer Workflow:** Document process for adding keys and managing translations. Vaadin
  Copilot's "automated translations to get started" feature can be explored for initial drafts.
- **Language Switching & Context:** Implement a language switcher. Use React Context or Hilla's
  mechanisms to manage and propagate the current locale.
- **Formatting:** Handle dates, numbers, and currencies using locale-sensitive methods (e.g., `Intl`
  API, i18n library features).

## Feature Flag Management (Frontend SDK Usage)

- The ACCI EAF will have a dedicated Feature Flag Management Service. The frontend will consume this
  via a **JavaScript/TypeScript SDK** provided by the FFS team.
- **SDK Integration:** The JS/TS SDK will be initialized early in the application lifecycle (e.g.,
  in `index.ts`).
- **Context Provision:** The SDK will require an evaluation context (user ID, tenant ID, custom
  attributes). For client-side evaluation, this context needs to be trustworthy. We will implement
  **"Signed Contexts"** where the ACCI EAF application backend (e.g., Control Plane backend)
  generates and signs the context, which the frontend SDK then passes to the FFS evaluation endpoint
  (if evaluations are server-side for client SDK) or uses for local evaluation if the SDK receives
  rules.
- **Usage:** Wrap features or components in flag checks using the SDK's API (e.g.,
  `ffsClient.isFeatureEnabled('my-new-feature')`).
- **Fallback Behavior:** SDK must support hardcoded default values in code if the FFS is unavailable
  or a flag is not found.
- **Real-time Updates:** If the FFS uses NATS for real-time updates (as suggested in
  `Feature Flag Management Best Practices`), the JS/TS SDK should subscribe to these NATS events
  (likely via a WebSocket bridge managed by the application backend, as direct browser NATS clients
  are less common and might have limitations) to refresh flag states without polling.

## Frontend Security Considerations

- **Cross-Site Scripting (XSS) Prevention:**
  - React's JSX auto-escaping and Vaadin components' rendering mechanisms provide strong default
    protection against XSS when rendering dynamic content.
  - Avoid `dangerouslySetInnerHTML` or equivalent. If unavoidable, ensure data is sanitized with a
    robust library like DOMPurify.
  - A Content Security Policy (CSP) will be implemented via HTTP headers by the Spring Boot backend
    to further mitigate XSS.
- **Cross-Site Request Forgery (CSRF) Protection:**
  - Spring Security (used by Hilla backend) provides built-in CSRF protection. Hilla's communication
    mechanism should be compatible. For Hilla, CSRF protection is typically handled by default,
    ensuring requests originate from the same application.
- **Secure Token Storage & Handling (if any client-side):**
  - Hilla applications using Spring Security typically rely on **HTTP-only session cookies** for
    session management, which are not accessible to JavaScript, mitigating token theft risks.
  - If any other tokens are needed client-side, they must not be stored in `localStorage`.
- **Client-Side Data Validation:**
  - Client-side validation is for **User Experience improvement only**.
  - All critical data validation **MUST occur server-side** in the Hilla endpoints / Kotlin backend.
    Hilla can propagate backend validation errors.
- **API Key Exposure:**
  - No backend API keys should be embedded in client-side code. Proxy calls through the ACCI EAF
    backend if necessary.
- **Secure Communication (HTTPS):** All communication between client and server will be over HTTPS.

## Browser Support and Progressive Enhancement

- **Target Browsers:** Latest two stable versions of Chrome, Firefox, Safari, and Edge. Internet
  Explorer is NOT supported.
- **Polyfill Strategy:** Hilla/Vite's build process automatically includes necessary polyfills based
  on browser targets.
- **JavaScript Requirement:** Core application functionality REQUIRES JavaScript.
- **Progressive Enhancement:** The focus is on a rich client-side application experience.
- **CSS Compatibility:** Autoprefixer (via PostCSS, part of Vite) will handle vendor prefixes.

## Change Log

| Change                                                    | Date       | Version | Description                                                                                         | Author    |
| :-------------------------------------------------------- | :--------- | :------ | :-------------------------------------------------------------------------------------------------- | :-------- |
| Initial Draft of ACCI EAF Frontend Technical Architecture | 2025-06-04 | 0.1.0   | Comprehensive first pass based on Main Architecture, PRD, UI/UX Spec progress, and UI Kit Strategy. | Jane (DA) |
