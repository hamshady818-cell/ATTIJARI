import React, { useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { FolderItem } from '../../types';
import { FolderPlus } from 'lucide-react';
import { folderApi } from '../../api/folderApi';

interface CreateFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  folders: FolderItem[];
  defaultParentId?: string;
  onSuccess: () => void;
}

export const CreateFolderModal: React.FC<CreateFolderModalProps> = ({
  isOpen,
  onClose,
  folders,
  defaultParentId,
  onSuccess,
}) => {
  const [folderName, setFolderName] = useState('');
  const [parentFolderId, setParentFolderId] = useState(defaultParentId || '');
  const [isCreating, setIsCreating] = useState(false);

  React.useEffect(() => {
    if (isOpen) {
      setParentFolderId(defaultParentId || '');
      setFolderName('');
    }
  }, [isOpen, defaultParentId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!folderName.trim()) return;

    try {
      setIsCreating(true);
      await folderApi.createFolder(folderName.trim(), parentFolderId || undefined);
      setFolderName('');
      onSuccess();
      onClose();
    } catch (err: any) {
      alert('Erreur de création du dossier: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsCreating(false);
    }
  };

  const buildFolderOptions = (items: FolderItem[]) => {
    const map = new Map<string, FolderItem>();
    items.forEach((f) => map.set(f.id, f));

    const getFullPath = (f: FolderItem): string => {
      const parts = [f.name];
      let curr = f;
      while (curr.parentId && map.has(curr.parentId)) {
        curr = map.get(curr.parentId)!;
        parts.unshift(curr.name);
      }
      return parts.join(' / ');
    };

    return [
      { value: '', label: 'Racine (Aucun dossier parent)' },
      ...items
        .map((f) => ({ value: f.id, label: getFullPath(f) }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    ];
  };

  const folderOptions = buildFolderOptions(folders);

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Créer un nouveau répertoire" maxWidth="sm">
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        <Input
          label="Nom du répertoire"
          placeholder="Ex: Direction Financière, Contrats 2024..."
          value={folderName}
          onChange={(e) => setFolderName(e.target.value)}
          required
        />

        <Select
          label="Emplacement parent"
          options={folderOptions}
          value={parentFolderId}
          onChange={(e) => setParentFolderId(e.target.value)}
        />

        <div className="flex justify-end gap-2 pt-2 border-t border-brand-border">
          <Button type="button" variant="outline" onClick={onClose}>
            Annuler
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={isCreating}
            disabled={!folderName.trim()}
            icon={<FolderPlus className="w-3.5 h-3.5" />}
          >
            Créer le répertoire
          </Button>
        </div>
      </form>
    </Modal>
  );
};
