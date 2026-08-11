import React, { useState, useEffect } from 'react';
import { DocumentItem, DocumentSearchResult, CategoryItem, DepartmentItem, UserItem, DocumentMetadataValue, UpdateDocumentPayload } from '../../types';
import { refApi } from '../../api/refApi';
import { documentApi } from '../../api/documentApi';
import { Button } from '../ui/Button';
import { X, Save, Plus, Trash2, AlertCircle, CheckCircle2, Tag, Calendar, User, Building2, FolderKanban } from 'lucide-react';
import { useQueryClient, useMutation } from '@tanstack/react-query';

interface EditDocumentModalProps {
  document: DocumentItem | DocumentSearchResult;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedDoc: DocumentItem) => void;
}

export const EditDocumentModal: React.FC<EditDocumentModalProps> = ({
  document,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const queryClient = useQueryClient();

  const [name, setName] = useState(document.name || '');
  const [description, setDescription] = useState(document.description || '');
  const [categoryId, setCategoryId] = useState(document.categoryId || '');
  const [departmentId, setDepartmentId] = useState(document.departmentId || '');
  const [ownerId, setOwnerId] = useState(document.ownerId || '');
  const [expirationDate, setExpirationDate] = useState(document.expirationDate || '');
  const [tags, setTags] = useState<string[]>(document.tags || []);
  const [newTagInput, setNewTagInput] = useState('');
  const [metadata, setMetadata] = useState<DocumentMetadataValue[]>(document.metadata || []);

  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [departments, setDepartments] = useState<DepartmentItem[]>([]);
  const [users, setUsers] = useState<UserItem[]>([]);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<{ name?: string }>({});

  const updateMutation = useMutation({
    mutationFn: (payload: any) => documentApi.update(document.id, payload),
    onSuccess: async (updated) => {
      setSuccessMessage('✓ Document modifié avec succès');
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });

      setTimeout(() => {
        onSuccess(updated);
        onClose();
      }, 700);
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || err.message || 'Impossible de modifier le document';
      setErrorMessage(msg);
    },
  });

  const isSubmitting = updateMutation.isPending;

  useEffect(() => {
    if (isOpen) {
      setName(document.name || '');
      setDescription(document.description || '');
      setCategoryId(document.categoryId || '');
      setDepartmentId(document.departmentId || '');
      setOwnerId(document.ownerId || '');
      setExpirationDate(document.expirationDate || '');
      setTags(document.tags || []);
      setMetadata(document.metadata || []);
      setErrorMessage(null);
      setSuccessMessage(null);
      setValidationErrors({});

      // Fetch reference data
      refApi.getCategories().then(setCategories).catch(() => {});
      refApi.getDepartments().then(setDepartments).catch(() => {});
      refApi.getUsers().then(setUsers).catch(() => {});
    }
  }, [isOpen, document]);

  if (!isOpen) return null;

  const handleAddTag = () => {
    const trimmed = newTagInput.trim();
    if (!trimmed) return;
    if (tags.some((t) => t.toLowerCase() === trimmed.toLowerCase())) {
      setErrorMessage(`Le tag "${trimmed}" existe déjà.`);
      return;
    }
    setTags([...tags, trimmed]);
    setNewTagInput('');
    setErrorMessage(null);
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove));
  };

  const handleAddMetadataRow = () => {
    setMetadata([...metadata, { key: '', value: '' }]);
  };

  const handleMetadataChange = (index: number, field: 'key' | 'value', val: string) => {
    const updated = [...metadata];
    updated[index][field] = val;
    setMetadata(updated);
  };

  const handleRemoveMetadataRow = (index: number) => {
    setMetadata(metadata.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);
    setValidationErrors({});

    const trimmedName = name.trim();
    if (!trimmedName) {
      setValidationErrors({ name: 'Le nom du document ne peut pas être vide.' });
      return;
    }

    const payload: UpdateDocumentPayload = {
      name: trimmedName,
      description: description.trim() ? description.trim() : undefined,
      categoryId: categoryId || undefined,
      departmentId: departmentId || undefined,
      ownerId: ownerId || undefined,
      expirationDate: expirationDate || undefined,
      tags: tags,
      metadata: metadata.filter((m) => m.key.trim() !== ''),
    };

    updateMutation.mutate(payload);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150">
      <div className="bg-brand-surface border border-brand-border rounded-xl shadow-2xl w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150">
        {/* Accent Bar */}
        <div className="h-1.5 bg-brand-primary w-full shrink-0" />

        {/* Header */}
        <div className="p-4 bg-brand-alt/50 border-b border-brand-border flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-brand-primary/10 rounded-lg text-brand-primary">
              <FolderKanban className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-brand-text">Modifier les propriétés</h3>
              <p className="text-[11px] text-brand-muted truncate max-w-xs">{document.name}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-border/60 rounded-md transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-5 space-y-4 text-xs">
          {/* Notifications */}
          {errorMessage && (
            <div className="flex items-center gap-2.5 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span className="font-medium">{errorMessage}</span>
            </div>
          )}

          {successMessage && (
            <div className="flex items-center gap-2.5 p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span className="font-medium">{successMessage}</span>
            </div>
          )}

          {/* Nom */}
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
              Nom du document <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="ex: Contrat Fournisseur ABC - 2026"
              className={`w-full bg-brand-surface border ${
                validationErrors.name ? 'border-red-500 focus:ring-red-500' : 'border-brand-border focus:border-brand-primary'
              } rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:ring-1`}
              required
            />
            {validationErrors.name && (
              <p className="text-[10px] text-red-500 mt-1 font-medium">{validationErrors.name}</p>
            )}
          </div>

          {/* Description */}
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
              Description
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Contrat de prestation informatique avec le fournisseur..."
              className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary"
            />
          </div>

          {/* Catégorie & Département (2-column layout) */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
                Catégorie
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
              >
                <option value="">-- Sélectionner une catégorie --</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
                Département
              </label>
              <select
                value={departmentId}
                onChange={(e) => setDepartmentId(e.target.value)}
                className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
              >
                <option value="">-- Sélectionner un département --</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Responsable & Date d'expiration (2-column layout) */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1 flex items-center gap-1">
                <User className="w-3 h-3 text-brand-primary" /> Responsable / Propriétaire
              </label>
              <select
                value={ownerId}
                onChange={(e) => setOwnerId(e.target.value)}
                className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
              >
                <option value="">-- Choisir un responsable --</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.firstName && u.lastName ? `${u.firstName} ${u.lastName}` : u.username} ({u.email})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1 flex items-center gap-1">
                <Calendar className="w-3 h-3 text-brand-primary" /> Date d'expiration
              </label>
              <input
                type="date"
                value={expirationDate}
                onChange={(e) => setExpirationDate(e.target.value)}
                className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
              />
            </div>
          </div>

          {/* Tags */}
          <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg space-y-2">
            <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted flex items-center gap-1">
              <Tag className="w-3 h-3 text-brand-primary" /> Étiquettes (Tags)
            </label>
            <div className="flex flex-wrap gap-1.5 mb-2">
              {tags.map((t) => (
                <span
                  key={t}
                  className="inline-flex items-center gap-1 px-2.5 py-1 bg-brand-alt border border-brand-border text-brand-text font-medium text-[11px] rounded-md"
                >
                  #{t}
                  <button
                    type="button"
                    onClick={() => handleRemoveTag(t)}
                    className="text-brand-muted hover:text-red-500 transition-colors ml-0.5"
                  >
                    &times;
                  </button>
                </span>
              ))}
              {tags.length === 0 && (
                <span className="text-brand-muted italic text-[11px]">Aucun tag défini</span>
              )}
            </div>

            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Nouveau tag (ex: IT, renouvellement)..."
                value={newTagInput}
                onChange={(e) => setNewTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddTag();
                  }
                }}
                className="flex-1 bg-brand-surface border border-brand-border rounded-md px-2.5 py-1 text-xs focus:outline-none focus:border-brand-primary"
              />
              <Button type="button" variant="outline" size="sm" icon={<Plus className="w-3 h-3" />} onClick={handleAddTag}>
                Ajouter
              </Button>
            </div>
          </div>

          {/* Métadonnées dynamiques */}
          <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg space-y-2.5">
            <div className="flex items-center justify-between border-b border-brand-border pb-1.5">
              <label className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Métadonnées dynamiques
              </label>
              <Button type="button" variant="outline" size="sm" icon={<Plus className="w-3 h-3" />} onClick={handleAddMetadataRow}>
                Ajouter un champ
              </Button>
            </div>

            {metadata.length === 0 ? (
              <p className="text-[11px] text-brand-muted italic">Aucune métadonnée personnalisée.</p>
            ) : (
              <div className="space-y-2">
                {metadata.map((row, idx) => (
                  <div key={idx} className="flex items-center gap-2">
                    <input
                      type="text"
                      placeholder="Clé (ex: Numéro de contrat)"
                      value={row.key}
                      onChange={(e) => handleMetadataChange(idx, 'key', e.target.value)}
                      className="flex-1 bg-brand-surface border border-brand-border rounded-md px-2.5 py-1 text-xs focus:outline-none focus:border-brand-primary"
                    />
                    <input
                      type="text"
                      placeholder="Valeur (ex: CTR-2026-00521)"
                      value={row.value}
                      onChange={(e) => handleMetadataChange(idx, 'value', e.target.value)}
                      className="flex-1 bg-brand-surface border border-brand-border rounded-md px-2.5 py-1 text-xs focus:outline-none focus:border-brand-primary"
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveMetadataRow(idx)}
                      className="p-1 text-brand-muted hover:text-red-500 rounded-md transition-colors"
                      title="Supprimer cette métadonnée"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer Buttons */}
          <div className="pt-3 border-t border-brand-border flex items-center justify-end gap-2 shrink-0">
            <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={isSubmitting}>
              Annuler
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              icon={<Save className="w-3.5 h-3.5" />}
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Enregistrement...' : 'Enregistrer'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
