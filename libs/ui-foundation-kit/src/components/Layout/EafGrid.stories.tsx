import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { EafGrid } from './EafGrid';

const meta: Meta<typeof EafGrid> = {
  component: EafGrid,
  title: 'Layout/EafGrid',
  tags: ['autodocs'],
  argTypes: {
    columns: {
      control: 'select',
      options: [1, 2, 3, 4, 6, 'auto', 'fit'],
    },
    gap: {
      control: 'select',
      options: ['none', 'small', 'medium', 'large'],
    },
    className: {
      control: 'text',
    },
  },
  args: {
    columns: 'auto',
    gap: 'medium',
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Grid component that provides a flexible grid layout system using CSS Grid with Tailwind utility classes.',
      },
    },
  },
};
export default meta;

type Story = StoryObj<typeof EafGrid>;

// Sample grid items for demonstrations
const GridItem = ({
  children,
  className = '',
}: {
  children: React.ReactNode;
  className?: string;
}) => (
  <div
    className={`bg-blue-100 border border-blue-300 p-4 rounded text-center ${className}`}
  >
    {children}
  </div>
);

export const Default: Story = {
  render: args => (
    <EafGrid {...args}>
      <GridItem>Item 1</GridItem>
      <GridItem>Item 2</GridItem>
      <GridItem>Item 3</GridItem>
      <GridItem>Item 4</GridItem>
      <GridItem>Item 5</GridItem>
      <GridItem>Item 6</GridItem>
    </EafGrid>
  ),
};

export const TwoColumns: Story = {
  args: {
    columns: 2,
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>Column 1</GridItem>
      <GridItem>Column 2</GridItem>
      <GridItem>Column 1</GridItem>
      <GridItem>Column 2</GridItem>
    </EafGrid>
  ),
};

export const ThreeColumns: Story = {
  args: {
    columns: 3,
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>Item 1</GridItem>
      <GridItem>Item 2</GridItem>
      <GridItem>Item 3</GridItem>
      <GridItem>Item 4</GridItem>
      <GridItem>Item 5</GridItem>
      <GridItem>Item 6</GridItem>
    </EafGrid>
  ),
};

export const AutoFit: Story = {
  args: {
    columns: 'fit',
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>Auto-fit 1</GridItem>
      <GridItem>Auto-fit 2</GridItem>
      <GridItem>Auto-fit 3</GridItem>
      <GridItem>Auto-fit 4</GridItem>
    </EafGrid>
  ),
};

export const SmallGap: Story = {
  args: {
    columns: 3,
    gap: 'small',
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>Small gap 1</GridItem>
      <GridItem>Small gap 2</GridItem>
      <GridItem>Small gap 3</GridItem>
    </EafGrid>
  ),
};

export const LargeGap: Story = {
  args: {
    columns: 2,
    gap: 'large',
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>Large gap 1</GridItem>
      <GridItem>Large gap 2</GridItem>
    </EafGrid>
  ),
};

export const NoGap: Story = {
  args: {
    columns: 4,
    gap: 'none',
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem>No gap 1</GridItem>
      <GridItem>No gap 2</GridItem>
      <GridItem>No gap 3</GridItem>
      <GridItem>No gap 4</GridItem>
    </EafGrid>
  ),
};

export const ResponsiveCards: Story = {
  args: {
    columns: 'auto',
    gap: 'medium',
  },
  render: args => (
    <EafGrid {...args}>
      <GridItem className="min-h-[120px]">
        <h3 className="font-bold mb-2">Card 1</h3>
        <p className="text-sm">
          This is a responsive card that adapts to the grid.
        </p>
      </GridItem>
      <GridItem className="min-h-[120px]">
        <h3 className="font-bold mb-2">Card 2</h3>
        <p className="text-sm">
          Cards automatically adjust their width based on available space.
        </p>
      </GridItem>
      <GridItem className="min-h-[120px]">
        <h3 className="font-bold mb-2">Card 3</h3>
        <p className="text-sm">
          Perfect for dashboard layouts and content grids.
        </p>
      </GridItem>
      <GridItem className="min-h-[120px]">
        <h3 className="font-bold mb-2">Card 4</h3>
        <p className="text-sm">
          Resize the viewport to see the responsive behavior.
        </p>
      </GridItem>
    </EafGrid>
  ),
};
