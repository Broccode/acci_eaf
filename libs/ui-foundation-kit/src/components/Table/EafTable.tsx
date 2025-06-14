import React from 'react';

export interface EafTableColumn {
  key: string;
  title: string;
  width?: string;
  align?: 'left' | 'center' | 'right';
}

export interface EafTableProps {
  columns: EafTableColumn[];
  data: Record<string, any>[];
  className?: string;
  striped?: boolean;
  bordered?: boolean;
  hover?: boolean;
}

/**
 * EAF Table component that provides a flexible table with customizable styling.
 *
 * Uses Tailwind CSS classes for responsive design and consistent styling.
 */
export const EafTable: React.FC<EafTableProps> = ({
  columns,
  data,
  className = '',
  striped = false,
  bordered = false,
  hover = false,
}) => {
  const getTableClasses = () => {
    const classes = ['w-full', 'table-auto'];

    if (bordered) {
      classes.push('border', 'border-gray-300');
    }

    return classes.join(' ');
  };

  const getRowClasses = (index: number) => {
    const classes: string[] = [];

    if (striped && index % 2 === 1) {
      classes.push('bg-gray-50');
    }

    if (hover) {
      classes.push('hover:bg-gray-100');
    }

    if (bordered) {
      classes.push('border-b', 'border-gray-200');
    }

    return classes.join(' ');
  };

  const getCellClasses = (align?: string) => {
    const classes = ['px-4', 'py-2'];

    if (align === 'center') {
      classes.push('text-center');
    } else if (align === 'right') {
      classes.push('text-right');
    } else {
      classes.push('text-left');
    }

    if (bordered) {
      classes.push('border-r', 'border-gray-200', 'last:border-r-0');
    }

    return classes.join(' ');
  };

  const getHeaderClasses = (align?: string) => {
    const classes = [
      'px-4',
      'py-3',
      'bg-gray-100',
      'font-semibold',
      'text-gray-900',
    ];

    if (align === 'center') {
      classes.push('text-center');
    } else if (align === 'right') {
      classes.push('text-right');
    } else {
      classes.push('text-left');
    }

    if (bordered) {
      classes.push('border-r', 'border-gray-200', 'last:border-r-0');
    }

    return classes.join(' ');
  };

  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className={getTableClasses()}>
        <thead>
          <tr className={bordered ? 'border-b border-gray-300' : ''}>
            {columns.map(column => (
              <th
                key={column.key}
                className={getHeaderClasses(column.align)}
                style={column.width ? { width: column.width } : undefined}
              >
                {column.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, index) => (
            <tr key={index} className={getRowClasses(index)}>
              {columns.map(column => (
                <td key={column.key} className={getCellClasses(column.align)}>
                  {row[column.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
