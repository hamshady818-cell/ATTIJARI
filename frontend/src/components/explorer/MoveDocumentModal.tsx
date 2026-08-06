import React, { useState } from 'react';
import { FolderItem } from '../../types';
import { documentApi } from '../../api/documentApi';
import { useQueryClient } from '@tanstack/react-query';
import { Button } from '../ui/Button';
import {
  X,
  FolderInput,
  Folder,
  FolderOpen,
  ChevronRight,
  ChevronDown,
  HardDrive,
  CheckCircle,
  Loader2,
} from 'lucide-react';

interface MoveDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  documentIds: string[];
  documentNames?: string[];
  folders: FolderItem[];
  currentFolderId?: string;
  onSuccess: () => void;
}

export const MoveDocumentModal: React.FC<MoveDocumentModalProps> = ({
  isOpen,
  onClose,
  documentIds,
  documentNames = [],
  folders,
  currentFolderId,
  onSuccess,
}) => {
  const [selectedFolderId, setSelectedFolderId] = useState<string | undefined>(currentFolderId);
  const [isRootSelected, setIsRootSelected] = useState<boolean>(!currentFolderId);
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  React.useEffect(() => {
    if (isOpen) {
      setSelectedFolderId(currentFolderId);
      setIsRootSelected(!currentFolderId);
    }
  }, [isOpen, currentFolderId]);

  if (!isOpen) return null;

  const toggleExpand = (folderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedFolders((prev) => ({ ...prev, [folderId]: !prev[folderId] }));
  };

  const selectFolder = (folderId?: string) => {
    if (folderId === undefined) {
      setIsRootSelected(true);
      setSelectedFolderId(undefined);
    } else {
      setIsRootSelected(false);
      setSelectedFolderId(folderId);
    }
  };

  // Build tree hierarchy
  const rootFolders = folders.filter((f) => !f.parentId);
  const getSubfolders = (parentId: string) => folders.filter((f) => f.parentId === parentId);

  const handleConfirmMove = async () => {
    if (documentIds.length === 0) return;
    setError(null);
    setIsSubmitting(true);
    try {
      await documentApi.bulkMove(documentIds, isRootSelected ? undefined : selectedFolderId, isRootSelected);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Erreur lors du déplacement');
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderFolderNode = (folder: FolderItem, depth = 0) => {
    const isExpanded = expandedFolders[folder.id];
    const isSelected = !isRootSelected && selectedFolderId === folder.id;
    const subfolders = getSubfolders(folder.id);
    const hasChildren = subfolders.length > 0;

    return (
      <div key={folder.id} className="select-none">
        <div
          onClick={() => selectFolder(folder.id)}
          style={{ paddingLeft: `${depth * 16 + 12}px` }}
          className={`flex items-center justify-between py-1.5 pr-2.5 mx-1 my-0.5 text-xs font-medium cursor-pointer rounded-md transition-all duration-150 ${
            isSelected
              ? 'bg-brand-primary-light text-brand-primary font-bold border-l-3 border-brand-primary'
              : 'text-brand-text hover:bg-brand-alt'
          }`}
        >
          <div className="flex items-center gap-1.5 truncate">
            {hasChildren ? (
              <button
                onClick={(e) => toggleExpand(folder.id, e)}
                className="p-0.5 text-brand-muted hover:text-brand-text"
              >
                {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
              </button>
            ) : (
              <span className="w-4" />
            )}

            {isSelected ? (
              <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
            ) : (
              <Folder className="w-4 h-4 text-brand-muted shrink-0" />
            )}

            <span className="truncate">{folder.name}</span>
          </div>

          {isSelected && <CheckCircle className="w-3.5 h-3.5 text-brand-primary shrink-0" />}
        </div>

        {hasChildren && isExpanded && (
          <div>{subfolders.map((child) => renderFolderNode(child, depth + 1))}</div>
        )}
      </div>
    );
  };

  const titleText =
    documentNames.length === 1
      ? `Déplacer "${documentNames[0]}"`
      : documentNames.length > 1
      ? `Déplacer ${documentNames.length} documents`
      : `Déplacer ${documentIds.length} document(s)`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-brand-surface border border-brand-border shadow-modal rounded-lg overflow-hidden w-full max-w-md flex flex-col max-h-[85vh] animate-in zoom-in-95 duration-150">
        {/* Top Accent Bar */}
        <div className="h-1 bg-brand-primary w-full shrink-0" />

        {/* Modal Header */}
        <div className="p-3.5 bg-brand-alt/50 border-b border-brand-border flex items-center justify-between">
          <div className="flex items-center gap-2 min-w-0">
            <FolderInput className="w-4 h-4 text-brand-primary shrink-0" />
            <h3 className="font-bold text-xs uppercase tracking-wider text-brand-text truncate">
              {titleText}
            </h3>
          </div>
          <button
            onClick={onClose}
            className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-border/60 rounded-md transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Modal Body: Folder Picker */}
        <div className="p-4 flex-1 overflow-y-auto space-y-3">
          <p className="text-xs text-brand-muted font-medium">
            Choisissez le dossier de destination pour le déplacement :
          </p>

          {/* Root Choice */}
          <div
            onClick={() => selectFolder(undefined)}
            className={`flex items-center justify-between p-3 text-xs font-medium cursor-pointer border rounded-lg transition-all duration-150 ${
              isRootSelected
                ? 'bg-brand-primary-light border-brand-primary text-brand-primary font-bold'
                : 'bg-brand-surface border-brand-border text-brand-text hover:bg-brand-alt'
            }`}
          >
            <div className="flex items-center gap-2">
              <HardDrive className="w-4 h-4 text-brand-muted shrink-0" />
              <span>Racine (sans dossier parent)</span>
            </div>
            {isRootSelected && <CheckCircle className="w-3.5 h-3.5 text-brand-primary shrink-0" />}
          </div>

          {/* Folder Tree List */}
          <div className="border border-brand-border bg-brand-surface rounded-lg divide-y divide-brand-border max-h-60 overflow-y-auto">
            <div className="px-3 py-1.5 bg-brand-alt/50 text-[10px] font-bold uppercase tracking-wider text-brand-muted">
              Dossiers existants ({folders.length})
            </div>
            {rootFolders.length === 0 ? (
              <div className="p-4 text-center text-xs text-brand-muted italic">
                Aucun sous-dossier disponible.
              </div>
            ) : (
              rootFolders.map((folder) => renderFolderNode(folder, 0))
            )}
          </div>

          {/* Error Banner */}
          {error && (
            <div className="p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-md font-medium">
              {error}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-3 bg-brand-alt/50 border-t border-brand-border flex items-center justify-end gap-2">
          <Button variant="outline" size="sm" onClick={onClose} disabled={isSubmitting}>
            Annuler
          </Button>
          <Button
            variant="primary"
            size="sm"
            icon={isSubmitting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <FolderInput className="w-3.5 h-3.5" />}
            onClick={handleConfirmMove}
            loading={isSubmitting}
          >
            Déplacer ici
          </Button>
        </div>
      </div>
    </div>
  );
};
