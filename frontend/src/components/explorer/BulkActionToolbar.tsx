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
    <div className="flex items-center justify-between px-4 py-2.5 bg-brand-text text-white border border-brand-text rounded-lg shadow-popover mb-4 animate-in slide-in-from-top-2 duration-150">
      <div className="flex items-center gap-3">
        <span className="font-mono text-xs font-bold bg-brand-primary text-white px-2.5 py-0.5 rounded-md">
          {selectedCount} sélectionné{selectedCount > 1 ? 's' : ''}
        </span>
        <button
          onClick={onClearSelection}
          className="text-xs text-brand-border hover:text-white flex items-center gap-1 transition-colors"
        >
          <X className="w-3.5 h-3.5" />
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
