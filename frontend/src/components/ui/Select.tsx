import React, { forwardRef } from 'react';

interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options: SelectOption[];
  error?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, className = '', ...props }, ref) => {
    return (
      <div className="flex flex-col gap-1 w-full text-left">
        {label && <label className="text-xs font-semibold text-brand-muted uppercase tracking-wider">{label}</label>}
        <select
          ref={ref}
          className={`w-full bg-brand-surface border border-brand-border rounded-md px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 transition-all duration-150 cursor-pointer ${
            error ? 'border-red-600 focus:ring-red-600/20' : ''
          } ${className}`}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && <span className="text-[11px] font-medium text-red-600">{error}</span>}
      </div>
    );
  }
);

Select.displayName = 'Select';
