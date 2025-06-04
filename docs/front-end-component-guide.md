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
