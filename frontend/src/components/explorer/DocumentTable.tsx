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
  onSort,
  sortBy,
  sortDirection,
}) => {
  const isAllSelected = documents.length > 0 && selectedIds.length === documents.length;

  const getMimeIcon = (mimeType?: string) => {
    if (!mimeType) return <File className="w-4 h-4 text-brand-muted shrink-0" />;
    if (mimeType.includes('pdf')) return <FileText className="w-4 h-4 text-red-700 shrink-0" />;
    if (mimeType.includes('image')) return <ImageIcon className="w-4 h-4 text-amber-600 shrink-0" />;
    if (mimeType.includes('sheet') || mimeType.includes('excel'))
      return <FileSpreadsheet className="w-4 h-4 text-emerald-700 shrink-0" />;
    if (mimeType.includes('word') || mimeType.includes('document'))
      return <FileText className="w-4 h-4 text-blue-700 shrink-0" />;
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
    <div className="w-full overflow-x-auto border border-brand-border bg-white rounded-none">
      <table className="w-full text-left border-collapse table-dense">
        <thead>
          <tr className="select-none">
            <th className="w-8 text-center">
              <input
                type="checkbox"
                checked={isAllSelected}
                onChange={onToggleSelectAll}
                className="rounded-none border-brand-border text-brand-primary focus:ring-brand-primary cursor-pointer"
              />
            </th>
            <th className="cursor-pointer" onClick={() => onSort?.('name')}>
              <div className="flex items-center gap-1">
                <span>Document</span>
                <ArrowUpDown className="w-3 h-3 text-brand-muted" />
              </div>
            </th>
            <th>Statut</th>
            <th>Dossier / Emplacement</th>
            <th>Propriétaire</th>
            <th className="cursor-pointer" onClick={() => onSort?.('updatedAt')}>
              <div className="flex items-center gap-1">
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

              return (
                <tr
                  key={doc.id}
                  className={`group transition-colors ${
                    isSelected ? 'bg-brand-primary-light/60' : ''
                  }`}
                >
                  {/* Checkbox */}
                  <td className="text-center">
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={() => onToggleSelect(doc.id)}
                      className="rounded-none border-brand-border text-brand-primary focus:ring-brand-primary cursor-pointer"
                    />
                  </td>

                  {/* Document Name + Icon + Lock indicator */}
                  <td>
                    <div className="flex items-center gap-2">
                      {getMimeIcon(doc.mimeType)}
                      <button
                        onClick={() => onSelectDocument(doc)}
                        className="font-medium text-xs text-brand-text hover:text-brand-primary hover:underline text-left truncate max-w-xs"
                      >
                        {doc.name}
                      </button>

                      {doc.isLocked && (
                        <span
                          className="inline-flex items-center gap-1 px-1 py-0.2 text-[9px] font-bold bg-red-100 text-red-800 border border-red-300"
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
                        <FolderInput className="w-3 h-3 shrink-0 text-brand-muted" />
                        {searchDoc.folderName}
                      </span>
                    ) : (
                      <span className="italic font-mono text-[10px] text-brand-muted">Racine</span>
                    )}
                  </td>

                  {/* Owner */}
                  <td className="text-xs text-brand-text font-mono">
                    {searchDoc.ownerUsername || 'Agent AWS'}
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
                            className="inline-flex items-center gap-0.5 px-1 py-0.2 text-[9px] font-mono bg-brand-alt border border-brand-border text-brand-text"
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
                      <a
                        href={documentApi.previewUrl(doc.id)}
                        target="_blank"
                        rel="noreferrer"
                        className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-none"
                        title="Aperçu rapide"
                      >
                        <Eye className="w-3.5 h-3.5" />
                      </a>

                      {/* Download Button */}
                      <a
                        href={documentApi.downloadUrl(doc.id)}
                        download
                        className="p-1 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light border border-transparent hover:border-brand-primary/30 rounded-none"
                        title="Télécharger"
                      >
                        <Download className="w-3.5 h-3.5" />
                      </a>

                      {/* Checkout / Lock toggle */}
                      {doc.isLocked ? (
                        <button
                          onClick={() => onCheckinDocument(doc.id)}
                          className="p-1 text-red-700 hover:bg-red-50 border border-transparent hover:border-red-300 rounded-none"
                          title="Déverrouiller (Check-in)"
                        >
                          <Lock className="w-3.5 h-3.5" />
                        </button>
                      ) : (
                        <button
                          onClick={() => onCheckoutDocument(doc.id)}
                          className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-none"
                          title="Verrouiller (Check-out)"
                        >
                          <Lock className="w-3.5 h-3.5 opacity-40" />
                        </button>
                      )}

                      {/* Delete */}
                      <button
                        onClick={() => onDeleteDocument(doc.id)}
                        className="p-1 text-brand-muted hover:text-red-700 hover:bg-red-50 border border-transparent hover:border-red-200 rounded-none"
                        title="Mettre en corbeille"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>

                      {/* More Details */}
                      <button
                        onClick={() => onSelectDocument(doc)}
                        className="p-1 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-transparent hover:border-brand-border rounded-none"
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
