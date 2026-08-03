import React, { forwardRef } from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, helperText, className = '', id, ...props }, ref) => {
    const inputId = id || Math.random().toString(36).substring(2, 9);

    return (
      <div className="flex flex-col gap-1 w-full text-left">
        {label && (
          <label htmlFor={inputId} className="text-xs font-semibold text-gray-700">
            {label}
          </label>
        )}
        <input
          id={inputId}
          ref={ref}
          className={`px-3 py-2 border rounded text-sm text-gray-800 bg-white placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-brand focus:border-brand disabled:bg-gray-50 disabled:text-gray-400 disabled:cursor-not-allowed ${
            error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : 'border-gray-300'
          } ${className}`}
          {...props}
        />
        {error && <span className="text-xs text-red-600 font-medium">{error}</span>}
        {!error && helperText && <span className="text-xs text-gray-400">{helperText}</span>}
      </div>
    );
  }
);

Input.displayName = 'Input';
