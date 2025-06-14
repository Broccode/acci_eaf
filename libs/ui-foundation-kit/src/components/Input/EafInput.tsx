import {
  TextField as VaadinTextField,
  type TextFieldProps as VaadinTextFieldProps,
} from '@vaadin/react-components/TextField.js';
import React from 'react';

export interface EafInputProps extends Omit<VaadinTextFieldProps, 'theme'> {
  label?: string;
  placeholder?: string;
  helperText?: string;
  errorMessage?: string;
  variant?: 'outlined' | 'filled';
  size?: 'small' | 'medium' | 'large';
  required?: boolean;
  disabled?: boolean;
  readonly?: boolean;
}

/**
 * EAF Input component that wraps the Vaadin TextField.
 *
 * It maps its `variant` and `size` props to Vaadin's theme attributes to provide a consistent API
 * while leveraging Vaadin's component system.
 *
 * Styling with utility classes like Tailwind is challenging due to Vaadin's use of Shadow DOM.
 * This approach is a compromise to adhere to the technical guidelines.
 */
export const EafInput: React.FC<EafInputProps> = ({
  label,
  placeholder,
  helperText,
  errorMessage,
  variant = 'outlined',
  size = 'medium',
  required = false,
  disabled = false,
  readonly = false,
  ...props
}) => {
  const themes: string[] = [];

  if (variant === 'filled') {
    themes.push('filled');
  }
  // The "outlined" variant uses the default Vaadin appearance.

  if (size === 'small') {
    themes.push('small');
  } else if (size === 'large') {
    themes.push('large');
  }
  // The "medium" size is the default Vaadin size.

  return (
    <VaadinTextField
      theme={themes.join(' ')}
      label={label}
      placeholder={placeholder}
      helperText={helperText}
      errorMessage={errorMessage}
      required={required}
      disabled={disabled}
      readonly={readonly}
      {...props}
    />
  );
};
