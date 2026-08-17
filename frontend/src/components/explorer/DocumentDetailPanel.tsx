import React, { useState } from 'react';
import { DocumentItem, DocumentSearchResult, DocumentVersion } from '../../types';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { documentApi } from '../../api/documentApi';
import { metadataApi, MetadataDefinition } from '../../api/metadataApi';
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
  Sliders,
  CheckCircle2,
  Clock,
  FileSpreadsheet,
  FileCode,
  Image as ImageIcon,
  File,
  Sparkles,
  Layers,
  FileCheck,
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

  // ── Fetch full document details (including metadata array)
  const { data: fetchedDocument } = useQuery({
    queryKey: ['document-details', document?.id],
    queryFn: () => documentApi.getById(document!.id),
    enabled: !!document?.id,
  });

  // ── Fetch metadata definitions to map labels and types
  const { data: defsPage } = useQuery({
    queryKey: ['metadata-definitions'],
    queryFn: () => metadataApi.list(0, 100),
  });

  const definitions: MetadataDefinition[] = defsPage?.content || [];
  const fullDoc = fetchedDocument || doc;
  const metadataList = (fullDoc as any)?.metadata || [];

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
    return new Date(dateStr).toLocaleString('fr-FR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
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

  // File type badge visual helper
  const getFileTypeInfo = (name: string, mime?: string) => {
    const ext = name.split('.').pop()?.toLowerCase() || '';
    if (ext === 'xlsx' || ext === 'xls' || ext === 'csv' || mime?.includes('excel') || mime?.includes('sheet')) {
      return { label: 'XLSX', typeLabel: 'Document Excel', icon: <FileSpreadsheet className="w-5 h-5 text-emerald-600" />, badgeStyle: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
    }
    if (ext === 'pdf' || mime?.includes('pdf')) {
      return { label: 'PDF', typeLabel: 'Document PDF', icon: <FileText className="w-5 h-5 text-red-600" />, badgeStyle: 'bg-red-50 text-red-700 border-red-200' };
    }
    if (ext === 'docx' || ext === 'doc' || mime?.includes('word')) {
      return { label: 'DOCX', typeLabel: 'Document Word', icon: <FileText className="w-5 h-5 text-blue-600" />, badgeStyle: 'bg-blue-50 text-blue-700 border-blue-200' };
    }
    if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext) || mime?.includes('image')) {
      return { label: ext.toUpperCase(), typeLabel: 'Fichier Image', icon: <ImageIcon className="w-5 h-5 text-purple-600" />, badgeStyle: 'bg-purple-50 text-purple-700 border-purple-200' };
    }
    return { label: ext ? ext.toUpperCase() : 'FILE', typeLabel: 'Fichier Numérique', icon: <File className="w-5 h-5 text-slate-600" />, badgeStyle: 'bg-slate-100 text-slate-700 border-slate-200' };
  };

  const fileTypeInfo = getFileTypeInfo(document.name, document.mimeType);

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full max-w-xl sm:max-w-xl md:max-w-2xl bg-white border-l border-slate-200/90 shadow-2xl flex flex-col animate-in slide-in-from-right duration-200 font-sans">
      {/* Top Corporate Red Accent Line */}
      <div className="h-1.5 bg-[#C8102E] w-full shrink-0" />

      {/* ─────────────────────────────────────────────────────────────
          1. HEADER STICKY DU DOCUMENT
      ───────────────────────────────────────────────────────────── */}
      <div className="p-5 bg-gradient-to-b from-slate-50/90 to-white border-b border-slate-200/80 shrink-0 space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3.5 flex-1 min-w-0">
            <div className={`p-2.5 rounded-xl border ${fileTypeInfo.badgeStyle} shrink-0 shadow-xs flex items-center justify-center mt-0.5`}>
              {fileTypeInfo.icon}
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="font-bold text-base sm:text-lg text-slate-900 tracking-tight leading-snug break-words" title={document.name}>
                  {document.name}
                </h2>
                <Badge status={fullDoc.status || doc.status} />
              </div>
              <p className="text-xs text-slate-500 font-medium flex items-center gap-2 mt-1">
                <span>{fileTypeInfo.typeLabel}</span>
                <span className="text-slate-300">•</span>
                <span>Modifié le {formatDate(fullDoc.updatedAt || doc.updatedAt)}</span>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-all shrink-0"
            aria-label="Fermer le panneau"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Action Buttons Bar */}
        <div className="flex items-center gap-2.5 pt-1">
          <Button
            variant="primary"
            size="sm"
            icon={<Download className="w-4 h-4" />}
            className="bg-[#C8102E] hover:bg-[#A60D25] text-white font-semibold text-xs px-4 py-2 rounded-lg shadow-sm hover:shadow transition-all flex-1 sm:flex-initial"
            onClick={() => handleSecureDownload(document.id)}
          >
            Télécharger
          </Button>

          {canPreview ? (
            <Button
              variant="outline"
              size="sm"
              icon={<Eye className="w-4 h-4" />}
              className="border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50 text-slate-700 font-medium text-xs px-3.5 py-2 rounded-lg transition-all"
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
              icon={<ExternalLink className="w-4 h-4" />}
              className="border-slate-200 opacity-50 cursor-not-allowed bg-slate-50 text-slate-400 text-xs px-3.5 py-2 rounded-lg"
              title={`Aperçu non disponible pour ${mimeType || 'ce format'}`}
              disabled
            >
              Aperçu
            </Button>
          )}

          <Button
            variant="outline"
            size="sm"
            icon={<Edit3 className="w-4 h-4 text-slate-600" />}
            className="border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50 text-slate-700 font-medium text-xs px-3.5 py-2 rounded-lg transition-all"
            onClick={() => setIsEditModalOpen(true)}
          >
            Modifier
          </Button>
        </div>

        {/* Download Error Banner */}
        {downloadError && (
          <div className="flex items-center gap-2.5 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg animate-in fade-in">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span className="font-medium">{downloadError}</span>
          </div>
        )}
      </div>

      {/* ─────────────────────────────────────────────────────────────
          2. NAV TABS STICKY
      ───────────────────────────────────────────────────────────── */}
      <div className="flex border-b border-slate-200 bg-slate-50/70 px-4 text-xs font-semibold shrink-0 gap-6">
        <button
          onClick={() => setActiveTab('info')}
          className={`py-3 px-1 flex items-center gap-2 border-b-2 transition-all duration-150 ${
            activeTab === 'info'
              ? 'border-[#C8102E] text-[#C8102E] font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <Info className="w-4 h-4" />
          Fiche Document
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`py-3 px-1 flex items-center gap-2 border-b-2 transition-all duration-150 ${
            activeTab === 'versions'
              ? 'border-[#C8102E] text-[#C8102E] font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <History className="w-4 h-4" />
          Historique
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
            activeTab === 'versions' ? 'bg-red-50 text-[#C8102E]' : 'bg-slate-200 text-slate-600'
          }`}>
            {versions.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('status')}
          className={`py-3 px-1 flex items-center gap-2 border-b-2 transition-all duration-150 ${
            activeTab === 'status'
              ? 'border-[#C8102E] text-[#C8102E] font-bold'
              : 'border-transparent text-slate-500 hover:text-slate-900 hover:border-slate-300'
          }`}
        >
          <Shield className="w-4 h-4" />
          Statut & Verrou
          {isCurrentlyLocked && (
            <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" title="Document verrouillé" />
          )}
        </button>
      </div>

      {/* ─────────────────────────────────────────────────────────────
          3. CONTENU PRINCIPAL PAR ONGLET
      ───────────────────────────────────────────────────────────── */}
      <div className="flex-1 overflow-y-auto p-5 space-y-5 text-xs bg-slate-50/40">
        {/* ── TAB 1: FICHE DOCUMENT ─────────────────────────────── */}
        {activeTab === 'info' && (
          <div className="space-y-5">
            {/* 1. INFORMATIONS GÉNÉRALES */}
            <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-xs space-y-3.5">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 flex items-center gap-2">
                  <FileCheck className="w-4 h-4 text-slate-600" />
                  Informations Générales
                </h3>
                <button
                  onClick={() => setIsEditModalOpen(true)}
                  className="text-[11px] text-[#C8102E] font-bold hover:underline flex items-center gap-1"
                >
                  <Edit3 className="w-3 h-3" /> Éditer les propriétés
                </button>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Statut Metier</span>
                  <Badge status={fullDoc.status || doc.status} />
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Format / MIME</span>
                  <span className="font-mono text-xs font-semibold text-slate-800 bg-slate-100 px-2 py-0.5 rounded border border-slate-200/60 inline-block truncate max-w-full">
                    {fullDoc.mimeType || doc.mimeType || 'Standard'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Catégorie</span>
                  <span className="font-medium text-slate-800 flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5 text-slate-400" />
                    {(fullDoc as any).categoryName || (doc as any).categoryName || 'Non catégorisé'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Département</span>
                  <span className="font-medium text-slate-800 flex items-center gap-1.5">
                    <Building2 className="w-3.5 h-3.5 text-slate-400" />
                    {(fullDoc as any).departmentName || (doc as any).departmentName || 'Tous départements'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Responsable</span>
                  <span className="font-medium text-slate-800 flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    {(fullDoc as any).ownerName || (fullDoc as any).ownerUsername || (doc as any).ownerName || (doc as any).ownerUsername || doc.ownerId || '-'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Date d'expiration</span>
                  <span className="font-mono text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    {(fullDoc as any).expirationDate || (doc as any).expirationDate || '—'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Date de création</span>
                  <span className="font-mono text-slate-700 flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    {formatDate(fullDoc.createdAt || doc.createdAt)}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">Dernière modification</span>
                  <span className="font-mono text-slate-700 flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-slate-400" />
                    {formatDate(fullDoc.updatedAt || doc.updatedAt)}
                  </span>
                </div>
              </div>
            </div>

            {/* 2. MÉTADONNÉES DYNAMIQUES */}
            <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-xs space-y-3">
              <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                <div>
                  <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-[#C8102E]" />
                    Métadonnées Spécifiques
                  </h3>
                  <p className="text-[10px] text-slate-400 font-normal mt-0.5">
                    Informations personnalisées associées à ce document
                  </p>
                </div>
              </div>

              {metadataList.length === 0 ? (
                <div className="p-3 text-center bg-slate-50 rounded-lg border border-slate-100 text-slate-400 italic text-xs">
                  Aucune métadonnée renseignée pour ce document.
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs pt-1">
                  {metadataList.map((meta: any, idx: number) => {
                    const def = definitions.find(
                      (d) =>
                        (meta.definitionId && d.id === meta.definitionId) ||
                        (d.name && meta.key && d.name.toLowerCase() === meta.key.toLowerCase()) ||
                        (d.label && meta.key && d.label.toLowerCase() === meta.key.toLowerCase())
                    );
                    const label = def?.label || def?.name || meta.key;
                    let type = def?.type || 'STRING';
                    const rawVal = meta.value != null ? String(meta.value).trim() : '';

                    // Fallback type inference if definition not found
                    if (!def) {
                      if (rawVal === 'true' || rawVal === 'false') {
                        type = 'BOOLEAN';
                      } else if (rawVal.startsWith('[') || (rawVal.includes(',') && !rawVal.includes(' '))) {
                        type = 'MULTI_SELECT';
                      }
                    }

                    const renderFormattedValue = () => {
                      if (!rawVal && rawVal !== 'false') {
                        return <span className="text-slate-400 italic text-xs">Non renseigné</span>;
                      }

                      if (type === 'BOOLEAN' || rawVal === 'true' || rawVal === 'false') {
                        const isTrue = rawVal === 'true' || rawVal === '1';
                        return (
                          <span
                            className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${
                              isTrue
                                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                : 'bg-slate-100 text-slate-600 border-slate-200'
                            }`}
                          >
                            {isTrue ? (
                              <>
                                <CheckCircle className="w-3.5 h-3.5 text-emerald-600" /> Oui
                              </>
                            ) : (
                              'Non'
                            )}
                          </span>
                        );
                      }

                      if (type === 'MULTI_SELECT' || rawVal.startsWith('[')) {
                        let items: string[] = [];
                        if (rawVal.startsWith('[') && rawVal.endsWith(']')) {
                          try {
                            const parsed = JSON.parse(rawVal);
                            if (Array.isArray(parsed)) {
                              items = parsed.map((s: any) => String(s).trim()).filter(Boolean);
                            }
                          } catch (e) {
                            items = rawVal.replace(/[\[\]"]/g, '').split(',').map((s) => s.trim()).filter(Boolean);
                          }
                        } else {
                          items = rawVal.split(',').map((s: string) => s.trim()).filter(Boolean);
                        }

                        if (items.length === 0) {
                          return <span className="text-slate-400 italic text-xs">Non renseigné</span>;
                        }
                        return (
                          <div className="flex flex-wrap gap-1.5 mt-1">
                            {items.map((item: string, i: number) => (
                              <span
                                key={i}
                                className="px-2.5 py-1 text-xs font-semibold bg-red-50 text-[#C8102E] border border-red-100 rounded-md shadow-xs"
                              >
                                {item}
                              </span>
                            ))}
                          </div>
                        );
                      }

                      if (type === 'SELECT') {
                        return (
                          <span className="font-semibold text-slate-800 text-xs bg-slate-100 px-2.5 py-1 rounded-md border border-slate-200 inline-block shadow-xs">
                            {rawVal}
                          </span>
                        );
                      }

                      if (type === 'URL') {
                        return (
                          <a
                            href={rawVal.startsWith('http') ? rawVal : `https://${rawVal}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="font-mono text-[#C8102E] hover:underline text-xs truncate block font-medium"
                          >
                            {rawVal}
                          </a>
                        );
                      }

                      return <span className="font-semibold text-slate-800 text-xs block font-mono">{rawVal}</span>;
                    };

                    return (
                      <div key={idx} className={type === 'MULTI_SELECT' ? 'col-span-full' : ''}>
                        <span className="text-slate-400 block text-[10px] uppercase font-semibold tracking-wider mb-1">
                          {label}
                        </span>
                        {renderFormattedValue()}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 3. DESCRIPTION & NOTES */}
            <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-xs space-y-2">
              <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 border-b border-slate-100 pb-2">
                Description & Notes
              </h3>
              <p className="text-slate-700 leading-relaxed text-xs pt-1">
                {fullDoc.description || doc.description ? (
                  <span className="italic">{fullDoc.description || doc.description}</span>
                ) : (
                  <span className="text-slate-400 italic">Aucune description renseignée.</span>
                )}
              </p>
            </div>

            {/* 4. ÉTIQUETTES (TAGS) */}
            <div className="bg-white border border-slate-200/80 rounded-xl p-4 shadow-xs space-y-2.5">
              <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 border-b border-slate-100 pb-2 flex items-center gap-1.5">
                <TagIcon className="w-3.5 h-3.5 text-slate-500" />
                Étiquettes (Tags)
              </h3>
              <div className="flex flex-wrap gap-2 pt-0.5">
                {(fullDoc.tags || doc.tags) && (fullDoc.tags || doc.tags)!.length > 0 ? (
                  (fullDoc.tags || doc.tags)!.map((t) => (
                    <span key={t} className="px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200/80 shadow-xs">
                      #{t}
                    </span>
                  ))
                ) : (
                  <span className="text-slate-400 italic">Aucune étiquette définie.</span>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── TAB 2: HISTORIQUE ET VERSIONS ──────────────────────── */}
        {activeTab === 'versions' && (
          <div className="space-y-5">
            {/* Formulaire Nouvel envoi */}
            <form onSubmit={handleUploadVersion} className="bg-white border border-slate-200/80 p-4 rounded-xl shadow-xs space-y-3">
              <div className="flex items-center gap-2 border-b border-slate-100 pb-2">
                <Upload className="w-4 h-4 text-[#C8102E]" />
                <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-700">
                  Verser une nouvelle version du document
                </h3>
              </div>

              <input
                type="file"
                onChange={(e) => setNewVersionFile(e.target.files?.[0] || null)}
                className="block w-full text-xs text-slate-600 file:mr-3 file:py-1.5 file:px-3 file:border file:border-slate-200 file:bg-slate-50 file:rounded-lg file:text-xs file:font-semibold hover:file:bg-slate-100 cursor-pointer"
              />

              <input
                type="text"
                placeholder="Note de version (ex: Révision clause 4, mise à jour tarifaire)..."
                value={changeSummary}
                onChange={(e) => setChangeSummary(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 text-xs text-slate-800 focus:outline-none focus:border-[#C8102E] focus:bg-white transition-all"
              />

              <Button
                type="submit"
                variant="primary"
                size="sm"
                icon={<Upload className="w-3.5 h-3.5" />}
                loading={isUploadingVersion}
                disabled={!newVersionFile}
                className="bg-[#C8102E] hover:bg-[#A60D25] text-white text-xs font-semibold"
              >
                Verser v{versions.length + 1}
              </Button>
            </form>

            {/* Timeline Historique */}
            <div className="space-y-3">
              <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 flex items-center gap-2">
                <History className="w-4 h-4 text-slate-600" />
                Historique Complet des Versions
              </h3>

              {versions.length === 0 ? (
                <div className="p-5 text-center text-slate-400 italic bg-white border border-slate-200/80 rounded-xl shadow-xs">
                  Une seule version initiale (v1) enregistrée.
                </div>
              ) : (
                <div className="relative pl-3 space-y-3 before:absolute before:left-5 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-200">
                  {versions.map((ver) => (
                    <div
                      key={ver.id}
                      className="relative pl-6 bg-white border border-slate-200/80 p-3.5 rounded-xl shadow-xs flex items-center justify-between gap-3"
                    >
                      <div className="absolute left-3 top-4 w-2.5 h-2.5 rounded-full bg-[#C8102E] ring-4 ring-white" />
                      <div className="space-y-1 min-w-0 flex-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-mono font-bold text-xs text-[#C8102E] bg-red-50 px-2 py-0.5 rounded border border-red-100">
                            v{ver.versionNumber}
                          </span>
                          {ver.id === document.activeVersionId && (
                            <span className="text-[10px] bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold px-2 py-0.5 rounded-full inline-flex items-center gap-1">
                              <CheckCircle className="w-3 h-3 text-emerald-600" /> VERSION ACTIVES
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-slate-600 font-medium">
                          {(ver as any).summary || 'Nouvelle version versée'}
                        </div>
                        <div className="text-[10px] text-slate-400 font-mono">
                          SHA256: {ver.hash ? ver.hash.substring(0, 20) + '...' : '-'} • Versé le {formatDate(ver.uploadedAt)}
                        </div>
                      </div>

                      <button
                        onClick={() => handleSecureDownload(document.id, ver.id)}
                        className="p-2 border border-slate-200 hover:border-[#C8102E] text-slate-500 hover:text-[#C8102E] bg-slate-50 hover:bg-red-50 rounded-lg transition-all shrink-0"
                        title="Télécharger cette version"
                      >
                        <Download className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* ── TAB 3: STATUT ET VERROU ────────────────────────────── */}
        {activeTab === 'status' && (
          <div className="space-y-5">
            {/* Contrôle de Verrou (Check-out) */}
            <div className="bg-white border border-slate-200/80 p-4 rounded-xl shadow-xs space-y-3">
              <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 flex items-center gap-2 border-b border-slate-100 pb-2">
                <Lock className="w-4 h-4 text-slate-600" />
                Contrôle d'Accès & Verrou (Check-out)
              </h3>

              <div className="flex items-center justify-between gap-4 pt-1">
                <div className="space-y-1">
                  <span className="font-semibold text-slate-800 text-xs block">
                    {isCurrentlyLocked ? '🔒 Document actuellement verrouillé' : '🔓 Document disponible en édition'}
                  </span>
                  <p className="text-[11px] text-slate-500 leading-relaxed">
                    {isCurrentlyLocked
                      ? 'Un utilisateur détient le verrou d\'édition sur ce document.'
                      : 'Poser un verrou empêche les modifications concurrentes par d\'autres employés.'}
                  </p>
                  {lockStatus?.lockedByUsername && isCurrentlyLocked && (
                    <span className="text-xs text-amber-800 font-medium block bg-amber-50 px-2.5 py-1 rounded border border-amber-200 mt-1">
                      Verrouillé par : <strong>{lockStatus.lockedByUsername}</strong>
                    </span>
                  )}
                </div>

                <Button
                  variant={isCurrentlyLocked ? 'danger' : 'outline'}
                  size="sm"
                  icon={<Lock className="w-4 h-4" />}
                  onClick={handleToggleCheckout}
                  className={`shrink-0 font-semibold ${
                    isCurrentlyLocked
                      ? 'bg-amber-600 hover:bg-amber-700 border-amber-600 text-white'
                      : 'border-slate-200 hover:border-slate-300 text-slate-700'
                  }`}
                >
                  {isCurrentlyLocked ? 'Déverrouiller (Check-in)' : 'Verrouiller (Check-out)'}
                </Button>
              </div>

              {/* Erreur verrou */}
              {lockError && (
                <div className="flex items-center gap-2.5 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{lockError}</span>
                </div>
              )}
            </div>

            {/* Transition de statut Métier */}
            <div className="bg-white border border-slate-200/80 p-4 rounded-xl shadow-xs space-y-3">
              <h3 className="font-bold text-[11px] uppercase tracking-wider text-slate-500 border-b border-slate-100 pb-2">
                Changer le Statut du Document
              </h3>

              <div className="flex items-center gap-2">
                <span className="text-slate-500">Statut actuel :</span>
                <Badge status={document.status} />
              </div>

              <div className="grid grid-cols-2 gap-2.5 pt-2">
                <Button
                  variant={document.status === 'DRAFT' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('DRAFT')}
                  className={document.status === 'DRAFT' ? 'bg-slate-700 border-slate-700 text-white font-bold' : ''}
                >
                  BROUILLON
                </Button>

                <Button
                  variant={document.status === 'PUBLISHED' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('PUBLISHED')}
                  className={document.status === 'PUBLISHED' ? 'bg-emerald-600 border-emerald-600 text-white font-bold' : ''}
                >
                  PUBLIÉ
                </Button>

                <Button
                  variant={document.status === 'ARCHIVED' ? 'primary' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('ARCHIVED')}
                  className={document.status === 'ARCHIVED' ? 'bg-amber-600 border-amber-600 text-white font-bold' : ''}
                >
                  ARCHIVÉ
                </Button>

                <Button
                  variant={document.status === 'TRASHED' ? 'danger' : 'outline'}
                  size="sm"
                  onClick={() => handleChangeStatus('TRASHED')}
                  className={document.status === 'TRASHED' ? 'bg-[#C8102E] border-[#C8102E] text-white font-bold' : ''}
                >
                  CORBEILLE
                </Button>
              </div>

              {/* Erreur statut */}
              {statusError && (
                <div className="flex items-center gap-2.5 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{statusError}</span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* ─────────────────────────────────────────────────────────────
          MODAL DE MODIFICATION DES PROPRIÉTÉS
      ───────────────────────────────────────────────────────────── */}
      <EditDocumentModal
        document={fullDoc!}
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onSuccess={(updatedDoc) => {
          setCurrentDocument(updatedDoc);
          onRefresh();
          queryClient.invalidateQueries({ queryKey: ['document-details', document.id] });
          queryClient.invalidateQueries({ queryKey: ['folder-content'] });
          queryClient.invalidateQueries({ queryKey: ['search-documents'] });
        }}
      />
    </div>
  );
};
