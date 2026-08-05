import React from 'react';
import { Button } from '../ui/Button';
import { Trash2, FolderInput, Tag, X } from 'lucide-react';

interface BulkActionToolbarProps {
  selectedCount: number;
  onClearSelection: () => void;
  onBulkDelete: () => void;
  onBulkMove: () => void;
  onBulkTag: () => void;
}

export const BulkActionToolbar: React.FC<BulkActionToolbarProps> = ({
  selectedCount,
  onClearSelection,
  onBulkDelete,
  onBulkMove,
  onBulkTag,
}) => {
  if (selectedCount === 0) return null;

  return (
    <div className="flex items-center justify-between px-4 py-2 bg-brand-text text-white border border-brand-text mb-3 animate-in slide-in-from-top-2 duration-150">
      <div className="flex items-center gap-3">
        <span className="font-mono text-xs font-bold bg-brand-primary px-2 py-0.5">
          {selectedCount} sélectionné{selectedCount > 1 ? 's' : ''}
        </span>
        <button
          onClick={onClearSelection}
          className="text-xs text-gray-300 hover:text-white flex items-center gap-1 underline"
        >
          <X className="w-3 h-3" />
          Désélectionner
        </button>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="secondary" size="sm" icon={<FolderInput className="w-3.5 h-3.5" />} onClick={onBulkMove}>
          Déplacer
        </Button>

        <Button variant="secondary" size="sm" icon={<Tag className="w-3.5 h-3.5" />} onClick={onBulkTag}>
          Étiqueter (Tags)
        </Button>

        <Button variant="danger" size="sm" icon={<Trash2 className="w-3.5 h-3.5" />} onClick={onBulkDelete}>
          Supprimer ({selectedCount})
        </Button>
      </div>
    </div>
  );
};
