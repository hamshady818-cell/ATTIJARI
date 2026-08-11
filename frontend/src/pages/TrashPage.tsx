import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { trashApi } from '../api/trashApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import {
  Trash2,
  RotateCcw,
  FileText,
  Folder,
  ChevronLeft,
  ChevronRight,
  Loader2,
} from 'lucide-react';

export const TrashPage: React.FC = () => {
  const queryClient = useQueryClient();

  // State
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);
  const [restoringId, setRestoringId] = useState<string | null>(null);

  // Fetch paginated trash data
  const { data: pageData, isLoading, isFetching } = useQuery({
    queryKey: ['trash-documents', page, pageSize],
    queryFn: () => trashApi.getTrash(page, pageSize),
  });

  const trashedItems = pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;
  const isFirst = pageData?.first ?? page === 0;
  const isLast = pageData?.last ?? (totalPages === 0 || page >= totalPages - 1);

  // Pagination calculation
  const startItem = totalElements === 0 ? 0 : page * pageSize + 1;
  const endItem = Math.min((page + 1) * pageSize, totalElements);

  // Handle restoration
  const handleRestore = async (id: string) => {
    try {
      setRestoringId(id);
      await trashApi.restore(id);
      await queryClient.invalidateQueries({ queryKey: ['trash-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });

      // Edge case: if restoring last item on page > 0, go back to previous page
      if (trashedItems.length === 1 && page > 0) {
        setPage((prev) => prev - 1);
      }
    } catch (err: any) {
      alert('Erreur lors de la restauration: ' + (err.response?.data?.message || err.message));
    } finally {
      setRestoringId(null);
    }
  };

  // Handle page size selection
  const handlePageSizeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSize = Number(e.target.value);
    setPageSize(newSize);
    setPage(0); // Always reset to page 0 on size change
  };

  // Helper for generating pagination page numbers
  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    if (totalPages <= 7) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else {
      pages.push(0);
      if (page > 2) pages.push('...');

      const start = Math.max(1, page - 1);
      const end = Math.min(totalPages - 2, page + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (page < totalPages - 3) pages.push('...');
      pages.push(totalPages - 1);
    }
    return pages;
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Banner Header */}
        <div className="flex items-center justify-between bg-brand-surface p-4 border border-brand-border rounded-lg shadow-card">
          <div>
            <h1 className="text-base font-bold uppercase tracking-wider text-brand-text flex items-center gap-2.5">
              <Trash2 className="w-5 h-5 text-brand-primary" />
              Corbeille des Documents
            </h1>
            <p className="text-xs text-brand-muted mt-0.5">
              Documents et éléments supprimés temporairement. Vous pouvez les restaurer vers leur emplacement d'origine.
            </p>
          </div>
          {isFetching && !isLoading && (
            <div className="flex items-center gap-2 text-xs text-brand-primary font-medium">
              <Loader2 className="w-4 h-4 animate-spin" />
              Mise à jour...
            </div>
          )}
        </div>

        {/* Content Table Card */}
        <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card overflow-hidden flex flex-col">
          <div className="overflow-x-auto">
            <table className="w-full text-left table-dense">
              <thead>
                <tr>
                  <th>Élément</th>
                  <th>Statut</th>
                  <th>Propriétaire</th>
                  <th>Date de suppression</th>
                  <th className="text-right">Restauration</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border font-sans">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-12 text-brand-muted">
                      <div className="flex items-center justify-center gap-2">
                        <Loader2 className="w-5 h-5 text-brand-primary animate-spin" />
                        <span>Chargement de la corbeille...</span>
                      </div>
                    </td>
                  </tr>
                ) : trashedItems.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center py-12 text-brand-muted italic">
                      La corbeille est vide. Aucun document supprimé.
                    </td>
                  </tr>
                ) : (
                  trashedItems.map((item) => (
                    <tr key={item.id} className="hover:bg-red-50/20 transition-colors">
                      <td>
                        <div className="flex items-center gap-2.5">
                          {item.entityType === 'FOLDER' ? (
                            <Folder className="w-4 h-4 text-amber-500 shrink-0" />
                          ) : (
                            <FileText className="w-4 h-4 text-brand-primary shrink-0" />
                          )}
                          <span className="font-medium text-xs text-brand-text">
                            {item.name || 'Élément sans nom'}
                          </span>
                        </div>
                      </td>
                      <td>
                        <Badge status="TRASHED" />
                      </td>
                      <td className="font-mono text-xs text-brand-muted">
                        {item.ownerUsername || 'Agent GED'}
                      </td>
                      <td className="font-mono text-xs text-brand-muted">
                        {item.deletedAt ? new Date(item.deletedAt).toLocaleString('fr-FR') : '-'}
                      </td>
                      <td className="text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={restoringId === item.id}
                          icon={
                            restoringId === item.id ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <RotateCcw className="w-3.5 h-3.5" />
                            )
                          }
                          onClick={() => handleRestore(item.id)}
                        >
                          {restoringId === item.id ? 'Restauration...' : 'Restaurer'}
                        </Button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalElements > 0 && (
            <div className="p-4 border-t border-brand-border bg-slate-50/50 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-brand-muted">
              {/* Information Text */}
              <div className="flex items-center gap-4">
                <span>
                  Affichage de <strong className="text-brand-text">{startItem}</strong> à{' '}
                  <strong className="text-brand-text">{endItem}</strong> sur{' '}
                  <strong className="text-brand-text">{totalElements}</strong> documents
                </span>
                <span className="hidden sm:inline text-slate-300">|</span>
                <span>
                  Page <strong className="text-brand-text">{page + 1}</strong> sur{' '}
                  <strong className="text-brand-text">{totalPages}</strong>
                </span>
              </div>

              {/* Controls */}
              <div className="flex items-center gap-4">
                {/* Page Size Selector */}
                <div className="flex items-center gap-2">
                  <span>Documents par page :</span>
                  <select
                    value={pageSize}
                    onChange={handlePageSizeChange}
                    className="bg-white border border-brand-border rounded px-2 py-1 text-xs font-medium text-brand-text focus:outline-none focus:ring-1 focus:ring-brand-primary"
                  >
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                  </select>
                </div>

                {/* Page Navigation Buttons */}
                <div className="flex items-center gap-1">
                  {/* Previous Button */}
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={isFirst || isLoading}
                    className={`flex items-center gap-1 px-2.5 py-1 rounded border text-xs font-medium transition-colors ${
                      isFirst || isLoading
                        ? 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'
                        : 'bg-white text-brand-text border-brand-border hover:bg-slate-100 hover:text-brand-primary'
                    }`}
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                    <span>Précédent</span>
                  </button>

                  {/* Direct Page Numbers */}
                  <div className="flex items-center gap-1">
                    {getPageNumbers().map((pNum, idx) =>
                      typeof pNum === 'number' ? (
                        <button
                          key={idx}
                          onClick={() => setPage(pNum)}
                          disabled={isLoading}
                          className={`w-7 h-7 rounded text-xs font-semibold flex items-center justify-center transition-colors ${
                            page === pNum
                              ? 'bg-brand-primary text-white shadow-sm'
                              : 'bg-white text-brand-text border border-brand-border hover:bg-red-50 hover:text-brand-primary'
                          }`}
                        >
                          {pNum + 1}
                        </button>
                      ) : (
                        <span key={idx} className="px-1 text-slate-400">
                          {pNum}
                        </span>
                      )
                    )}
                  </div>

                  {/* Next Button */}
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={isLast || isLoading}
                    className={`flex items-center gap-1 px-2.5 py-1 rounded border text-xs font-medium transition-colors ${
                      isLast || isLoading
                        ? 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'
                        : 'bg-white text-brand-text border-brand-border hover:bg-slate-100 hover:text-brand-primary'
                    }`}
                  >
                    <span>Suivant</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};
