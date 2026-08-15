import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { trashApi } from '../api/trashApi';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../utils/errorMessages';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Pagination } from '../components/ui/Pagination';
import {
  Trash2,
  RotateCcw,
  FileText,
  FileSpreadsheet,
  FileImage,
  FileCode,
  Files,
  Folder,
  Loader2,
  ShieldAlert,
  Calendar,
  UserCheck,
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

  const getFileIcon = (entityType?: string, name?: string) => {
    if (entityType === 'FOLDER') {
      return <Folder className="w-4 h-4 text-amber-500 shrink-0" />;
    }
    const n = name?.toLowerCase() || '';
    if (n.endsWith('.pdf')) {
      return <FileText className="w-4 h-4 text-red-500 shrink-0" />;
    }
    if (n.endsWith('.xlsx') || n.endsWith('.xls')) {
      return <FileSpreadsheet className="w-4 h-4 text-emerald-600 shrink-0" />;
    }
    if (n.endsWith('.docx') || n.endsWith('.doc')) {
      return <FileCode className="w-4 h-4 text-blue-600 shrink-0" />;
    }
    if (n.endsWith('.png') || n.endsWith('.jpg') || n.endsWith('.jpeg')) {
      return <FileImage className="w-4 h-4 text-purple-500 shrink-0" />;
    }
    return <Files className="w-4 h-4 text-brand-muted shrink-0" />;
  };

  // Handle restoration
  const handleRestore = async (id: string) => {
    try {
      setRestoringId(id);
      await trashApi.restore(id);
      await queryClient.invalidateQueries({ queryKey: ['trash-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });

      toast.success('Document restauré avec succès dans son dossier d\'origine');

      // Edge case: if restoring last item on page > 0, go back to previous page
      if (trashedItems.length === 1 && page > 0) {
        setPage((prev) => prev - 1);
      }
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la restauration du document.'));
    } finally {
      setRestoringId(null);
    }
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg text-brand-text">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Banner Hero Header */}
        <div className="relative overflow-hidden bg-gradient-to-r from-neutral-900 via-neutral-800 to-red-950 border border-neutral-700/60 rounded-xl p-6 shadow-xl text-white">
          <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-red-500/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-red-500/20 backdrop-blur-md border border-red-500/30 text-xs font-semibold text-red-300">
                <ShieldAlert className="w-3.5 h-3.5 text-red-400" />
                <span>Zone de Rétention Sécurisée</span>
              </div>
              <h1 className="text-2xl font-extrabold tracking-tight font-display text-white flex items-center gap-3">
                <Trash2 className="w-6 h-6 text-red-400" />
                Corbeille & Éléments Supprimés
              </h1>
              <p className="text-xs text-neutral-300 max-w-2xl leading-relaxed">
                Les documents et dossiers placés dans la corbeille conservent l'intégralité de leurs versions et peuvent être restaurés à tout moment vers leur emplacement d'origine.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="bg-neutral-900/80 border border-neutral-700 px-4 py-2.5 rounded-xl backdrop-blur-md shadow-inner text-right">
                <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400 block">
                  Éléments en rétention
                </span>
                <span className="text-xl font-extrabold font-mono text-red-400">
                  {totalElements} document{totalElements > 1 ? 's' : ''}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Content Table Card */}
        <div className="bg-brand-surface border border-brand-border rounded-xl shadow-card overflow-hidden flex flex-col">
          {/* Header toolbar */}
          <div className="p-4 border-b border-brand-border flex items-center justify-between bg-brand-alt/30">
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-brand-text">
              <Trash2 className="w-4 h-4 text-brand-primary" />
              <span>Liste des éléments corbeille</span>
            </div>
            {isFetching && !isLoading && (
              <div className="flex items-center gap-2 text-xs text-brand-primary font-medium">
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Actualisation...</span>
              </div>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left table-dense">
              <thead>
                <tr>
                  <th>Élément</th>
                  <th>Statut</th>
                  <th>Supprimé par</th>
                  <th>Date de suppression</th>
                  <th className="text-right">Restauration</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border font-sans">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <Loader2 className="w-6 h-6 text-brand-primary animate-spin" />
                        <span className="text-xs font-medium">Chargement de la corbeille...</span>
                      </div>
                    </td>
                  </tr>
                ) : trashedItems.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-3">
                        <div className="p-4 bg-brand-alt border border-brand-border rounded-full text-brand-muted">
                          <Trash2 className="w-8 h-8" />
                        </div>
                        <p className="text-sm font-semibold text-brand-text">La corbeille est actuellement vide</p>
                        <p className="text-xs text-brand-muted max-w-sm">
                          Aucun document ni répertoire n'a été placé dans la corbeille.
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  trashedItems.map((item) => (
                    <tr key={item.id} className="group hover:bg-red-500/5 transition-colors">
                      <td>
                        <div className="flex items-center gap-2.5">
                          {getFileIcon(item.entityType, item.name)}
                          <span className="font-medium text-xs text-brand-text group-hover:text-brand-primary transition-colors">
                            {item.name || 'Élément sans nom'}
                          </span>
                        </div>
                      </td>
                      <td>
                        <Badge status="TRASHED" />
                      </td>
                      <td>
                        <div className="flex items-center gap-1.5 font-mono text-xs text-brand-muted">
                          <UserCheck className="w-3.5 h-3.5 text-brand-muted shrink-0" />
                          <span>{item.ownerUsername || 'Agent GED'}</span>
                        </div>
                      </td>
                      <td>
                        <div className="flex items-center gap-1.5 font-mono text-xs text-brand-muted whitespace-nowrap">
                          <Calendar className="w-3.5 h-3.5 text-brand-muted shrink-0" />
                          <span>{item.deletedAt ? new Date(item.deletedAt).toLocaleString('fr-FR') : '-'}</span>
                        </div>
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
                              <RotateCcw className="w-3.5 h-3.5 text-emerald-600" />
                            )
                          }
                          onClick={() => handleRestore(item.id)}
                          className="hover:border-emerald-500 hover:bg-emerald-50 hover:text-emerald-700 transition-all"
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

          <div className="p-3 border-t border-brand-border">
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
        </div>
      </main>
    </div>
  );
};

