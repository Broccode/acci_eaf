import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { EafButton } from './EafButton';

describe('EafButton', () => {
  it('renders button with text', () => {
    render(<EafButton>Click me</EafButton>);

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
  });

  it('renders with primary variant by default', () => {
    render(<EafButton>Test Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('eaf-button--primary');
  });

  it('renders with secondary variant when specified', () => {
    render(<EafButton variant="secondary">Secondary Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('eaf-button--secondary');
  });

  it('applies medium size by default', () => {
    render(<EafButton>Medium Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('eaf-button--medium');
  });

  it('can be disabled', () => {
    render(<EafButton disabled>Disabled Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });
});
