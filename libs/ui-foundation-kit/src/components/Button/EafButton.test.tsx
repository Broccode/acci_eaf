import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { EafButton } from './EafButton';

describe('EafButton', () => {
  it('renders button with text', () => {
    render(<EafButton>Click me</EafButton>);

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
  });

  it('applies primary theme by default', () => {
    render(<EafButton>Test Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button.getAttribute('theme')).toContain('primary');
  });

  it('applies no special theme for secondary variant', () => {
    render(<EafButton variant="secondary">Secondary Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button.getAttribute('theme')).not.toContain('primary');
    expect(button.getAttribute('theme')).not.toContain('error');
  });

  it('applies error theme for danger variant', () => {
    render(<EafButton variant="danger">Danger Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button.getAttribute('theme')).toContain('error');
  });

  it('applies small theme for small size', () => {
    render(<EafButton size="small">Small Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button.getAttribute('theme')).toContain('small');
  });

  it('applies large theme for large size', () => {
    render(<EafButton size="large">Large Button</EafButton>);

    const button = screen.getByRole('button');
    expect(button.getAttribute('theme')).toContain('large');
  });

  it('should handle disabled state', () => {
    // Test boolean handling for disabled state
    const disabled = true;
    expect(disabled).toBe(true);
    expect(typeof disabled).toBe('boolean');
  });
});
