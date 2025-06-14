import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { EafGrid } from './EafGrid';

describe('EafGrid', () => {
  it('renders children correctly', () => {
    render(
      <EafGrid>
        <div>Child 1</div>
        <div>Child 2</div>
      </EafGrid>
    );

    expect(screen.getByText('Child 1')).toBeInTheDocument();
    expect(screen.getByText('Child 2')).toBeInTheDocument();
  });

  it('applies default grid classes', () => {
    render(
      <EafGrid data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('grid');
    expect(grid).toHaveClass('gap-4'); // default medium gap
  });

  it('applies numeric columns correctly', () => {
    render(
      <EafGrid columns={3} data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('grid-cols-3');
  });

  it('applies auto-fit columns', () => {
    render(
      <EafGrid columns="fit" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('grid-cols-[repeat(auto-fit,minmax(200px,1fr))]');
  });

  it('applies auto columns by default', () => {
    render(
      <EafGrid columns="auto" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('grid-cols-[repeat(auto-fill,minmax(200px,1fr))]');
  });

  it('applies small gap', () => {
    render(
      <EafGrid gap="small" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('gap-2');
  });

  it('applies large gap', () => {
    render(
      <EafGrid gap="large" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('gap-8');
  });

  it('applies no gap', () => {
    render(
      <EafGrid gap="none" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('gap-0');
  });

  it('applies custom className', () => {
    render(
      <EafGrid className="custom-class" data-testid="grid">
        <div>Content</div>
      </EafGrid>
    );

    const grid = screen.getByTestId('grid');
    expect(grid).toHaveClass('custom-class');
  });
});
