## Front-End Coding Standards

This document outlines coding standards specifically for front-end development within the ACCI EAF.
For general coding standards applicable across the EAF (including backend), please refer to
`docs/operational-guidelines.md` (section: Coding Standards).

### Framework & Core Libraries

- **Vaadin with Hilla, using React and TypeScript** is the chosen stack. This provides end-to-end
  type safety from the Kotlin/Spring backend to the frontend and simplifies client-server
  communication.

### Component Architecture & Naming

- The UI Foundation Kit will be based on **standard Vaadin UI components** (from
  `@vaadin/react-components`).
- **Custom React components** will be developed for specific needs not met by standard Vaadin
  components.
- **Component Naming Convention:** **PascalCase** for React component files and named exports (e.g.,
  `UserProfileCard.tsx`, `export function UserProfileCard(...)`).
- **Organization:** Globally reusable components reside in `libs/ui-foundation-kit/src/components/`.
  Application-specific components reside within the application's frontend structure.

### Styling Approach

- Vaadin's Lumo theme will be customized to reflect Axians' branding.
- Custom React components will utilize **CSS Modules** for scoped styling to prevent conflicts and
  ensure maintainability, aligning with Hilla project structures.

### Key Design Patterns & Practices

- Utilize **React Hooks** (`useState`, `useEffect`, `useContext`, custom hooks) for managing
  component logic and state.
- Adhere to **file-based routing** provided by Hilla.
- Leverage **type-safe service invocation patterns** enabled by Hilla.
- Favor **composition over inheritance** for UI components.
- All UI Foundation Kit components and application components MUST be **i18n-ready**.
- Ensure components meet **accessibility standards (WCAG 2.1 Level AA)**.
