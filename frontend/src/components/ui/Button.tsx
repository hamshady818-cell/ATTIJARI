import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  icon?: React.ReactNode;
  loading?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  icon,
  loading,
  disabled,
  className = '',
  ...props
}) => {
  const baseStyle =
    'inline-flex items-center justify-center font-medium rounded-sm border transition-colors focus:outline-none focus:ring-1 focus:ring-brand-primary disabled:opacity-50 disabled:cursor-not-allowed select-none';

  const variants = {
    primary: 'bg-brand-primary text-white border-brand-primary hover:bg-brand-primary-hover active:bg-red-800',
    secondary: 'bg-brand-alt text-brand-text border-brand-border hover:bg-gray-200 active:bg-gray-300',
    outline: 'bg-white text-brand-text border-brand-border hover:bg-brand-bg hover:border-brand-border-dark',
    danger: 'bg-red-700 text-white border-red-700 hover:bg-red-800',
    ghost: 'bg-transparent text-brand-text border-transparent hover:bg-brand-alt',
  };

  const sizes = {
    sm: 'px-2.5 py-1 text-xs gap-1.5 min-h-[28px]',
    md: 'px-3.5 py-1.5 text-xs gap-2 min-h-[34px]',
    lg: 'px-4 py-2 text-sm gap-2 min-h-[40px]',
  };

  return (
    <button
      className={`${baseStyle} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <span className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin mr-1" />
      ) : (
        icon && <span className="shrink-0">{icon}</span>
      )}
      {children}
    </button>
  );
};
