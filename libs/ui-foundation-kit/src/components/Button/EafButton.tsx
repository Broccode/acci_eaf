import React from 'react'

export interface EafButtonProps {
  children: React.ReactNode
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'small' | 'medium' | 'large'
  disabled?: boolean
  onClick?: () => void
}

/**
 * EAF Button component - extends Vaadin Button with EAF theming
 * This is a placeholder implementation for the monorepo setup
 */
export const EafButton: React.FC<EafButtonProps> = ({
  children,
  variant = 'primary',
  size = 'medium',
  disabled = false,
  onClick
}) => {
  // This is a basic placeholder implementation
  // In the real implementation, this would use @vaadin/react-components
  return (
    <button
      className={`eaf-button eaf-button--${variant} eaf-button--${size}`}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  )
} 