# UI Foundation Kit

The ACCI EAF UI Foundation Kit provides a comprehensive collection of reusable React components, design tokens, and patterns built on top of Vaadin with Hilla integration.

## Overview

The UI Foundation Kit ensures consistency across all ACCI EAF applications while providing:

- **Design System**: Unified visual language and component library
- **Vaadin/Hilla Integration**: Type-safe full-stack components
- **Accessibility**: WCAG 2.1 AA compliant components
- **Theming**: Customizable design tokens and themes
- **Internationalization**: Built-in i18n support
- **Storybook**: Interactive component documentation

## Component Categories

### 📋 Form Components

- **Text Input**: Single and multi-line text fields
- **Select**: Dropdown and multi-select components
- **Checkbox/Radio**: Boolean and choice selections
- **Date Picker**: Date and time selection
- **File Upload**: Drag-and-drop file handling
- **Form Validation**: Real-time validation feedback

### 🧭 Navigation

- **App Shell**: Main application layout structure
- **Sidebar**: Collapsible navigation panels
- **Breadcrumbs**: Hierarchical navigation aids
- **Tabs**: Content organization within views
- **Pagination**: Large dataset navigation

### 📊 Data Display

- **Data Grid**: Sortable, filterable tables
- **Cards**: Content containers with actions
- **Charts**: Data visualization components
- **Badges**: Status and count indicators
- **Progress**: Loading and completion states

### 💬 Feedback

- **Notifications**: Toast and banner messages
- **Dialogs**: Modal confirmation and input dialogs
- **Tooltips**: Contextual help and information
- **Loading States**: Spinners and skeleton screens

### 🎨 Layout

- **Grid System**: Responsive layout utilities
- **Containers**: Content wrapping and spacing
- **Dividers**: Visual content separation
- **Spacing**: Consistent margin and padding utilities

## Design Tokens

The Foundation Kit uses design tokens for consistent styling:

```css
/* Color Tokens */
--color-primary: #1976d2;
--color-secondary: #dc004e;
--color-surface: #ffffff;
--color-background: #fafafa;

/* Typography Tokens */
--font-family-primary: 'Inter', sans-serif;
--font-size-small: 0.875rem;
--font-size-medium: 1rem;
--font-size-large: 1.25rem;

/* Spacing Tokens */
--spacing-xs: 0.25rem;
--spacing-sm: 0.5rem;
--spacing-md: 1rem;
--spacing-lg: 1.5rem;
```

## Usage Examples

### Basic Component Usage

```typescript
import { Button, TextField, Dialog } from '@acci-eaf/ui-foundation-kit';

function CreateTenantForm() {
  const [isOpen, setIsOpen] = useState(false);
  const [tenantName, setTenantName] = useState('');

  return (
    <>
      <Button onClick={() => setIsOpen(true)}>
        Create Tenant
      </Button>
      
      <Dialog isOpen={isOpen} onClose={() => setIsOpen(false)}>
        <TextField
          label="Tenant Name"
          value={tenantName}
          onChange={setTenantName}
          required
        />
        <Button variant="primary">Create</Button>
      </Dialog>
    </>
  );
}
```

### Hilla Integration

```typescript
import { useEndpoint } from '@vaadin/hilla-react';
import { TenantService } from 'Frontend/generated/endpoints';
import { DataGrid } from '@acci-eaf/ui-foundation-kit';

function TenantList() {
  const tenants = useEndpoint(TenantService, (service) => service.getAllTenants());
  
  return (
    <DataGrid
      items={tenants}
      columns={[
        { key: 'name', header: 'Tenant Name' },
        { key: 'adminEmail', header: 'Admin Email' },
        { key: 'status', header: 'Status' }
      ]}
    />
  );
}
```

## Theming

### Custom Theme

```typescript
import { createTheme } from '@acci-eaf/ui-foundation-kit';

const customTheme = createTheme({
  colors: {
    primary: '#2563eb',
    secondary: '#7c3aed'
  },
  typography: {
    fontFamily: 'Roboto, sans-serif'
  }
});

function App() {
  return (
    <ThemeProvider theme={customTheme}>
      <YourApp />
    </ThemeProvider>
  );
}
```

## Storybook Integration

The UI Foundation Kit includes comprehensive Storybook documentation:

```bash
# Start Storybook
cd libs/ui-foundation-kit
npm run storybook
```

Storybook provides:

- **Interactive Examples**: Try components with different props
- **Code Snippets**: Copy-paste ready component usage
- **Design Tokens**: Visual reference for colors, typography, spacing
- **Accessibility**: Built-in accessibility testing

## Development Workflow

### Adding New Components

1. **Design**: Create component designs following the design system
2. **Develop**: Build the component with TypeScript and React
3. **Test**: Write unit tests and accessibility tests
4. **Document**: Create Storybook stories
5. **Review**: Code review focusing on API design and accessibility
6. **Publish**: Release as part of the Foundation Kit

### Component API Guidelines

- **Consistent Naming**: Use clear, descriptive prop names
- **Type Safety**: Leverage TypeScript for robust APIs
- **Accessibility**: Include ARIA attributes and keyboard navigation
- **Customization**: Allow theme overrides and custom styling
- **Performance**: Optimize for bundle size and runtime performance

## Best Practices

1. **Composition**: Build complex UIs by composing simple components
2. **Accessibility**: Test with screen readers and keyboard navigation
3. **Performance**: Use React.memo and lazy loading for large lists
4. **Consistency**: Follow the established patterns and naming conventions
5. **Documentation**: Keep Storybook stories up to date

*This is a placeholder document. Detailed component documentation, design guidelines, and usage examples will be added as the UI Foundation Kit evolves.*
