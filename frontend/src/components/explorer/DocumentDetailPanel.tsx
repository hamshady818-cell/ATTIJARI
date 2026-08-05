import React, { useState } from 'react';
import { DocumentItem, DocumentSearchResult, DocumentVersion } from '../../types';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { documentApi } from '../../api/documentApi';
import {
  X,
  FileText,
  Lock,
  Download,
  Eye,
  History,
  Info,
  Calendar,
  User,
  Shield,
  Upload,
  CheckCircle,
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';

interface DocumentDetailPanelProps {
  document: DocumentItem | DocumentSearchResult | null;
  onClose: () => void;
  onRefresh: () => void;
}

export const DocumentDetailPanel: React.FC<DocumentDetailPanelProps> = ({
  document,
  onClose,
  onRefresh,
}) => {
  const [activeTab, setActiveTab] = useState<'info' | 'versions' | 'status'>('info');
  const [newVersionFile, setNewVersionFile] = useState<File | null>(null);
  const [changeSummary, setChangeSummary] = useState('');
  const [isUploadingVersion, setIsUploadingVersion] = useState(false);

  // Query Version history using TanStack Query
  const { data: versions = [], refetch: refetchVersions } = useQuery<DocumentVersion[]>({
    queryKey: ['document-versions', document?.id],
    queryFn: () => documentApi.listVersions(document!.id),
    enabled: !!document?.id,
  });

  if (!document) return null;

  const handleUploadVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newVersionFile) return;

    try {
      setIsUploadingVersion(true);
      const formData = new FormData();
      formData.append('file', newVersionFile);
      if (changeSummary) formData.append('changeSummary', changeSummary);

      await documentApi.uploadVersion(document.id, formData);
      setNewVersionFile(null);
      setChangeSummary('');
      refetchVersions();
      onRefresh();
    } catch (err: any) {
      alert('Erreur lors du versement de la version: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsUploadingVersion(false);
    }
  };

  const handleChangeStatus = async (newStatus: any) => {
    try {
      await documentApi.updateStatus(document.id, newStatus);
      onRefresh();
    } catch (err: any) {
      alert('Erreur de changement de statut: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleToggleCheckout = async () => {
    try {
      if (document.isLocked) {
        await documentApi.checkin(document.id);
      } else {
        await documentApi.checkout(document.id);
      }
      onRefresh();
    } catch (err: any) {
      alert('Erreur de verrouillage: ' + (err.response?.data?.message || err.message));
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('fr-FR');
  };

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-md bg-brand-surface border-l border-brand-border shadow-popover flex flex-col animate-in slide-in-from-right duration-200">
      {/* Panel Header */}
      <div className="p-4 bg-brand-alt border-b border-brand-border flex items-center justify-between">
        <div className="flex items-center gap-2 truncate">
          <FileText className="w-4 h-4 text-brand-primary shrink-0" />
          <h3 className="font-bold text-xs uppercase tracking-wider text-brand-text truncate">
            {document.name}
          </h3>
        </div>
        <button
          onClick={onClose}
          className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-border rounded-sm"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-brand-border bg-brand-bg text-xs font-medium">
        <button
          onClick={() => setActiveTab('info')}
          className={`flex-1 py-2 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
            activeTab === 'info'
              ? 'border-brand-primary text-brand-primary bg-white font-bold'
              : 'border-transparent text-brand-muted hover:text-brand-text'
          }`}
        >
          <Info className="w-3.5 h-3.5" />
          Fiche Métadonnées
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`flex-1 py-2 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
            activeTab === 'versions'
              ? 'border-brand-primary text-brand-primary bg-white font-bold'
              : 'border-transparent text-brand-muted hover:text-brand-text'
          }`}
        >
          <History className="w-3.5 h-3.5" />
          Historique ({versions.length})
        </button>

        <button
          onClick={() => setActiveTab('status')}
          className={`flex-1 py-2 px-3 flex items-center justify-center gap-1.5 border-b-2 transition-colors ${
            activeTab === 'status'
              ? 'border-brand-primary text-brand-primary bg-white font-bold'
              : 'border-transparent text-brand-muted hover:text-brand-text'
          }`}
        >
          <Shield className="w-3.5 h-3.5" />
          Statut & Verrou
        </button>
      </div>

      {/* Content Body */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
        {activeTab === 'info' && (
          <div className="space-y-4">
            {/* Quick Actions Bar */}
            <div className="flex items-center gap-2 p-2 bg-brand-alt border border-brand-border">
              <a
                href={documentApi.previewUrl(document.id)}
                target="_blank"
                rel="noreferrer"
                className="flex-1"
              >
                <Button variant="outline" size="sm" icon={<Eye className="w-3.5 h-3.5" />} className="w-full">
                  Aperçu
                </Button>
              </a>

              <a href={documentApi.downloadUrl(document.id)} download className="flex-1">
                <Button variant="primary" size="sm" icon={<Download className="w-3.5 h-3.5" />} className="w-full">
                  Télécharger
                </Button>
              </a>
            </div>

            {/* General Properties */}
            <div className="border border-brand-border p-3 space-y-2 bg-white">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1">
                Propriétés Générales
              </h4>

              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Statut</span>
                  <Badge status={document.status} />
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Format / MIME</span>
                  <span className="font-mono text-brand-text">{document.mimeType || 'Standard'}</span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Créé le</span>
                  <span className="font-mono text-brand-text">{formatDate(document.createdAt)}</span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Modifié le</span>
                  <span className="font-mono text-brand-text">{formatDate(document.updatedAt)}</span>
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="border border-brand-border p-3 bg-white">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1 mb-2">
                Description & Notes
              </h4>
              <p className="text-brand-text italic">
                {document.description || 'Aucune description renseignée.'}
              </p>
            </div>

            {/* Tags */}
            <div className="border border-brand-border p-3 bg-white">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1 mb-2">
                Étiquettes (Tags)
              </h4>
              <div className="flex flex-wrap gap-1">
                {document.tags && document.tags.length > 0 ? (
                  document.tags.map((t) => <Badge key={t} variant="tag">{t}</Badge>)
                ) : (
                  <span className="text-brand-muted italic">Aucun tag</span>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'versions' && (
          <div className="space-y-4">
            {/* Upload New Version Form */}
            <form onSubmit={handleUploadVersion} className="border border-brand-border p-3 bg-brand-alt space-y-2">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-text">
                Verser une nouvelle version
              </h4>
              <input
                type="file"
                onChange={(e) => setNewVersionFile(e.target.files?.[0] || null)}
                className="block w-full text-xs text-brand-text file:mr-2 file:py-1 file:px-2 file:border file:border-brand-border file:bg-white file:text-xs hover:file:bg-brand-alt"
              />
              <input
                type="text"
                placeholder="Note de version (ex: révision clause 4)..."
                value={changeSummary}
                onChange={(e) => setChangeSummary(e.target.value)}
                className="w-full bg-white border border-brand-border px-2 py-1 text-xs"
              />
              <Button
                type="submit"
                variant="primary"
                size="sm"
                icon={<Upload className="w-3.5 h-3.5" />}
                loading={isUploadingVersion}
                disabled={!newVersionFile}
              >
                Verser v{versions.length + 1}
              </Button>
            </form>

            {/* Version List */}
            <div className="space-y-2">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Historique des versions
              </h4>
              {versions.length === 0 ? (
                <div className="p-4 text-center text-brand-muted italic border border-brand-border bg-white">
                  Une seule version initiale (v1)
                </div>
              ) : (
                versions.map((ver) => (
                  <div
                    key={ver.id}
                    className="border border-brand-border p-2.5 bg-white flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-xs text-brand-primary">v{ver.versionNumber}</span>
                        {ver.id === document.activeVersionId && (
                          <span className="text-[9px] bg-emerald-100 text-emerald-800 border border-emerald-300 font-bold px-1">
                            ACTIF
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] text-brand-muted font-mono mt-0.5">
                        SHA256: {ver.hash ? ver.hash.substring(0, 16) + '...' : '-'}
                      </div>
                      <div className="text-[10px] text-brand-muted">
                        Versé le {formatDate(ver.uploadedAt)}
                      </div>
                    </div>

                    <a
                      href={documentApi.downloadUrl(document.id, ver.id)}
                      download
                      className="p-1.5 border border-brand-border hover:border-brand-primary text-brand-muted hover:text-brand-primary"
                      title="Télécharger cette version"
                    >
                      <Download className="w-3.5 h-3.5" />
                    </a>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {activeTab === 'status' && (
          <div className="space-y-4">
            {/* Lock Control */}
            <div className="border border-brand-border p-3 bg-white space-y-2">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Contrôle d'accès & Verrou (Check-out)
              </h4>
              <div className="flex items-center justify-between">
                <div>
                  <span className="font-semibold block">
                    {document.isLocked ? 'Document actuellement verrouillé' : 'Document libre en édition'}
                  </span>
                  <span className="text-[11px] text-brand-muted">
                    {document.isLocked
                      ? 'Un seul agent peut modifier ou verser une version à la fois.'
                      : 'Vous pouvez poser un verrou pour empêcher les modifications concurrentes.'}
                  </span>
                </div>
                <Button
                  variant={document.isLocked ? 'danger' : 'outline'}
                  size="sm"
                  icon={<Lock className="w-3.5 h-3.5" />}
                  onClick={handleToggleCheckout}
                >
                  {document.isLocked ? 'Check-in' : 'Check-out'}
                </Button>
              </div>
            </div>

            {/* Lifecycle Status Change */}
            <div className="border border-brand-border p-3 bg-white space-y-2">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Changer le statut du document
              </h4>
              <div className="grid grid-cols-2 gap-2">
                <Button
                  variant={document.status === 'DRAFT' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('DRAFT')}
                >
                  BROUILLON
                </Button>
                <Button
                  variant={document.status === 'PUBLISHED' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('PUBLISHED')}
                >
                  PUBLIÉ
                </Button>
                <Button
                  variant={document.status === 'ARCHIVED' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('ARCHIVED')}
                >
                  ARCHIVÉ
                </Button>
                <Button
                  variant={document.status === 'TRASHED' ? 'danger' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('TRASHED')}
                >
                  CORBEILLE
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
