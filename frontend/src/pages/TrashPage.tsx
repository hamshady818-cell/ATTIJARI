import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { documentApi } from '../api/documentApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Trash2, RotateCcw, FileText } from 'lucide-react';

export const TrashPage: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: trashedDocs = [], refetch } = useQuery({
    queryKey: ['trashed-documents'],
    queryFn: async () => {
      const page = await documentApi.search({ status: 'TRASHED', size: 100 });
      return page.content;
    },
  });

  const handleRestore = async (id: string) => {
    try {
      await documentApi.updateStatus(id, 'DRAFT');
      await queryClient.invalidateQueries({ queryKey: ['trashed-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      refetch();
    } catch (err: any) {
      alert('Erreur lors de la restauration: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        <div className="flex items-center justify-between bg-brand-surface p-4 border border-brand-border rounded-lg shadow-card">
          <div>
            <h1 className="text-base font-bold uppercase tracking-wider text-brand-text flex items-center gap-2.5">
              <Trash2 className="w-5 h-5 text-brand-primary" />
              Corbeille des Documents
            </h1>
            <p className="text-xs text-brand-muted mt-0.5">
              Documents supprimés temporairement. Vous pouvez les restaurer vers l'état Brouillon.
            </p>
          </div>
        </div>

        <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card overflow-hidden">
          <table className="w-full text-left table-dense">
            <thead>
              <tr>
                <th>Document</th>
                <th>Statut</th>
                <th>Propriétaire</th>
                <th>Date</th>
                <th className="text-right">Restauration</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-border font-sans">
              {trashedDocs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-brand-muted italic">
                    La corbeille est vide. Aucun document supprimé.
                  </td>
                </tr>
              ) : (
                trashedDocs.map((doc) => (
                  <tr key={doc.id}>
                    <td>
                      <div className="flex items-center gap-2.5">
                        <FileText className="w-4 h-4 text-brand-primary shrink-0" />
                        <span className="font-medium text-xs text-brand-text">{doc.name}</span>
                      </div>
                    </td>
                    <td>
                      <Badge status={doc.status} />
                    </td>
                    <td className="font-mono text-xs text-brand-muted">{doc.ownerUsername || 'Agent GED'}</td>
                    <td className="font-mono text-xs text-brand-muted">
                      {new Date(doc.updatedAt).toLocaleString('fr-FR')}
                    </td>
                    <td className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        icon={<RotateCcw className="w-3.5 h-3.5" />}
                        onClick={() => handleRestore(doc.id)}
                      >
                        Restaurer
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
};
