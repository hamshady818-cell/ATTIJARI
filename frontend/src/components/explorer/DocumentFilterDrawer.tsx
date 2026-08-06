import React from 'react';
import { Select } from '../ui/Select';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { CategoryItem, SearchFilterParams } from '../../types';
import { Filter, RotateCcw } from 'lucide-react';

interface DocumentFilterDrawerProps {
  filters: SearchFilterParams;
  categories: CategoryItem[];
  onChange: (filters: SearchFilterParams) => void;
  onReset: () => void;
}

export const DocumentFilterDrawer: React.FC<DocumentFilterDrawerProps> = ({
  filters,
  categories,
  onChange,
  onReset,
}) => {
  const statusOptions = [
    { value: '', label: 'Tous les statuts' },
    { value: 'DRAFT', label: 'Brouillon (DRAFT)' },
    { value: 'PUBLISHED', label: 'Publié (PUBLISHED)' },
    { value: 'ARCHIVED', label: 'Archivé (ARCHIVED)' },
    { value: 'TRASHED', label: 'Corbeille (TRASHED)' },
  ];

  const categoryOptions = [
    { value: '', label: 'Toutes les catégories' },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  return (
    <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-4 mb-4 text-xs">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 font-bold uppercase tracking-wider text-brand-muted text-[11px]">
          <Filter className="w-4 h-4 text-brand-primary" />
          <span>Filtres de recherche avancée</span>
        </div>
        <Button variant="ghost" size="sm" icon={<RotateCcw className="w-3.5 h-3.5" />} onClick={onReset}>
          Réinitialiser
        </Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3.5">
        {/* Mot-clé */}
        <Input
          label="Mot-clé"
          placeholder="Ex: Contrat, Facture 2024..."
          value={filters.keyword || ''}
          onChange={(e) => onChange({ ...filters, keyword: e.target.value, page: 0 })}
        />

        {/* Statut */}
        <Select
          label="Statut du document"
          options={statusOptions}
          value={filters.status || ''}
          onChange={(e) => onChange({ ...filters, status: e.target.value, page: 0 })}
        />

        {/* Catégorie */}
        <Select
          label="Catégorie"
          options={categoryOptions}
          value={filters.categoryId || ''}
          onChange={(e) => onChange({ ...filters, categoryId: e.target.value, page: 0 })}
        />

        {/* Tag */}
        <Input
          label="Tag / Étiquette"
          placeholder="Ex: urgent, finance..."
          value={filters.tagName || ''}
          onChange={(e) => onChange({ ...filters, tagName: e.target.value, page: 0 })}
        />
      </div>
    </div>
  );
};
