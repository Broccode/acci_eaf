import React from 'react';

export interface EafGridProps {
  children: React.ReactNode;
  columns?: number | 'auto' | 'fit';
  gap?: 'none' | 'small' | 'medium' | 'large';
  className?: string;
  'data-testid'?: string;
}

/**
 * EAF Grid component that provides a flexible grid layout system.
 *
 * Uses CSS Grid with Tailwind utility classes for responsive layouts.
 */
export const EafGrid: React.FC<EafGridProps> = ({
  children,
  columns = 'auto',
  gap = 'medium',
  className = '',
  'data-testid': testId,
  ...props
}) => {
  const getGridColumns = () => {
    if (typeof columns === 'number') {
      return `grid-cols-${columns}`;
    }
    if (columns === 'fit') {
      return 'grid-cols-[repeat(auto-fit,minmax(200px,1fr))]';
    }
    return 'grid-cols-[repeat(auto-fill,minmax(200px,1fr))]';
  };

  const getGapClass = () => {
    switch (gap) {
      case 'none':
        return 'gap-0';
      case 'small':
        return 'gap-2';
      case 'large':
        return 'gap-8';
      default:
        return 'gap-4';
    }
  };

  const gridClasses = ['grid', getGridColumns(), getGapClass(), className]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={gridClasses} data-testid={testId} {...props}>
      {children}
    </div>
  );
};
