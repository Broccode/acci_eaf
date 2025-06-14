import React from 'react';

export interface EafContainerProps {
  children: React.ReactNode;
  size?: 'small' | 'medium' | 'large' | 'full';
  padding?: 'none' | 'small' | 'medium' | 'large';
  className?: string;
  'data-testid'?: string;
}

/**
 * EAF Container component that provides consistent page-level layout.
 *
 * Offers different container sizes and padding options for responsive design.
 */
export const EafContainer: React.FC<EafContainerProps> = ({
  children,
  size = 'large',
  padding = 'medium',
  className = '',
  'data-testid': testId,
  ...props
}) => {
  const getSizeClass = () => {
    switch (size) {
      case 'small':
        return 'max-w-2xl';
      case 'medium':
        return 'max-w-4xl';
      case 'large':
        return 'max-w-6xl';
      case 'full':
        return 'max-w-full';
      default:
        return 'max-w-6xl';
    }
  };

  const getPaddingClass = () => {
    switch (padding) {
      case 'none':
        return '';
      case 'small':
        return 'px-4 py-2';
      case 'large':
        return 'px-8 py-8';
      default:
        return 'px-6 py-4';
    }
  };

  const containerClasses = [
    'mx-auto',
    'w-full',
    getSizeClass(),
    getPaddingClass(),
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={containerClasses} data-testid={testId} {...props}>
      {children}
    </div>
  );
};
