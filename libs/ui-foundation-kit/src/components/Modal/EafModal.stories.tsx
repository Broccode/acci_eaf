import type { Meta, StoryObj } from '@storybook/react';
import React, { useState } from 'react';
import { EafModal } from './EafModal';
import { EafButton } from '../Button/EafButton';

const meta: Meta<typeof EafModal> = {
  component: EafModal,
  title: 'Components/EafModal',
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['small', 'medium', 'large', 'full'],
    },
    showCloseButton: {
      control: 'boolean',
    },
    closeOnOverlayClick: {
      control: 'boolean',
    },
    closeOnEscape: {
      control: 'boolean',
    },
    className: {
      control: 'text',
    },
  },
  args: {
    size: 'medium',
    showCloseButton: true,
    closeOnOverlayClick: true,
    closeOnEscape: true,
  },
  parameters: {
    docs: {
      description: {
        component:
          'EAF Modal component that provides a modal dialog with overlay and customizable content.',
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof EafModal>;

// Template component for interactive stories
const ModalTemplate = (args: any) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <EafButton onClick={() => setIsOpen(true)}>Open Modal</EafButton>
      <EafModal {...args} isOpen={isOpen} onClose={() => setIsOpen(false)}>
        {args.children}
      </EafModal>
    </>
  );
};

export const Default: Story = {
  render: ModalTemplate,
  args: {
    title: 'Default Modal',
    children: (
      <div>
        <p className="mb-4">
          This is a default modal with standard settings. You can close it by
          clicking the X button, clicking outside the modal, or pressing the
          Escape key.
        </p>
        <p>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua.
        </p>
      </div>
    ),
  },
};

export const Small: Story = {
  render: ModalTemplate,
  args: {
    title: 'Small Modal',
    size: 'small',
    children: (
      <div>
        <p>This is a small modal with limited width.</p>
      </div>
    ),
  },
};

export const Large: Story = {
  render: ModalTemplate,
  args: {
    title: 'Large Modal',
    size: 'large',
    children: (
      <div>
        <p className="mb-4">
          This is a large modal that can accommodate more content.
        </p>
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className="p-4 bg-gray-100 rounded">
            <h3 className="font-semibold mb-2">Section 1</h3>
            <p>Content for the first section.</p>
          </div>
          <div className="p-4 bg-gray-100 rounded">
            <h3 className="font-semibold mb-2">Section 2</h3>
            <p>Content for the second section.</p>
          </div>
        </div>
        <p>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do
          eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad
          minim veniam, quis nostrud exercitation ullamco laboris nisi ut
          aliquip ex ea commodo consequat.
        </p>
      </div>
    ),
  },
};

export const WithoutCloseButton: Story = {
  render: ModalTemplate,
  args: {
    title: 'Modal without Close Button',
    showCloseButton: false,
    children: (
      <div>
        <p className="mb-4">
          This modal doesn&apos;t have a close button. You can still close it by
          clicking outside or pressing Escape.
        </p>
        <EafButton onClick={() => {}}>Custom Action</EafButton>
      </div>
    ),
  },
};

export const NoOverlayClose: Story = {
  render: ModalTemplate,
  args: {
    title: 'No Overlay Close',
    closeOnOverlayClick: false,
    children: (
      <div>
        <p className="mb-4">
          This modal cannot be closed by clicking the overlay. Use the close
          button or Escape key.
        </p>
      </div>
    ),
  },
};

export const NoEscapeClose: Story = {
  render: ModalTemplate,
  args: {
    title: 'No Escape Close',
    closeOnEscape: false,
    children: (
      <div>
        <p className="mb-4">
          This modal cannot be closed with the Escape key. Use the close button
          or click outside.
        </p>
      </div>
    ),
  },
};

export const FormModal: Story = {
  render: ModalTemplate,
  args: {
    title: 'Contact Form',
    children: (
      <form className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Name
          </label>
          <input
            type="text"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Enter your name"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Email
          </label>
          <input
            type="email"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Enter your email"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Message
          </label>
          <textarea
            rows={4}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Enter your message"
          />
        </div>
        <div className="flex justify-end space-x-2 pt-4">
          <EafButton variant="secondary">Cancel</EafButton>
          <EafButton variant="primary">Send Message</EafButton>
        </div>
      </form>
    ),
  },
};

export const LongContent: Story = {
  render: ModalTemplate,
  args: {
    title: 'Modal with Long Content',
    children: (
      <div>
        <p className="mb-4">
          This modal demonstrates scrollable content when the content exceeds
          the modal height.
        </p>
        {Array.from({ length: 20 }, (_, i) => (
          <p key={i} className="mb-4">
            Paragraph {i + 1}: Lorem ipsum dolor sit amet, consectetur
            adipiscing elit. Sed do eiusmod tempor incididunt ut labore et
            dolore magna aliqua. Ut enim ad minim veniam, quis nostrud
            exercitation ullamco laboris nisi ut aliquip ex ea commodo
            consequat. Duis aute irure dolor in reprehenderit in voluptate velit
            esse cillum dolore eu fugiat nulla pariatur.
          </p>
        ))}
      </div>
    ),
  },
};
