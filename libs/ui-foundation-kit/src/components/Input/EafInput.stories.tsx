import type { Meta, StoryObj } from '@storybook/react';
import { EafInput } from './EafInput';
import '@vaadin/react-components/css/Lumo.css';

const meta: Meta<typeof EafInput> = {
  component: EafInput,
  title: 'Components/EafInput',
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['outlined', 'filled'],
    },
    size: {
      control: 'select',
      options: ['small', 'medium', 'large'],
    },
    disabled: {
      control: 'boolean',
    },
    readonly: {
      control: 'boolean',
    },
    required: {
      control: 'boolean',
    },
    label: {
      control: 'text',
    },
    placeholder: {
      control: 'text',
    },
    helperText: {
      control: 'text',
    },
    errorMessage: {
      control: 'text',
    },
  },
  args: {
    label: 'Username',
    placeholder: 'Enter your username',
    variant: 'outlined',
    size: 'medium',
    disabled: false,
    readonly: false,
    required: false,
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Input component that wraps the Vaadin TextField, providing a consistent API for form inputs.',
      },
    },
  },
};
export default meta;

type Story = StoryObj<typeof EafInput>;

export const Default: Story = {
  args: {
    label: 'Username',
    placeholder: 'Enter your username',
  },
};

export const Filled: Story = {
  args: {
    variant: 'filled',
    label: 'Email',
    placeholder: 'Enter your email',
  },
};

export const Small: Story = {
  args: {
    size: 'small',
    label: 'Small Input',
    placeholder: 'Small size',
  },
};

export const Large: Story = {
  args: {
    size: 'large',
    label: 'Large Input',
    placeholder: 'Large size',
  },
};

export const WithHelperText: Story = {
  args: {
    label: 'Password',
    placeholder: 'Enter your password',
    helperText: 'Password must be at least 8 characters long',
  },
};

export const WithError: Story = {
  args: {
    label: 'Email',
    placeholder: 'Enter your email',
    errorMessage: 'Please enter a valid email address',
  },
};

export const Required: Story = {
  args: {
    label: 'Required Field',
    placeholder: 'This field is required',
    required: true,
  },
};

export const Disabled: Story = {
  args: {
    label: 'Disabled Input',
    placeholder: 'This input is disabled',
    disabled: true,
  },
};

export const Readonly: Story = {
  args: {
    label: 'Readonly Input',
    value: 'This value cannot be changed',
    readonly: true,
  },
};

export const WithHillaIntegration: Story = {
  render: args => {
    // const { data, error } = SomeEndpoint.use(); // Example Hilla endpoint
    return (
      <div>
        <p>
          This example shows how the input could be used with Hilla for form
          handling.
        </p>
        <EafInput
          {...args}
          onChange={e => {
            console.log('Input value changed:', e.target.value);
            // Here you would typically update your form state or call a Hilla endpoint
          }}
        />
      </div>
    );
  },
  args: {
    label: 'User Profile',
    placeholder: 'Enter profile information',
    helperText: 'This data will be saved to the backend via Hilla',
  },
};
