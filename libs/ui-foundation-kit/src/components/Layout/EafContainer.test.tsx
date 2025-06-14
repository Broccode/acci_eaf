import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { EafContainer } from './EafContainer';

describe('EafContainer', () => {
  it('renders children correctly', () => {
    render(
      <EafContainer>
        <div>Container content</div>
      </EafContainer>
    );

    expect(screen.getByText('Container content')).toBeInTheDocument();
  });

  it('applies default container classes', () => {
    render(
      <EafContainer data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('mx-auto');
    expect(container).toHaveClass('w-full');
    expect(container).toHaveClass('max-w-6xl'); // default large size
    expect(container).toHaveClass('px-6'); // default medium padding
    expect(container).toHaveClass('py-4'); // default medium padding
  });

  it('applies small size correctly', () => {
    render(
      <EafContainer size="small" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('max-w-2xl');
  });

  it('applies medium size correctly', () => {
    render(
      <EafContainer size="medium" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('max-w-4xl');
  });

  it('applies full size correctly', () => {
    render(
      <EafContainer size="full" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('max-w-full');
  });

  it('applies no padding', () => {
    render(
      <EafContainer padding="none" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).not.toHaveClass('px-6');
    expect(container).not.toHaveClass('py-4');
  });

  it('applies small padding', () => {
    render(
      <EafContainer padding="small" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('px-4');
    expect(container).toHaveClass('py-2');
  });

  it('applies large padding', () => {
    render(
      <EafContainer padding="large" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('px-8');
    expect(container).toHaveClass('py-8');
  });

  it('applies custom className', () => {
    render(
      <EafContainer className="custom-class" data-testid="container">
        <div>Content</div>
      </EafContainer>
    );

    const container = screen.getByTestId('container');
    expect(container).toHaveClass('custom-class');
  });
});
