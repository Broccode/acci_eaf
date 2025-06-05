import React, { useState } from 'react'

/**
 * TDD Example: Simple Counter React component demonstrating Test-Driven Development.
 * This implementation is created AFTER the test to follow the red-green-refactor cycle.
 *
 * This serves as a reference implementation for EAF developers practicing TDD with React components.
 */
export const SimpleCounter: React.FC = () => {
  const [count, setCount] = useState(0)

  const increment = () => setCount(prev => prev + 1)
  const decrement = () => setCount(prev => prev - 1)
  const reset = () => setCount(0)

  return (
    <div>
      <div data-testid="count-display">Count: {count}</div>
      <button data-testid="increment-button" onClick={increment}>
        +
      </button>
      <button data-testid="decrement-button" onClick={decrement}>
        -
      </button>
      <button data-testid="reset-button" onClick={reset}>
        Reset
      </button>
    </div>
  )
}
