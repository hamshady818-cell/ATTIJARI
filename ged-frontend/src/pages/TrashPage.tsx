import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { toast } from '../components/ui/Toast';
import { Trash2, RotateCcw, Calendar, Folder, FileText } from 'lucide-react';
import type { TrashItemResponseDto, ApiErrorResponse } from '../types';
import { mapErrorCodeToMessage } from '../api/client';

export const TrashPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Query soft-deleted items
  const { data: trashItems = [], isLoading } = useQuery<TrashItemResponseDto[]>({
    queryKey: ['trash'],
    queryFn: async () => {
      const res = await api.get('/trash');
      return res.data;
    },
  });

  // Restore mutation
  const restoreMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/trash/${id}/restore`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['trash'] });
      queryClient.invalidateQueries({ queryKey: ['folderContent'] });
      toast.success('L’élément a été restauré dans son emplacement d’origine.');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de restaurer l\'élément.');
    },
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2 select-none">
          <Trash2 className="h-5 w-5 text-gray-500" />
          Corbeille
        </h1>
        <p className="text-xs text-gray-400">
          Consultez et restaurez les dossiers et documents supprimés. Les éléments sont purgés automatiquement après expiration.
        </p>
      </div>

      {isLoading ? (
        <div className="py-12 flex flex-col items-center justify-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brand" />
          <span className="text-xs text-gray-400">Chargement de la corbeille...</span>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded shadow-xs overflow-hidden">
          {trashItems.length === 0 ? (
            <div className="py-12 text-center text-gray-400 font-medium text-xs">
              La corbeille est vide.
            </div>
          ) : (
            <div className="divide-y divide-gray-150">
              {trashItems.map((item) => (
                <div
                  key={item.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between px-6 py-4 hover:bg-gray-50/70 transition-colors gap-4"
                >
                  <div className="flex items-start gap-3">
                    {item.entityType === 'FOLDER' ? (
                      <Folder className="h-5 w-5 text-yellow-500 fill-yellow-500/10 mt-0.5 flex-shrink-0" />
                    ) : (
                      <FileText className="h-5 w-5 text-gray-400 mt-0.5 flex-shrink-0" />
                    )}
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-semibold text-gray-800 text-xs">
                          {item.entityType === 'FOLDER' ? 'Dossier' : 'Document'}
                        </span>
                        <Badge variant="secondary">{item.entityId}</Badge>
                      </div>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-[10px] text-gray-400">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3.5 w-3.5" />
                          Supprimé le : {new Date(item.deletedAt).toLocaleDateString('fr-FR')}
                        </span>
                        {item.autoPurgeAt && (
                          <span className="text-red-500 font-medium">
                            Purge automatique : {new Date(item.autoPurgeAt).toLocaleDateString('fr-FR')}
                          </span>
                        )}
                        <span>Par : {item.deletedBy}</span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center">
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5"
                      onClick={() => restoreMutation.mutate(item.id)}
                      isLoading={restoreMutation.isPending && restoreMutation.variables === item.id}
                    >
                      <RotateCcw className="h-3.5 w-3.5" />
                      Restaurer
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
