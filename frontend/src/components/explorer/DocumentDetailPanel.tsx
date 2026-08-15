import React, { useState } from 'react';
import { DocumentItem, DocumentSearchResult, DocumentVersion } from '../../types';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { documentApi } from '../../api/documentApi';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../../utils/errorMessages';
import { EditDocumentModal } from './EditDocumentModal';
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
  AlertCircle,
  ExternalLink,
  Edit3,
  Building2,
  Tag as TagIcon,
} from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';

interface DocumentDetailPanelProps {
  document: DocumentItem | DocumentSearchResult | null;
  onClose: () => void;
  onRefresh: () => void;
  onPreview?: (doc: DocumentItem | DocumentSearchResult) => void;
}

export const DocumentDetailPanel: React.FC<DocumentDetailPanelProps> = ({
  document,
  onClose,
  onRefresh,
  onPreview,
}) => {
  const [activeTab, setActiveTab] = useState<'info' | 'versions' | 'status'>('info');
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [currentDocument, setCurrentDocument] = useState<DocumentItem | DocumentSearchResult | null>(document);

  React.useEffect(() => {
    setCurrentDocument(document);
  }, [document]);

  const doc = (currentDocument || document)!;
  const [newVersionFile, setNewVersionFile] = useState<File | null>(null);
  const [changeSummary, setChangeSummary] = useState('');
  const [isUploadingVersion, setIsUploadingVersion] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [lockError, setLockError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  // ── Polling de l'état du verrou toutes les 10 secondes
  const { data: lockStatus } = useQuery({
    queryKey: ['document-lock', document?.id],
    queryFn: () => documentApi.getLockStatus(document!.id),
    enabled: !!document?.id,
    refetchInterval: 10_000,
  });

  // Le verrou actif combine l'état de polling (temps réel) + l'état initial du document
  const isCurrentlyLocked = lockStatus?.locked ?? document?.isLocked ?? (document as any)?.locked ?? false;

  // Query Version history
  const { data: versions = [], refetch: refetchVersions } = useQuery<DocumentVersion[]>({
    queryKey: ['document-versions', document?.id],
    queryFn: () => documentApi.listVersions(document!.id),
    enabled: !!document?.id,
  });

  if (!document) return null;

  const buildVersionedFilename = (name: string, versionId?: string): string => {
    if (!versionId) return name;
    const lastDotIndex = name.lastIndexOf('.');
    const shortVersion = versionId.substring(0, 8);
    if (lastDotIndex === -1) {
      return `${name}_v${shortVersion}`;
    }
    const baseName = name.slice(0, lastDotIndex);
    const extension = name.slice(lastDotIndex);
    return `${baseName}_v${shortVersion}${extension}`;
  };

  // ── Téléchargement sécurisé avec jeton Keycloak Bearer via apiClient
  const handleSecureDownload = async (docId: string, versionId?: string) => {
    setDownloadError(null);
    try {
      const filename = buildVersionedFilename(document.name, versionId);
      await documentApi.downloadFile(docId, filename, versionId);
    } catch (err: any) {
      setDownloadError(err.response?.data?.message || err.message || 'Échec du téléchargement');
    }
  };

  const handleUploadVersion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newVersionFile) return;

    try {
      setIsUploadingVersion(true);
      const formData = new FormData();
      formData.append('file', newVersionFile);
      if (changeSummary) formData.append('changeSummary', changeSummary);

      await documentApi.uploadVersion(document.id, formData);
      toast.success('Nouvelle version ajoutée avec succès');
      setNewVersionFile(null);
      setChangeSummary('');
      refetchVersions();
      onRefresh();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de l\'envoi de la nouvelle version.'));
    } finally {
      setIsUploadingVersion(false);
    }
  };

  const handleChangeStatus = async (newStatus: any) => {
    setStatusError(null);
    try {
      await documentApi.updateStatus(document.id, newStatus);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      onRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Erreur inconnue';
      setStatusError(msg);
    }
  };

  const handleToggleCheckout = async () => {
    setLockError(null);
    try {
      if (isCurrentlyLocked) {
        await documentApi.checkin(document.id);
      } else {
        await documentApi.checkout(document.id);
      }
      await queryClient.invalidateQueries({ queryKey: ['document-lock', document.id] });
      onRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Erreur inconnue';
      setLockError(msg);
    }
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('fr-FR');
  };

  const PREVIEWABLE_EXTENSIONS = /\.(pdf|docx?|xlsx?|txt|csv|json|xml|html?|png|jpe?g|gif|webp|tiff?|bmp)$/i;
  const mimeType = document.mimeType?.toLowerCase() || '';
  const canPreview =
    mimeType.includes('pdf') ||
    mimeType.includes('image') ||
    mimeType.includes('text') ||
    mimeType.includes('word') ||
    mimeType.includes('excel') ||
    mimeType.includes('sheet') ||
    PREVIEWABLE_EXTENSIONS.test(document.name);

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-md bg-brand-surface border-l border-brand-border shadow-popover flex flex-col animate-in slide-in-from-right duration-200">
      {/* Top Accent Bar */}
      <div className="h-1 bg-brand-primary w-full shrink-0" />

      {/* Panel Header */}
      <div className="p-4 bg-brand-alt/50 border-b border-brand-border flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2.5 truncate">
          <FileText className="w-4 h-4 text-brand-primary shrink-0" />
          <h3 className="font-bold text-xs uppercase tracking-wider text-brand-text truncate">
            {document.name}
          </h3>
        </div>
        <button
          onClick={onClose}
          className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-border/60 rounded-md transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-brand-border bg-brand-bg text-xs font-semibold p-1 gap-1 shrink-0">
        <button
          onClick={() => setActiveTab('info')}
          className={`flex-1 py-2 px-2.5 flex items-center justify-center gap-1.5 rounded-md transition-all duration-150 ${
            activeTab === 'info'
              ? 'bg-brand-surface text-brand-primary shadow-xs font-bold'
              : 'text-brand-muted hover:text-brand-text hover:bg-brand-surface/50'
          }`}
        >
          <Info className="w-3.5 h-3.5" />
          Fiche
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`flex-1 py-2 px-2.5 flex items-center justify-center gap-1.5 rounded-md transition-all duration-150 ${
            activeTab === 'versions'
              ? 'bg-brand-surface text-brand-primary shadow-xs font-bold'
              : 'text-brand-muted hover:text-brand-text hover:bg-brand-surface/50'
          }`}
        >
          <History className="w-3.5 h-3.5" />
          Historique ({versions.length})
        </button>

        <button
          onClick={() => setActiveTab('status')}
          className={`flex-1 py-2 px-2.5 flex items-center justify-center gap-1.5 rounded-md transition-all duration-150 ${
            activeTab === 'status'
              ? 'bg-brand-surface text-brand-primary shadow-xs font-bold'
              : 'text-brand-muted hover:text-brand-text hover:bg-brand-surface/50'
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
            <div className="flex items-center gap-2 p-2.5 bg-brand-alt/50 border border-brand-border rounded-lg shadow-xs">
              {canPreview ? (
                <Button
                  variant="outline"
                  size="sm"
                  icon={<Eye className="w-3.5 h-3.5" />}
                  className="w-full flex-1"
                  onClick={() => {
                    if (onPreview) {
                      onPreview(document);
                    } else {
                      window.open(documentApi.previewUrl(document.id), '_blank');
                    }
                  }}
                >
                  Aperçu
                </Button>
              ) : (
                <Button
                  variant="outline"
                  size="sm"
                  icon={<ExternalLink className="w-3.5 h-3.5" />}
                  className="w-full flex-1 opacity-50 cursor-not-allowed"
                  title={`Aperçu non disponible pour le format ${mimeType || 'inconnu'}`}
                  disabled
                >
                  Aperçu
                </Button>
              )}

              <Button
                variant="primary"
                size="sm"
                icon={<Download className="w-3.5 h-3.5" />}
                className="w-full flex-1"
                onClick={() => handleSecureDownload(document.id)}
              >
                Télécharger
              </Button>

              <Button
                variant="outline"
                size="sm"
                icon={<Edit3 className="w-3.5 h-3.5" />}
                className="w-full flex-1 text-brand-primary border-brand-primary/40 hover:bg-brand-primary/10"
                onClick={() => setIsEditModalOpen(true)}
              >
                Modifier
              </Button>
            </div>

            {/* Message d'erreur téléchargement */}
            {downloadError && (
              <div className="flex items-center gap-2 p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-md">
                <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                <span>{downloadError}</span>
              </div>
            )}

            {/* General Properties */}
            <div className="border border-brand-border p-3.5 space-y-2.5 bg-brand-surface rounded-lg shadow-card">
              <div className="flex items-center justify-between border-b border-brand-border pb-1.5">
                <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                  Propriétés Générales
                </h4>
                <button
                  onClick={() => setIsEditModalOpen(true)}
                  className="text-[11px] text-brand-primary font-bold hover:underline flex items-center gap-1"
                >
                  <Edit3 className="w-3 h-3" /> Modifier les propriétés
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Statut</span>
                  <Badge status={doc.status} />
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Format / MIME</span>
                  <span className="font-mono text-brand-text">{doc.mimeType || 'Standard'}</span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Catégorie</span>
                  <span className="font-medium text-brand-text">
                    {(doc as any).categoryName || 'Non catégorisé'}
                  </span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Département</span>
                  <span className="font-medium text-brand-text">
                    {(doc as any).departmentName || 'Tous départements'}
                  </span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Responsable</span>
                  <span className="font-medium text-brand-text">
                    {(doc as any).ownerName || (doc as any).ownerUsername || doc.ownerId || '-'}
                  </span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Date d'expiration</span>
                  <span className="font-mono text-brand-text">
                    {(doc as any).expirationDate || '-'}
                  </span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Créé le</span>
                  <span className="font-mono text-brand-text">{formatDate(doc.createdAt)}</span>
                </div>

                <div>
                  <span className="text-brand-muted block text-[10px] uppercase font-semibold">Modifié le</span>
                  <span className="font-mono text-brand-text">{formatDate(doc.updatedAt)}</span>
                </div>
              </div>
            </div>

            {/* Description */}
            <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg shadow-card">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1.5 mb-2">
                Description & Notes
              </h4>
              <p className="text-brand-text italic">
                {doc.description || 'Aucune description renseignée.'}
              </p>
            </div>

            {/* Tags */}
            <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg shadow-card">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1.5 mb-2">
                Étiquettes (Tags)
              </h4>
              <div className="flex flex-wrap gap-1.5">
                {doc.tags && doc.tags.length > 0 ? (
                  doc.tags.map((t) => <Badge key={t} variant="tag">{t}</Badge>)
                ) : (
                  <span className="text-brand-muted italic">Aucun tag</span>
                )}
              </div>
            </div>

            {/* Dynamic Metadata */}
            {(doc as any).metadata && (doc as any).metadata.length > 0 && (
              <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg shadow-card">
                <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted border-b border-brand-border pb-1.5 mb-2">
                  Métadonnées dynamiques
                </h4>
                <div className="space-y-1.5">
                  {(doc as any).metadata.map((meta: any, idx: number) => (
                    <div key={idx} className="flex items-center justify-between text-xs py-1 border-b border-brand-border/40 last:border-0">
                      <span className="text-brand-muted font-medium">{meta.key}</span>
                      <span className="font-semibold text-brand-text">{meta.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'versions' && (
          <div className="space-y-4">
            {/* Upload New Version Form */}
            <form onSubmit={handleUploadVersion} className="border border-brand-border p-3.5 bg-brand-alt/50 rounded-lg space-y-2.5">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-text">
                Verser une nouvelle version
              </h4>
              <input
                type="file"
                onChange={(e) => setNewVersionFile(e.target.files?.[0] || null)}
                className="block w-full text-xs text-brand-text file:mr-2 file:py-1 file:px-2.5 file:border file:border-brand-border file:bg-white file:rounded-md file:text-xs hover:file:bg-brand-alt cursor-pointer"
              />
              <input
                type="text"
                placeholder="Note de version (ex: révision clause 4)..."
                value={changeSummary}
                onChange={(e) => setChangeSummary(e.target.value)}
                className="w-full bg-brand-surface border border-brand-border rounded-md px-3 py-1.5 text-xs focus:outline-none focus:border-brand-primary"
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
            <div className="space-y-2.5">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Historique des versions
              </h4>
              {versions.length === 0 ? (
                <div className="p-4 text-center text-brand-muted italic border border-brand-border bg-brand-surface rounded-lg">
                  Une seule version initiale (v1)
                </div>
              ) : (
                versions.map((ver) => (
                  <div
                    key={ver.id}
                    className="border border-brand-border p-3 bg-brand-surface rounded-lg shadow-card flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-xs text-brand-primary">v{ver.versionNumber}</span>
                        {ver.id === document.activeVersionId && (
                          <span className="text-[9px] bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold px-1.5 py-0.5 rounded-md">
                            ACTIF
                          </span>
                        )}
                      </div>
                      <div className="text-[11px] text-brand-muted font-mono mt-0.5">
                        SHA256: {ver.hash ? ver.hash.substring(0, 16) + '...' : '-'}
                      </div>
                      <div className="text-[10px] text-brand-muted mt-0.5">
                        Versé le {formatDate(ver.uploadedAt)}
                      </div>
                    </div>

                    <button
                      onClick={() => handleSecureDownload(document.id, ver.id)}
                      className="p-1.5 border border-brand-border hover:border-brand-primary text-brand-muted hover:text-brand-primary rounded-md transition-colors"
                      title="Télécharger cette version"
                    >
                      <Download className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {activeTab === 'status' && (
          <div className="space-y-4">
            {/* Lock Control */}
            <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg shadow-card space-y-2.5">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Contrôle d'accès & Verrou (Check-out)
              </h4>

              <div className="flex items-center justify-between gap-3">
                <div>
                  <span className="font-semibold block">
                    {isCurrentlyLocked ? 'Document actuellement verrouillé' : 'Document libre en édition'}
                  </span>
                  <span className="text-[11px] text-brand-muted block mt-0.5">
                    {isCurrentlyLocked
                      ? 'Un seul agent peut modifier ou verser une version à la fois.'
                      : 'Vous pouvez poser un verrou pour empêcher les modifications concurrentes.'}
                  </span>
                  {lockStatus?.lockedByUsername && isCurrentlyLocked && (
                    <span className="text-[11px] text-amber-700 font-medium block mt-1">
                      Verrouillé par : {lockStatus.lockedByUsername}
                    </span>
                  )}
                </div>
                <Button
                  variant={isCurrentlyLocked ? 'danger' : 'outline'}
                  size="sm"
                  icon={<Lock className="w-3.5 h-3.5" />}
                  onClick={handleToggleCheckout}
                  className="shrink-0"
                >
                  {isCurrentlyLocked ? 'Check-in' : 'Check-out'}
                </Button>
              </div>

              {/* Erreur verrou */}
              {lockError && (
                <div className="flex items-center gap-2 p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-md">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{lockError}</span>
                </div>
              )}
            </div>

            {/* Lifecycle Status Change */}
            <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg shadow-card space-y-2.5">
              <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
                Changer le statut du document
              </h4>
              <p className="text-[11px] text-brand-muted">
                Statut actuel : <Badge status={document.status} />
              </p>
              <div className="grid grid-cols-2 gap-2 pt-1">
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

              {/* Erreur statut */}
              {statusError && (
                <div className="flex items-center gap-2 p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-md">
                  <AlertCircle className="w-3.5 h-3.5 shrink-0" />
                  <span>{statusError}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Edit Document Properties Modal */}
      <EditDocumentModal
        document={doc!}
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSuccess={(updatedDoc) => {
          setCurrentDocument(updatedDoc);
          onRefresh();
          queryClient.invalidateQueries({ queryKey: ['folder-content'] });
          queryClient.invalidateQueries({ queryKey: ['search-documents'] });
        }}
      />
    </div>
  );
};
