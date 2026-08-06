import React from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { FolderItem } from '../../types';
import { AlertTriangle, Trash2, FolderOpen } from 'lucide-react';

interface DeleteFolderModalProps {
  isOpen: boolean;
  folder: FolderItem | null;
  /** Number of direct documents inside the folder (from loaded folder content) */
  documentCount: number;
  /** Whether the folder has sub-folders */
  hasSubfolders: boolean;
  isDeleting: boolean;
  onCancel: () => void;
  /** Called with force=true when the user confirms cascade deletion */
  onConfirm: (force: boolean) => void;
}

export const DeleteFolderModal: React.FC<DeleteFolderModalProps> = ({
  isOpen,
  folder,
  documentCount,
  hasSubfolders,
  isDeleting,
  onCancel,
  onConfirm,
}) => {
  if (!folder) return null;

  const isEmpty = documentCount === 0 && !hasSubfolders;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onCancel}
      title="Supprimer le dossier"
      maxWidth="sm"
    >
      <div className="space-y-4 text-xs">
        {/* Folder name badge */}
        <div className="flex items-center gap-2.5 p-3.5 bg-brand-alt/50 border border-brand-border rounded-lg shadow-xs">
          <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
          <span className="font-bold text-brand-text truncate">{folder.name}</span>
        </div>

        {isEmpty ? (
          /* Empty folder — simple confirmation */
          <div className="flex items-start gap-3 p-3.5 bg-amber-50 border border-amber-200 rounded-lg">
            <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <p className="text-brand-text leading-relaxed">
              Ce dossier est vide. Il sera déplacé vers la{' '}
              <span className="font-semibold text-amber-900">corbeille</span> et pourra être restauré
              ultérieurement.
            </p>
          </div>
        ) : (
          /* Non-empty folder — warn about cascade */
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3.5 bg-red-50 border border-red-200 rounded-lg">
              <AlertTriangle className="w-4 h-4 text-brand-primary shrink-0 mt-0.5" />
              <div className="space-y-1.5 leading-relaxed text-brand-text">
                <p className="font-semibold text-brand-primary">
                  Ce dossier contient du contenu :
                </p>
                <ul className="list-disc list-inside space-y-0.5 text-brand-text">
                  {hasSubfolders && (
                    <li>Des <span className="font-medium">sous-dossiers</span> (et leur contenu)</li>
                  )}
                  {documentCount > 0 && (
                    <li>
                      <span className="font-medium">{documentCount}</span>{' '}
                      document{documentCount > 1 ? 's' : ''}
                    </li>
                  )}
                </ul>
                <p className="mt-2 text-brand-muted">
                  Tout le contenu sera déplacé vers la{' '}
                  <span className="font-semibold text-brand-text">corbeille</span> et
                  pourra être restauré ultérieurement.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Action buttons */}
        <div className="flex justify-end gap-2 pt-3 border-t border-brand-border">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isDeleting}
          >
            Annuler
          </Button>

          {isEmpty ? (
            <Button
              type="button"
              variant="danger"
              loading={isDeleting}
              icon={<Trash2 className="w-3.5 h-3.5" />}
              onClick={() => onConfirm(false)}
            >
              Supprimer le dossier
            </Button>
          ) : (
            <Button
              type="button"
              variant="danger"
              loading={isDeleting}
              icon={<Trash2 className="w-3.5 h-3.5" />}
              onClick={() => onConfirm(true)}
            >
              Supprimer le dossier et tout son contenu
            </Button>
          )}
        </div>
      </div>
    </Modal>
  );
};
