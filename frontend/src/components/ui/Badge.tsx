import React from 'react';
import { DocumentStatus } from '../../types';

interface BadgeProps {
  status?: DocumentStatus | string;
  children?: React.ReactNode;
  variant?: 'default' | 'draft' | 'published' | 'archived' | 'trashed' | 'locked' | 'tag';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ status, children, variant, className = '' }) => {
  let activeVariant = variant;
  let text = children || status;

  if (status && !variant) {
    switch (status) {
      case 'DRAFT':
        activeVariant = 'draft';
        text = text || 'BROUILLON';
        break;
      case 'PUBLISHED':
        activeVariant = 'published';
        text = text || 'PUBLIÉ';
        break;
      case 'ARCHIVED':
        activeVariant = 'archived';
        text = text || 'ARCHIVÉ';
        break;
      case 'TRASHED':
        activeVariant = 'trashed';
        text = text || 'CORBEILLE';
        break;
      default:
        activeVariant = 'default';
    }
  }

  const styles = {
    default: 'bg-brand-alt text-brand-muted border-brand-border',
    draft: 'bg-gray-100 text-gray-700 border-gray-200 font-semibold',
    published: 'bg-emerald-50 text-emerald-800 border-emerald-200 font-semibold',
    archived: 'bg-amber-50 text-amber-800 border-amber-200 font-semibold',
    trashed: 'bg-red-50 text-red-700 border-red-200 font-semibold',
    locked: 'bg-red-100 text-red-800 border-red-300 font-bold',
    tag: 'bg-brand-alt text-brand-text border-brand-border hover:border-brand-secondary transition-colors',
  };

  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 text-[10px] uppercase font-mono tracking-wider border rounded-md ${
        styles[activeVariant || 'default']
      } ${className}`}
    >
      {text}
    </span>
  );
};
