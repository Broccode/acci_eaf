import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { EafContainer } from './EafContainer';

const meta: Meta<typeof EafContainer> = {
  component: EafContainer,
  title: 'Layout/EafContainer',
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['small', 'medium', 'large', 'full'],
    },
    padding: {
      control: 'select',
      options: ['none', 'small', 'medium', 'large'],
    },
    className: {
      control: 'text',
    },
  },
  args: {
    size: 'large',
    padding: 'medium',
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Container component that provides consistent page-level layout with proper spacing and responsive behavior.',
      },
    },
  },
};
export default meta;

type Story = StoryObj<typeof EafContainer>;

// Sample content for demonstrations
const SampleContent = () => (
  <div className="bg-gray-100 border border-gray-300 p-6 rounded">
    <h2 className="text-xl font-bold mb-4">Container Content</h2>
    <p className="mb-4">
      This is sample content inside the container. The container provides
      consistent spacing and maximum width constraints for better readability
      and layout.
    </p>
    <p className="mb-4">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod
      tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim
      veniam, quis nostrud exercitation ullamco laboris.
    </p>
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="bg-blue-100 p-4 rounded">
        <h3 className="font-semibold">Feature 1</h3>
        <p className="text-sm">Description of feature 1</p>
      </div>
      <div className="bg-green-100 p-4 rounded">
        <h3 className="font-semibold">Feature 2</h3>
        <p className="text-sm">Description of feature 2</p>
      </div>
    </div>
  </div>
);

export const Default: Story = {
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const Small: Story = {
  args: {
    size: 'small',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const Medium: Story = {
  args: {
    size: 'medium',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const Large: Story = {
  args: {
    size: 'large',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const FullWidth: Story = {
  args: {
    size: 'full',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const NoPadding: Story = {
  args: {
    padding: 'none',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const SmallPadding: Story = {
  args: {
    padding: 'small',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const LargePadding: Story = {
  args: {
    padding: 'large',
  },
  render: args => (
    <EafContainer {...args}>
      <SampleContent />
    </EafContainer>
  ),
};

export const NestedContainers: Story = {
  render: args => (
    <EafContainer size="full" padding="large">
      <div className="bg-red-50 border border-red-200 p-4 rounded mb-4">
        <h2 className="text-lg font-bold mb-2">Outer Container (Full Width)</h2>
        <EafContainer {...args}>
          <div className="bg-blue-50 border border-blue-200 p-4 rounded">
            <h3 className="font-semibold">Inner Container</h3>
            <p>This demonstrates nested containers for complex layouts.</p>
          </div>
        </EafContainer>
      </div>
    </EafContainer>
  ),
  args: {
    size: 'medium',
    padding: 'medium',
  },
};
