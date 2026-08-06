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
    'inline-flex items-center justify-center font-medium rounded-md border transition-all duration-150 focus:outline-none focus:ring-2 focus:ring-brand-primary/30 focus:border-brand-primary disabled:opacity-50 disabled:cursor-not-allowed select-none shadow-xs';

  const variants = {
    primary: 'bg-brand-primary text-white border-brand-primary hover:bg-brand-primary-hover active:bg-brand-primary-hover shadow-xs',
    secondary: 'bg-brand-alt text-brand-text border-brand-border hover:bg-brand-border active:bg-brand-border-dark',
    outline: 'bg-brand-surface text-brand-text border-brand-border hover:bg-brand-bg hover:border-brand-border-dark',
    danger: 'bg-brand-primary text-white border-brand-primary hover:bg-brand-primary-hover shadow-xs',
    ghost: 'bg-transparent text-brand-text border-transparent hover:bg-brand-alt shadow-none',
  };

  const sizes = {
    sm: 'px-3 py-1 text-xs gap-1.5 min-h-[30px]',
    md: 'px-4 py-1.5 text-xs gap-2 min-h-[36px]',
    lg: 'px-5 py-2 text-sm gap-2 min-h-[42px]',
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
