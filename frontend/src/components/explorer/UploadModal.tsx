import React, { useState } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Select } from '../ui/Select';
import { CategoryItem, FolderItem } from '../../types';
import { Upload, File, X } from 'lucide-react';
import { documentApi } from '../../api/documentApi';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../../utils/errorMessages';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  folders: FolderItem[];
  categories: CategoryItem[];
  defaultFolderId?: string;
  onSuccess: () => void;
}

export const UploadModal: React.FC<UploadModalProps> = ({
  isOpen,
  onClose,
  folders,
  categories,
  defaultFolderId,
  onSuccess,
}) => {
  const [files, setFiles] = useState<File[]>([]);
  const [targetFolderId, setTargetFolderId] = useState(defaultFolderId || '');
  const [categoryId, setCategoryId] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  React.useEffect(() => {
    if (isOpen) {
      setTargetFolderId(defaultFolderId || '');
    }
  }, [isOpen, defaultFolderId]);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') setDragActive(true);
    else if (e.type === 'dragleave') setDragActive(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setFiles((prev) => [...prev, ...Array.from(e.dataTransfer.files)]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFiles((prev) => [...prev, ...Array.from(e.target.files!)]);
    }
  };

  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (files.length === 0) return;

    try {
      setIsUploading(true);

      if (files.length === 1) {
        // Single Upload
        const file = files[0];
        const formData = new FormData();
        formData.append('file', file);
        formData.append('name', file.name);
        if (targetFolderId) formData.append('folderId', targetFolderId);
        if (categoryId) formData.append('categoryId', categoryId);

        await documentApi.upload(formData);
      } else {
        // Bulk Upload
        const formData = new FormData();
        files.forEach((f) => formData.append('files', f));
        if (targetFolderId) formData.append('folderId', targetFolderId);
        if (categoryId) formData.append('categoryId', categoryId);

        await documentApi.bulkUpload(formData);
      }

      toast.success(
        files.length > 1
          ? `${files.length} documents envoyés avec succès`
          : 'Document envoyé avec succès'
      );
      setFiles([]);
      onSuccess();
      onClose();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de l\'envoi du document.'));
    } finally {
      setIsUploading(false);
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
      { value: '', label: 'Racine (sans dossier)' },
      ...items
        .map((f) => ({ value: f.id, label: getFullPath(f) }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    ];
  };

  const folderOptions = buildFolderOptions(folders);

  const categoryOptions = [
    { value: '', label: 'Aucune catégorie' },
    ...categories.map((c) => ({ value: c.id, label: c.name })),
  ];

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Verser des documents dans la GED" maxWidth="lg">
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        {/* Destination Folder & Category Selection */}
        <div className="grid grid-cols-2 gap-3 bg-brand-alt/50 p-3.5 border border-brand-border rounded-lg">
          <Select
            label="Dossier de destination"
            options={folderOptions}
            value={targetFolderId}
            onChange={(e) => setTargetFolderId(e.target.value)}
          />

          <Select
            label="Catégorie initiale"
            options={categoryOptions}
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          />
        </div>

        {/* Dropzone */}
        <div
          onDragEnter={handleDrag}
          onDragOver={handleDrag}
          onDragLeave={handleDrag}
          onDrop={handleDrop}
          className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-all duration-150 ${
            dragActive
              ? 'border-brand-primary bg-brand-primary-light/50 scale-[1.005]'
              : 'border-brand-border bg-brand-surface hover:bg-brand-alt/40'
          }`}
          onClick={() => document.getElementById('file-upload-input')?.click()}
        >
          <div className="w-10 h-10 bg-brand-primary-light text-brand-primary rounded-full flex items-center justify-center mx-auto mb-2.5">
            <Upload className="w-5 h-5" />
          </div>
          <p className="font-semibold text-brand-text">
            Glissez-déposez vos fichiers ici ou <span className="text-brand-primary underline">parcourez</span>
          </p>
          <p className="text-[11px] text-brand-muted mt-1">
            Formats acceptés : PDF, DOCX, XLSX, JPEG, PNG, TIFF (Taille max: 50 Mo)
          </p>
          <input
            id="file-upload-input"
            type="file"
            multiple
            onChange={handleFileSelect}
            className="hidden"
          />
        </div>

        {/* Selected Files Queue */}
        {files.length > 0 && (
          <div className="border border-brand-border bg-brand-surface rounded-lg p-3.5 space-y-2.5">
            <div className="flex justify-between font-bold text-brand-muted uppercase text-[10px]">
              <span>Fichiers en attente ({files.length})</span>
              <button
                type="button"
                onClick={() => setFiles([])}
                className="text-brand-primary hover:underline"
              >
                Vider la liste
              </button>
            </div>

            <div className="max-h-40 overflow-y-auto space-y-1.5 pr-1">
              {files.map((file, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-2.5 bg-brand-alt/50 border border-brand-border rounded-md text-xs"
                >
                  <div className="flex items-center gap-2 truncate">
                    <File className="w-4 h-4 text-brand-primary shrink-0" />
                    <span className="truncate font-medium">{file.name}</span>
                    <span className="font-mono text-[10px] text-brand-muted">
                      ({(file.size / 1024 / 1024).toFixed(2)} Mo)
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeFile(idx)}
                    className="p-1 text-brand-muted hover:text-brand-primary rounded-md transition-colors"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Footer Actions */}
        <div className="flex justify-end gap-2 pt-3 border-t border-brand-border">
          <Button type="button" variant="outline" onClick={onClose}>
            Annuler
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={isUploading}
            disabled={files.length === 0}
            icon={<Upload className="w-3.5 h-3.5" />}
          >
            Verser {files.length > 0 ? `(${files.length} fichier${files.length > 1 ? 's' : ''})` : ''}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
