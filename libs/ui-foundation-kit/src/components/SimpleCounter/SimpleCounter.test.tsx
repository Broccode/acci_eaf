import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { SimpleCounter } from './SimpleCounter';

/**
 * TDD Example: Test-Driven Development demonstration for SimpleCounter React component.
 * This test is written BEFORE the implementation to follow the red-green-refactor cycle.
 *
 * This serves as a reference implementation for EAF developers practicing TDD with React components.
 */
describe('SimpleCounter', () => {
  it('should render initial count of 0', () => {
    // Given
    render(<SimpleCounter />);

    // When
    const countDisplay = screen.getByTestId('count-display');

    // Then
    expect(countDisplay).toHaveTextContent('Count: 0');
  });

  it('should increment count when increment button is clicked', () => {
    // Given
    render(<SimpleCounter />);
    const incrementButton = screen.getByTestId('increment-button');
    const countDisplay = screen.getByTestId('count-display');

    // When
    fireEvent.click(incrementButton);

    // Then
    expect(countDisplay).toHaveTextContent('Count: 1');
  });

  it('should decrement count when decrement button is clicked', () => {
    // Given
    render(<SimpleCounter />);
    const decrementButton = screen.getByTestId('decrement-button');
    const countDisplay = screen.getByTestId('count-display');

    // When
    fireEvent.click(decrementButton);

    // Then
    expect(countDisplay).toHaveTextContent('Count: -1');
  });

  it('should handle multiple increments correctly', () => {
    // Given
    render(<SimpleCounter />);
    const incrementButton = screen.getByTestId('increment-button');
    const countDisplay = screen.getByTestId('count-display');

    // When
    fireEvent.click(incrementButton);
    fireEvent.click(incrementButton);
    fireEvent.click(incrementButton);

    // Then
    expect(countDisplay).toHaveTextContent('Count: 3');
  });

  it('should reset count to 0 when reset button is clicked', () => {
    // Given
    render(<SimpleCounter />);
    const incrementButton = screen.getByTestId('increment-button');
    const resetButton = screen.getByTestId('reset-button');
    const countDisplay = screen.getByTestId('count-display');

    // When - increment first, then reset
    fireEvent.click(incrementButton);
    fireEvent.click(incrementButton);
    fireEvent.click(resetButton);

    // Then
    expect(countDisplay).toHaveTextContent('Count: 0');
  });
});
