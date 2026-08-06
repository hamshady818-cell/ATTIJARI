import React, { useState, useEffect, useRef } from 'react';
import { DocumentItem, DocumentSearchResult } from '../../types';
import { documentApi } from '../../api/documentApi';
import { renderAsync } from 'docx-preview';
import * as XLSX from 'xlsx';
import {
  X,
  Download,
  Eye,
  FileText,
  Image as ImageIcon,
  FileSpreadsheet,
  FileCode,
  File,
  ExternalLink,
  AlertCircle,
  Loader2,
  Search,
  Maximize2,
  Minimize2,
} from 'lucide-react';
import { Button } from '../ui/Button';

interface DocumentPreviewModalProps {
  document: DocumentItem | DocumentSearchResult | null;
  onClose: () => void;
}

export const DocumentPreviewModal: React.FC<DocumentPreviewModalProps> = ({
  document,
  onClose,
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [textContent, setTextContent] = useState<string>('');
  const [docxBuffer, setDocxBuffer] = useState<ArrayBuffer | null>(null);
  
  // XLSX State
  const [sheets, setSheets] = useState<{ name: string; html: string }[]>([]);
  const [activeSheetIndex, setActiveSheetIndex] = useState(0);
  const [sheetSearch, setSheetSearch] = useState('');

  // Fullscreen toggle state
  const [isFullscreen, setIsFullscreen] = useState(false);

  const docxContainerRef = useRef<HTMLDivElement>(null);

  if (!document) return null;

  const mimeType = (document.mimeType || '').toLowerCase();
  const name = document.name.toLowerCase();

  // Strict format detection — DOCX, XLSX, PDF, Image take priority over Text!
  const isPdf = mimeType.includes('pdf') || name.endsWith('.pdf');
  const isImage = mimeType.includes('image') || /\.(png|jpe?g|gif|webp|tiff?|bmp)$/i.test(name);
  const isDocx = mimeType.includes('wordprocessingml') || mimeType.includes('msword') || /\.(docx?)$/i.test(name);
  const isXlsx = mimeType.includes('spreadsheetml') || mimeType.includes('excel') || /\.(xlsx?|csv)$/i.test(name);
  
  // isText ONLY matches explicit text formats when NOT a docx, xlsx, pdf or image
  const isText =
    !isPdf &&
    !isImage &&
    !isDocx &&
    !isXlsx &&
    (mimeType.startsWith('text/') ||
      mimeType.includes('json') ||
      /\.(txt|log|md|json|xml|html?)$/i.test(name));

  const [previewBlobUrl, setPreviewBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    let createdUrl: string | null = null;

    setLoading(true);
    setError(null);
    setTextContent('');
    setDocxBuffer(null);
    setSheets([]);
    setPreviewBlobUrl(null);

    async function loadContent() {
      if (!document) return;
      try {
        // Fetch binary/stream blob via apiClient (includes Keycloak Bearer Authorization token)
        const blob = await documentApi.fetchPreviewBlob(document.id);

        if (!isMounted) return;

        // Generate local Object URL for iframe / img display
        createdUrl = URL.createObjectURL(blob);
        setPreviewBlobUrl(createdUrl);

        if (isDocx) {
          const arrayBuffer = await blob.arrayBuffer();
          if (isMounted) setDocxBuffer(arrayBuffer);
        } else if (isXlsx) {
          const arrayBuffer = await blob.arrayBuffer();
          const workbook = XLSX.read(arrayBuffer, { type: 'array' });
          const parsedSheets = workbook.SheetNames.map((sheetName) => {
            const worksheet = workbook.Sheets[sheetName];
            const html = XLSX.utils.sheet_to_html(worksheet, { id: `sheet-${sheetName}` });
            return { name: sheetName, html };
          });
          if (isMounted) {
            setSheets(parsedSheets);
            setActiveSheetIndex(0);
          }
        } else if (isText) {
          const text = await blob.text();
          if (isMounted) setTextContent(text);
        }

        if (isMounted) setLoading(false);
      } catch (err: any) {
        if (isMounted) {
          const msg = err.response?.data?.message || err.message || 'Impossible de charger l\'aperçu du document';
          setError(msg);
          setLoading(false);
        }
      }
    }

    loadContent();

    return () => {
      isMounted = false;
      if (createdUrl) {
        URL.revokeObjectURL(createdUrl);
      }
    };
  }, [document.id, isPdf, isImage, isText, isDocx, isXlsx]);

  // Render DOCX with docx-preview when ArrayBuffer and container DOM ref are ready
  useEffect(() => {
    if (isDocx && docxBuffer && docxContainerRef.current) {
      docxContainerRef.current.innerHTML = '';
      renderAsync(docxBuffer, docxContainerRef.current, undefined, {
        className: 'docx-preview-content',
        inWrapper: true,
        ignoreWidth: false,
        ignoreHeight: false,
      }).catch((err) => {
        console.error('Docx render error:', err);
        setError('Erreur lors du rendu du document Word');
      });
    }
  }, [isDocx, docxBuffer, loading]);

  // Handle Secure Download Action via apiClient + Keycloak Token
  const handleDownload = async () => {
    try {
      await documentApi.downloadFile(document.id, document.name);
    } catch (err: any) {
      alert('Erreur lors du téléchargement: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4 animate-in fade-in duration-200">
      <div
        className={`bg-brand-surface border border-brand-border shadow-popover flex flex-col transition-all duration-200 ${
          isFullscreen ? 'w-screen h-screen rounded-none' : 'w-full max-w-5xl h-[88vh] rounded-sm'
        }`}
      >
        {/* Modal Header */}
        <div className="p-3 bg-brand-alt border-b border-brand-border flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3 min-w-0">
            <div className="p-1.5 bg-brand-surface border border-brand-border shrink-0">
              {isPdf && <FileText className="w-4 h-4 text-brand-primary" />}
              {isImage && <ImageIcon className="w-4 h-4 text-brand-secondary" />}
              {isXlsx && <FileSpreadsheet className="w-4 h-4 text-emerald-700" />}
              {isDocx && <FileText className="w-4 h-4 text-brand-primary" />}
              {isText && <FileCode className="w-4 h-4 text-brand-secondary" />}
              {!isPdf && !isImage && !isXlsx && !isDocx && !isText && <File className="w-4 h-4 text-brand-muted" />}
            </div>

            <div className="min-w-0">
              <h3 className="font-bold text-xs uppercase tracking-wider text-brand-text truncate">
                {document.name}
              </h3>
              <span className="font-mono text-[10px] text-brand-muted block truncate">
                {document.mimeType || 'Format standard'}
              </span>
            </div>
          </div>

          {/* Controls Header Right */}
          <div className="flex items-center gap-2 shrink-0">
            {/* Download Button */}
            <Button
              variant="outline"
              size="sm"
              icon={<Download className="w-3.5 h-3.5" />}
              onClick={handleDownload}
            >
              Télécharger
            </Button>

            {/* Open in new tab (PDF / Images) */}
            {(isPdf || isImage) && previewBlobUrl && (
              <a href={previewBlobUrl} target="_blank" rel="noreferrer">
                <Button variant="outline" size="sm" icon={<ExternalLink className="w-3.5 h-3.5" />}>
                  Onglet
                </Button>
              </a>
            )}

            {/* Fullscreen Toggle */}
            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-border rounded-sm transition-colors"
              title={isFullscreen ? 'Réduire' : 'Plein écran'}
            >
              {isFullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
            </button>

            {/* Close Button */}
            <button
              onClick={onClose}
              className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-border rounded-sm transition-colors"
              title="Fermer"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-hidden relative bg-brand-bg flex flex-col">
          {/* Loading Indicator */}
          {loading && (
            <div className="absolute inset-0 z-20 bg-brand-surface/90 flex flex-col items-center justify-center gap-2">
              <Loader2 className="w-8 h-8 text-brand-primary animate-spin" />
              <span className="text-xs font-semibold text-brand-text">Chargement de l'aperçu...</span>
            </div>
          )}

          {/* Error View */}
          {error && (
            <div className="flex-1 flex flex-col items-center justify-center p-6 text-center space-y-3 bg-brand-surface">
              <AlertCircle className="w-10 h-10 text-brand-primary" />
              <h4 className="font-bold text-sm text-brand-text">Impossible d'afficher l'aperçu</h4>
              <p className="text-xs text-brand-muted max-w-md">{error}</p>
              <Button variant="primary" size="sm" icon={<Download className="w-4 h-4" />} onClick={handleDownload}>
                Télécharger le fichier à la place
              </Button>
            </div>
          )}

          {/* 1. PDF Preview via local Blob URL */}
          {!loading && !error && isPdf && previewBlobUrl && (
            <iframe
              src={previewBlobUrl}
              title={document.name}
              className="w-full h-full border-0 bg-brand-surface"
            />
          )}

          {/* 2. Image Preview via local Blob URL */}
          {!loading && !error && isImage && previewBlobUrl && (
            <div className="flex-1 overflow-auto p-4 flex items-center justify-center bg-brand-text">
              <img
                src={previewBlobUrl}
                alt={document.name}
                className="max-h-full max-w-full object-contain shadow-2xl rounded-sm"
              />
            </div>
          )}

          {/* 3. Text / Code Preview */}
          {!loading && !error && isText && (
            <div className="flex-1 overflow-auto p-4 bg-brand-text text-emerald-400 font-mono text-xs leading-relaxed selection:bg-brand-primary selection:text-white">
              <pre className="whitespace-pre-wrap break-words">{textContent}</pre>
            </div>
          )}

          {/* 4. DOCX Preview (Always rendered when isDocx is true so ref is mounted) */}
          {!error && isDocx && (
            <div className={`flex-1 overflow-auto p-6 bg-brand-bg flex justify-center ${loading ? 'hidden' : 'block'}`}>
              <div
                ref={docxContainerRef}
                className="bg-brand-surface shadow-popover p-8 max-w-4xl w-full min-h-full font-serif text-brand-text text-sm leading-normal space-y-4 docx-wrapper"
              />
            </div>
          )}

          {/* 5. XLSX Preview */}
          {!loading && !error && isXlsx && (
            <div className="flex-1 flex flex-col overflow-hidden bg-brand-surface">
              {/* Sheet Tabs Header */}
              {sheets.length > 0 && (
                <div className="flex items-center justify-between bg-brand-alt border-b border-brand-border px-3 py-1 text-xs shrink-0 overflow-x-auto">
                  <div className="flex items-center gap-1">
                    {sheets.map((sheet, idx) => (
                      <button
                        key={sheet.name}
                        onClick={() => setActiveSheetIndex(idx)}
                        className={`px-3 py-1 font-mono text-xs border transition-colors ${
                          activeSheetIndex === idx
                            ? 'bg-brand-primary text-white font-bold border-brand-primary'
                            : 'bg-brand-surface text-brand-text border-brand-border hover:bg-brand-bg'
                        }`}
                      >
                        {sheet.name}
                      </button>
                    ))}
                  </div>

                  <div className="relative flex items-center max-w-xs">
                    <Search className="w-3.5 h-3.5 absolute left-2 text-brand-muted pointer-events-none" />
                    <input
                      type="text"
                      placeholder="Filtrer dans la feuille..."
                      value={sheetSearch}
                      onChange={(e) => setSheetSearch(e.target.value)}
                      className="bg-brand-surface border border-brand-border text-xs text-brand-text pl-7 pr-2 py-0.5 rounded-sm focus:outline-none focus:border-brand-primary"
                    />
                  </div>
                </div>
              )}

              {/* Sheet HTML Table View */}
              <div className="flex-1 overflow-auto p-4 font-mono text-xs">
                {sheets.length > 0 ? (
                  <div
                    className="xlsx-preview-table"
                    dangerouslySetInnerHTML={{
                      __html: sheetSearch
                        ? filterTableHtml(sheets[activeSheetIndex]?.html || '', sheetSearch)
                        : sheets[activeSheetIndex]?.html || '<p className="text-brand-muted">Feuille vide</p>',
                    }}
                  />
                ) : (
                  <div className="p-8 text-center text-brand-muted italic">Feuille de calcul vide</div>
                )}
              </div>
            </div>
          )}

          {/* 6. Unsupported Format Fallback */}
          {!loading && !error && !isPdf && !isImage && !isText && !isDocx && !isXlsx && (
            <div className="flex-1 flex flex-col items-center justify-center p-8 bg-brand-surface text-center space-y-4">
              <File className="w-12 h-12 text-brand-muted" />
              <div className="space-y-1">
                <h4 className="font-bold text-sm text-brand-text">Aperçu non disponible</h4>
                <p className="text-xs text-brand-muted max-w-sm">
                  Le format ({document.mimeType || 'inconnu'}) ne peut pas être prévisualisé directement.
                </p>
              </div>
              <Button variant="primary" size="sm" icon={<Download className="w-4 h-4" />} onClick={handleDownload}>
                Télécharger le document
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

/**
 * Utility to filter table rows by search keyword
 */
function filterTableHtml(html: string, keyword: string): string {
  if (!keyword.trim()) return html;
  const lowerKey = keyword.toLowerCase();
  
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, 'text/html');
  const table = doc.querySelector('table');
  if (!table) return html;

  const rows = Array.from(table.querySelectorAll('tr'));
  rows.forEach((row, index) => {
    if (index === 0) return; // Keep header
    const text = row.textContent || '';
    if (!text.toLowerCase().includes(lowerKey)) {
      row.style.display = 'none';
    }
  });

  return table.outerHTML;
}
