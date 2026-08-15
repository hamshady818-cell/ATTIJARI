import React, { useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Tag, Plus, X } from 'lucide-react';
import { documentApi } from '../../api/documentApi';
import { BulkActionResult } from '../../types';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../../utils/errorMessages';

interface BulkTagModalProps {
  isOpen: boolean;
  onClose: () => void;
  documentIds: string[];
  documentCount: number;
  onSuccess: (result: BulkActionResult) => void;
}

export const BulkTagModal: React.FC<BulkTagModalProps> = ({
  isOpen,
  onClose,
  documentIds,
  documentCount,
  onSuccess,
}) => {
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const handleAddTag = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = tagInput.trim().toLowerCase();
    if (trimmed && !tags.includes(trimmed)) {
      setTags([...tags, trimmed]);
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove));
  };

  const handleSubmit = async () => {
    if (tags.length === 0 || documentIds.length === 0) return;
    setIsLoading(true);
    try {
      const result = await documentApi.bulkTag(documentIds, tags);
      onClose();
      onSuccess(result);
    } catch (err: any) {
      toast.error(extractErrorMessage(err, "Échec de l'ajout des étiquettes"));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Gestion des étiquettes en masse" maxWidth="sm">
      <div className="space-y-4 text-xs">
        {/* Info Header */}
        <div className="p-3.5 bg-brand-primary-light/50 border border-brand-primary/20 rounded-lg flex items-center gap-3 text-brand-text">
          <div className="p-2 bg-brand-primary/10 text-brand-primary rounded-full shrink-0">
            <Tag className="w-4 h-4" />
          </div>
          <div>
            <p className="font-bold text-sm">
              Ajouter des étiquettes à {documentCount} document(s)
            </p>
            <p className="text-[11px] text-brand-muted">
              Saisissez les étiquettes à associer aux documents sélectionnés.
            </p>
          </div>
        </div>

        {/* Tag Input Form */}
        <form onSubmit={handleAddTag} className="flex gap-2">
          <div className="relative flex-1">
            <input
              type="text"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              placeholder="Ex: factures, contrat, 2026..."
              className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary text-brand-text"
            />
          </div>
          <Button type="submit" variant="secondary" icon={<Plus className="w-3.5 h-3.5" />}>
            Ajouter
          </Button>
        </form>

        {/* Tags Badges Display */}
        <div className="min-h-[60px] p-3 bg-brand-alt/40 border border-brand-border rounded-lg flex flex-wrap gap-1.5 items-center">
          {tags.length === 0 ? (
            <span className="text-brand-muted italic text-[11px]">
              Aucune étiquette ajoutée. Saisissez un nom et cliquez sur Ajouter.
            </span>
          ) : (
            tags.map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-mono bg-brand-surface border border-brand-border rounded-full shadow-xs text-brand-text group"
              >
                <Tag className="w-3 h-3 text-brand-primary" />
                <span>{tag}</span>
                <button
                  type="button"
                  onClick={() => handleRemoveTag(tag)}
                  className="hover:text-red-500 text-brand-muted transition-colors"
                >
                  <X className="w-3 h-3" />
                </button>
              </span>
            ))
          )}
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-2.5 pt-3 border-t border-brand-border">
          <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
            Annuler
          </Button>
          <Button
            type="button"
            variant="primary"
            loading={isLoading}
            disabled={tags.length === 0}
            onClick={handleSubmit}
          >
            Appliquer {tags.length} étiquette(s)
          </Button>
        </div>
      </div>
    </Modal>
  );
};
