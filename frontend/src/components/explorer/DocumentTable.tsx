import React from 'react';
import { DocumentItem, DocumentSearchResult } from '../../types';
import { Badge } from '../ui/Badge';
import {
  FileText,
  Image as ImageIcon,
  FileSpreadsheet,
  FileCode,
  File,
  Lock,
  Download,
  Eye,
  Trash2,
  MoreVertical,
  ArrowUpDown,
  Tag,
  FolderInput,
} from 'lucide-react';
import { documentApi } from '../../api/documentApi';

interface DocumentTableProps {
  documents: (DocumentItem | DocumentSearchResult)[];
  selectedIds: string[];
  onToggleSelect: (id: string) => void;
  onToggleSelectAll: () => void;
  onSelectDocument: (doc: DocumentItem | DocumentSearchResult) => void;
  onDeleteDocument: (id: string) => void;
  onCheckoutDocument: (id: string) => void;
  onCheckinDocument: (id: string) => void;
  onPreviewDocument?: (doc: DocumentItem | DocumentSearchResult) => void;
  onMoveSingleDocument?: (doc: DocumentItem | DocumentSearchResult) => void;
  onSort?: (field: string) => void;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export const DocumentTable: React.FC<DocumentTableProps> = ({
  documents,
  selectedIds,
  onToggleSelect,
  onToggleSelectAll,
  onSelectDocument,
  onDeleteDocument,
  onCheckoutDocument,
  onCheckinDocument,
  onPreviewDocument,
  onMoveSingleDocument,
  onSort,
  sortBy,
  sortDirection,
}) => {
  const isAllSelected = documents.length > 0 && selectedIds.length === documents.length;

  const getMimeIcon = (mimeType?: string) => {
    if (!mimeType) return <File className="w-4 h-4 text-brand-muted shrink-0" />;
    if (mimeType.includes('pdf')) return <FileText className="w-4 h-4 text-brand-primary shrink-0" />;
    if (mimeType.includes('image')) return <ImageIcon className="w-4 h-4 text-brand-secondary shrink-0" />;
    if (mimeType.includes('sheet') || mimeType.includes('excel'))
      return <FileSpreadsheet className="w-4 h-4 text-emerald-700 shrink-0" />;
    if (mimeType.includes('word') || mimeType.includes('document'))
      return <FileText className="w-4 h-4 text-brand-primary shrink-0" />;
    return <FileCode className="w-4 h-4 text-brand-muted shrink-0" />;
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="w-full overflow-x-auto border border-brand-border bg-brand-surface rounded-lg shadow-card">
      <table className="w-full text-left border-collapse table-dense">
        <thead>
          <tr className="select-none">
            <th className="w-9 text-center">
              <input
                type="checkbox"
                checked={isAllSelected}
                onChange={onToggleSelectAll}
                className="rounded-sm border-brand-border text-brand-primary focus:ring-brand-primary cursor-pointer"
              />
            </th>
            <th className="cursor-pointer" onClick={() => onSort?.('name')}>
              <div className="flex items-center gap-1.5">
                <span>Document</span>
                <ArrowUpDown className="w-3 h-3 text-brand-muted" />
              </div>
            </th>
            <th>Statut</th>
            <th>Dossier / Emplacement</th>
            <th>Propriétaire</th>
            <th className="cursor-pointer" onClick={() => onSort?.('updatedAt')}>
              <div className="flex items-center gap-1.5">
                <span>Dernière modification</span>
                <ArrowUpDown className="w-3 h-3 text-brand-muted" />
              </div>
            </th>
            <th>Tags</th>
            <th className="text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-brand-border font-sans">
          {documents.length === 0 ? (
            <tr>
              <td colSpan={8} className="py-12 text-center text-brand-muted italic bg-brand-bg">
                Aucun document trouvé dans cet emplacement
              </td>
            </tr>
          ) : (
            documents.map((doc) => {
              const isSelected = selectedIds.includes(doc.id);
              const searchDoc = doc as DocumentSearchResult;
              const isLocked = Boolean((doc as any).isLocked ?? (doc as any).locked);

              return (
                <tr
                  key={doc.id}
                  draggable={true}
                  onDragStart={(e) => {
                    const idsToMove = selectedIds.includes(doc.id) && selectedIds.length > 1 ? selectedIds : [doc.id];
                    e.dataTransfer.setData('text/plain', JSON.stringify({ documentIds: idsToMove }));
                    e.dataTransfer.effectAllowed = 'move';
                  }}
                  className={`group transition-colors cursor-grab active:cursor-grabbing ${
                    isSelected ? 'bg-brand-primary-light/60 border-l-3 border-l-brand-primary font-medium' : ''
                  }`}
                >
                  {/* Checkbox */}
                  <td className="text-center">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => onToggleSelect(doc.id)}
                      className="rounded-sm border-brand-border text-brand-primary focus:ring-brand-primary cursor-pointer"
                    />
                  </td>

                  {/* Document Name + Icon + Lock indicator */}
                  <td>
                    <div className="flex items-center gap-2.5">
                      {getMimeIcon(doc.mimeType)}
                      <button
                        onClick={() => onSelectDocument(doc)}
                        className="font-medium text-xs text-brand-text hover:text-brand-primary hover:underline text-left truncate max-w-xs"
                      >
                        {doc.name}
                      </button>

                      {isLocked && (
                        <span
                          className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-bold bg-red-50 text-brand-locked border border-red-200 rounded-md"
                          title="Document verrouillé par check-out"
                        >
                          <Lock className="w-2.5 h-2.5" />
                          VERROUILLÉ
                        </span>
                      )}
                    </div>
                  </td>

                  {/* Status Badge */}
                  <td>
                    <Badge status={doc.status} />
                  </td>

                  {/* Folder Location */}
                  <td className="text-xs text-brand-muted truncate max-w-[150px]">
                    {searchDoc.folderName ? (
                      <span className="inline-flex items-center gap-1 font-mono text-[11px]">
                        <FolderInput className="w-3.5 h-3.5 shrink-0 text-brand-muted" />
                        {searchDoc.folderName}
                      </span>
                    ) : (
                      <span className="italic font-mono text-[10px] text-brand-muted">Racine</span>
                    )}
                  </td>

                  {/* Owner */}
                  <td className="text-xs text-brand-text font-mono">
                    {searchDoc.ownerUsername || 'Agent GED'}
                  </td>

                  {/* Updated At */}
                  <td className="text-xs text-brand-muted font-mono whitespace-nowrap">
                    {formatDate(doc.updatedAt)}
                  </td>

                  {/* Tags */}
                  <td>
                    <div className="flex flex-wrap gap-1 max-w-[150px]">
                      {doc.tags && doc.tags.length > 0 ? (
                        doc.tags.map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center gap-1 px-1.5 py-0.5 text-[9px] font-mono bg-brand-alt border border-brand-border text-brand-text rounded-md"
                          >
                            <Tag className="w-2.5 h-2.5 text-brand-muted" />
                            {tag}
                          </span>
                        ))
                      ) : (
                        <span className="text-brand-muted text-[10px] italic">-</span>
                      )}
                    </div>
                  </td>

                  {/* Action buttons */}
                  <td className="text-right whitespace-nowrap">
                    <div className="inline-flex items-center gap-1 opacity-90 group-hover:opacity-100">
                      {/* Preview Button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (onPreviewDocument) {
                            onPreviewDocument(doc);
                          } else {
                            window.open(documentApi.previewUrl(doc.id), '_blank');
                          }
                        }}
                        className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-md transition-colors"
                        title="Aperçu rapide"
                      >
                        <Eye className="w-3.5 h-3.5" />
                      </button>

                      {/* Download Button */}
                      <button
                        onClick={async (e) => {
                          e.stopPropagation();
                          try {
                            await documentApi.downloadFile(doc.id, doc.name);
                          } catch (err: any) {
                            alert('Erreur lors du téléchargement: ' + (err.response?.data?.message || err.message));
                          }
                        }}
                        className="p-1.5 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light border border-transparent hover:border-brand-primary/30 rounded-md transition-colors"
                        title="Télécharger"
                      >
                        <Download className="w-3.5 h-3.5" />
                      </button>

                      {/* Move Button */}
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (onMoveSingleDocument) {
                            onMoveSingleDocument(doc);
                          }
                        }}
                        className="p-1.5 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light border border-transparent hover:border-brand-primary/30 rounded-md transition-colors"
                        title="Déplacer vers..."
                      >
                        <FolderInput className="w-3.5 h-3.5" />
                      </button>

                      {/* Checkout / Lock toggle */}
                      {isLocked ? (
                        <button
                          onClick={() => onCheckinDocument(doc.id)}
                          className="p-1.5 text-brand-primary hover:bg-brand-primary-light border border-transparent hover:border-brand-primary/30 rounded-md transition-colors"
                          title="Déverrouiller (Check-in)"
                        >
                          <Lock className="w-3.5 h-3.5" />
                        </button>
                      ) : (
                        <button
                          onClick={() => onCheckoutDocument(doc.id)}
                          className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-md transition-colors"
                          title="Verrouiller (Check-out)"
                        >
                          <Lock className="w-3.5 h-3.5 opacity-40" />
                        </button>
                      )}

                      {/* Delete */}
                      <button
                        onClick={() => onDeleteDocument(doc.id)}
                        className="p-1.5 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light border border-transparent hover:border-brand-primary/30 rounded-md transition-colors"
                        title="Mettre en corbeille"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>

                      {/* More Details */}
                      <button
                        onClick={() => onSelectDocument(doc)}
                        className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-md transition-colors"
                        title="Détails & Versions"
                      >
                        <MoreVertical className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
};
