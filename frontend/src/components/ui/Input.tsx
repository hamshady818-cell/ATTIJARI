import React, { forwardRef } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, leftIcon, rightIcon, className = '', ...props }, ref) => {
    return (
      <div className="flex flex-col gap-1 w-full text-left">
        {label && <label className="text-xs font-semibold text-brand-muted uppercase tracking-wider">{label}</label>}
        <div className="relative flex items-center">
          {leftIcon && <div className="absolute left-2.5 text-brand-muted pointer-events-none">{leftIcon}</div>}
          <input
            ref={ref}
            className={`w-full bg-white border border-brand-border rounded-sm px-3 py-1.5 text-xs text-brand-text placeholder:text-brand-muted/60 focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary transition-colors ${
              leftIcon ? 'pl-8' : ''
            } ${rightIcon ? 'pr-8' : ''} ${error ? 'border-red-600 focus:ring-red-600' : ''} ${className}`}
            {...props}
          />
          {rightIcon && <div className="absolute right-2.5 text-brand-muted">{rightIcon}</div>}
        </div>
        {error && <span className="text-[11px] text-red-600">{error}</span>}
      </div>
    );
  }
);

Input.displayName = 'Input';
