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
    default: 'bg-gray-100 text-gray-700 border-gray-300',
    draft: 'bg-gray-100 text-gray-800 border-gray-400 font-semibold',
    published: 'bg-emerald-50 text-emerald-800 border-emerald-300 font-semibold',
    archived: 'bg-amber-50 text-amber-900 border-amber-300 font-semibold',
    trashed: 'bg-red-50 text-red-800 border-red-300 font-semibold',
    locked: 'bg-red-100 text-red-900 border-red-400 font-bold',
    tag: 'bg-brand-alt text-brand-text border-brand-border',
  };

  return (
    <span
      className={`inline-flex items-center px-1.5 py-0.5 text-[10px] uppercase font-mono tracking-wider border rounded-none ${
        styles[activeVariant || 'default']
      } ${className}`}
    >
      {text}
    </span>
  );
};
