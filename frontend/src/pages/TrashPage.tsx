import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { trashApi } from '../api/trashApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Pagination } from '../components/ui/Pagination';
import {
  Trash2,
  RotateCcw,
  FileText,
  Folder,
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

          <Pagination
            page={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            isFirst={isFirst}
            isLast={isLast}
            isLoading={isLoading}
            onPageChange={setPage}
            onPageSizeChange={(size) => { setPageSize(size); setPage(0); }}
            label="documents"
          />
        </div>
      </main>
    </div>
  );
};
