## State Management In-Depth

* **Chosen Solution & Philosophy:** As Hilla provides direct, type-safe methods to call backend services and manage server data, the need for complex client-side global state management solutions (like Redux or Zustand) for server cache is significantly reduced.
  * Server data will primarily be fetched on demand via Hilla endpoint calls within components or custom React Hooks.
  * Client-side UI state will be managed as close to the components as possible.
* **Decision Guide for State Location:**
  * **Global UI State (React Context API):** For UI-specific state that needs to be shared across many unrelated components or deep component trees, but is not server data. Examples:
    * Current theme (e.g., light/dark, if implemented).
    * User session information relevant to UI behavior (e.g., user's display name, basic permissions for UI elements - though Hilla endpoints themselves are secured).
    * Application-wide notification state (e.g., messages for global toast notifications).
    * _Usage:_ Create specific React Context providers (e.g., `ThemeContext`, `SessionContext`, `NotificationContext`) and consume them via `useContext` hook.
  * **Local Component State (`useState`, `useReducer`):** This will be the default and most common approach for managing UI-specific state that is not needed outside a component or its direct children. Examples:
    * Form input values before submission.
    * Toggle states (e.g., modal open/closed, dropdown expanded).
    * Data specific to a component's rendering logic, fetched via Hilla.
* **Store Structure / Slices (If Applicable for Global UI State via Context):**
  * If React Context is used for global UI state, context providers will be defined (e.g., in `apps/acci-eaf-control-plane/backend/src/main/frontend/store/` or `libs/ui-foundation-kit/src/store/`).
  * Example `SessionContext` (conceptual):
    * **Purpose:** To hold basic, non-sensitive user information for UI display (e.g., username, preferred language) after login. Sensitive session management is handled by Spring Security and HTTP-only cookies.
    * **State Shape:** `{ currentUser: { name: string; email: string; uiLanguage: string; } | null; isAuthenticated: boolean; }`
    * **Actions:** `login(userData)`, `logout()`, `setLanguage(lang)`.
* **Key Selectors / Actions (for Context):** Simple functions to update or retrieve data from the context.
