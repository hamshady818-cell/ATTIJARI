import React, { useEffect } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  maxWidth = 'md',
}) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) onClose();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const widthClasses = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-none">
      <div
        className={`w-full ${widthClasses[maxWidth]} bg-brand-surface border border-brand-border rounded-sm shadow-popover overflow-hidden flex flex-col max-h-[90vh] animate-in fade-in zoom-in-95 duration-100`}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 bg-brand-alt border-b border-brand-border">
          <h3 className="text-xs font-bold uppercase tracking-wider text-brand-text">{title}</h3>
          <button
            onClick={onClose}
            className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-border rounded-sm transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 overflow-y-auto">{children}</div>
      </div>
    </div>
  );
};
