/**
 * EAF Theme Configuration
 * This is a placeholder for the EAF theme system
 */

export interface EafTheme {
  name: string;
  colors: {
    primary: string;
    secondary: string;
    accent: string;
    background: string;
    surface: string;
    text: string;
  };
  spacing: {
    xs: string;
    sm: string;
    md: string;
    lg: string;
    xl: string;
  };
}

export const eafTheme: EafTheme = {
  name: 'eaf-default',
  colors: {
    primary: '#0066cc',
    secondary: '#666666',
    accent: '#ff6b35',
    background: '#ffffff',
    surface: '#f5f5f5',
    text: '#333333',
  },
  spacing: {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
  },
};
