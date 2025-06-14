import type { Meta, StoryObj } from '@storybook/react';
import { EafTable } from './EafTable';

const meta: Meta<typeof EafTable> = {
  component: EafTable,
  title: 'Components/EafTable',
  tags: ['autodocs'],
  argTypes: {
    striped: {
      control: 'boolean',
    },
    bordered: {
      control: 'boolean',
    },
    hover: {
      control: 'boolean',
    },
    className: {
      control: 'text',
    },
  },
  args: {
    striped: false,
    bordered: false,
    hover: false,
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Table component that provides a flexible table with customizable styling using Tailwind CSS.',
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof EafTable>;

const sampleColumns = [
  { key: 'id', title: 'ID', width: '80px', align: 'center' as const },
  { key: 'name', title: 'Name', align: 'left' as const },
  { key: 'email', title: 'Email', align: 'left' as const },
  { key: 'role', title: 'Role', align: 'center' as const },
  { key: 'status', title: 'Status', align: 'center' as const },
  { key: 'actions', title: 'Actions', width: '120px', align: 'right' as const },
];

const sampleData = [
  {
    id: 1,
    name: 'John Doe',
    email: 'john.doe@example.com',
    role: 'Admin',
    status: 'Active',
    actions: 'Edit | Delete',
  },
  {
    id: 2,
    name: 'Jane Smith',
    email: 'jane.smith@example.com',
    role: 'User',
    status: 'Active',
    actions: 'Edit | Delete',
  },
  {
    id: 3,
    name: 'Bob Johnson',
    email: 'bob.johnson@example.com',
    role: 'User',
    status: 'Inactive',
    actions: 'Edit | Delete',
  },
  {
    id: 4,
    name: 'Alice Brown',
    email: 'alice.brown@example.com',
    role: 'Moderator',
    status: 'Active',
    actions: 'Edit | Delete',
  },
  {
    id: 5,
    name: 'Charlie Wilson',
    email: 'charlie.wilson@example.com',
    role: 'User',
    status: 'Pending',
    actions: 'Edit | Delete',
  },
];

export const Default: Story = {
  args: {
    columns: sampleColumns,
    data: sampleData,
  },
};

export const Striped: Story = {
  args: {
    columns: sampleColumns,
    data: sampleData,
    striped: true,
  },
};

export const Bordered: Story = {
  args: {
    columns: sampleColumns,
    data: sampleData,
    bordered: true,
  },
};

export const WithHover: Story = {
  args: {
    columns: sampleColumns,
    data: sampleData,
    hover: true,
  },
};

export const FullyStyled: Story = {
  args: {
    columns: sampleColumns,
    data: sampleData,
    striped: true,
    bordered: true,
    hover: true,
  },
};

export const SimpleTable: Story = {
  args: {
    columns: [
      { key: 'product', title: 'Product' },
      { key: 'price', title: 'Price', align: 'right' as const },
      { key: 'stock', title: 'Stock', align: 'center' as const },
    ],
    data: [
      { product: 'Laptop', price: '$999', stock: '15' },
      { product: 'Mouse', price: '$25', stock: '50' },
      { product: 'Keyboard', price: '$75', stock: '30' },
    ],
    bordered: true,
  },
};

export const EmptyTable: Story = {
  args: {
    columns: sampleColumns,
    data: [],
    bordered: true,
  },
};
