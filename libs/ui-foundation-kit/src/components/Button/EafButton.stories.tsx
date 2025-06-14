import type { Meta, StoryObj } from '@storybook/react';
import { EafButton } from './EafButton';
import '@vaadin/react-components/css/Lumo.css';

const meta: Meta<typeof EafButton> = {
  component: EafButton,
  title: 'Components/EafButton',
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['primary', 'secondary', 'danger'],
    },
    size: {
      control: 'select',
      options: ['small', 'medium', 'large'],
    },
    disabled: {
      control: 'boolean',
    },
    children: {
      control: 'text',
    },
  },
  args: {
    children: 'Click me',
    disabled: false,
    variant: 'primary',
    size: 'medium',
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Button component that wraps the Vaadin Button, mapping its props to Vaadin themes.',
      },
    },
  },
};
export default meta;

type Story = StoryObj<typeof EafButton>;

export const Primary: Story = {
  args: {
    variant: 'primary',
  },
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
  },
};

export const Danger: Story = {
  args: {
    variant: 'danger',
  },
};

export const Small: Story = {
  args: {
    size: 'small',
  },
};

export const Large: Story = {
  args: {
    size: 'large',
  },
};

export const Disabled: Story = {
  args: {
    disabled: true,
  },
};
