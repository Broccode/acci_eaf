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
