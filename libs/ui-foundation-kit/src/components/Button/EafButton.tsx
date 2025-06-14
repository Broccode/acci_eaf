import {
  Button as VaadinButton,
  type ButtonProps as VaadinButtonProps,
} from '@vaadin/react-components/Button.js';
import React from 'react';

export interface EafButtonProps extends Omit<VaadinButtonProps, 'theme'> {
  children: React.ReactNode;
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'small' | 'medium' | 'large';
}

/**
 * EAF Button component that wraps the Vaadin Button.
 *
 * It maps its `variant` and `size` props to Vaadin's theme attributes to provide a consistent API
 * while leveraging Vaadin's component system.
 *
 * Styling with utility classes like Tailwind is challenging due to Vaadin's use of Shadow DOM.
 * This approach is a compromise to adhere to the technical guidelines.
 */
export const EafButton: React.FC<EafButtonProps> = ({
  children,
  variant = 'primary',
  size = 'medium',
  ...props
}) => {
  const themes: string[] = [];
  if (variant === 'primary') {
    // Vaadin's "primary" theme
    themes.push('primary');
  } else if (variant === 'danger') {
    // Vaadin's "error" theme for a danger/destructive action
    themes.push('error');
  }
  // The "secondary" variant will use the default Vaadin button appearance.

  if (size === 'small') {
    themes.push('small');
  } else if (size === 'large') {
    themes.push('large');
  }
  // The "medium" size is the default Vaadin button size.

  return (
    <VaadinButton theme={themes.join(' ')} {...props}>
      {children}
    </VaadinButton>
  );
};
