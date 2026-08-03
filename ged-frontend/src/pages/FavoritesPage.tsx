import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { explorerApi } from '../features/documents/explorerApi';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { toast } from '../components/ui/Toast';
import { useNavigate } from 'react-router-dom';
import { Star, Folder, FileText, Trash2, ArrowRight } from 'lucide-react';
import type { ApiErrorResponse } from '../types';
import { mapErrorCodeToMessage } from '../api/client';

export const FavoritesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: favorites = [], isLoading } = useQuery({
    queryKey: ['favorites'],
    queryFn: explorerApi.getFavorites,
  });

  const removeFavoriteMutation = useMutation({
    mutationFn: explorerApi.removeFavorite,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      toast.success('Retiré des favoris.');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de retirer des favoris.');
    },
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2 select-none">
          <Star className="h-5 w-5 text-yellow-400 fill-current" />
          Mes Favoris
        </h1>
        <p className="text-xs text-gray-400">
          Retrouvez ici tous les dossiers et documents que vous avez marqués d'une étoile.
        </p>
      </div>

      {isLoading ? (
        <div className="py-12 flex flex-col items-center justify-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brand" />
          <span className="text-xs text-gray-400">Chargement des favoris...</span>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded shadow-xs overflow-hidden">
          {favorites.length === 0 ? (
            <div className="py-12 text-center text-gray-400 font-medium text-xs">
              Aucun favori pour le moment.
            </div>
          ) : (
            <div className="divide-y divide-gray-150">
              {favorites.map((fav) => (
                <div
                  key={fav.id}
                  className="flex items-center justify-between px-6 py-4 hover:bg-gray-50/70 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    {fav.entityType === 'FOLDER' ? (
                      <Folder className="h-5 w-5 text-yellow-500 fill-yellow-500/10 flex-shrink-0" />
                    ) : (
                      <FileText className="h-5 w-5 text-gray-400 flex-shrink-0" />
                    )}
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-gray-800 text-xs">
                          {fav.entityType === 'FOLDER' ? 'Dossier' : 'Document'}
                        </span>
                        <Badge variant={fav.entityType === 'FOLDER' ? 'warning' : 'primary'}>
                          {fav.entityId}
                        </Badge>
                      </div>
                      <p className="text-[10px] text-gray-400 mt-1">
                        Favorisé le {new Date(fav.createdAt).toLocaleDateString('fr-FR')}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5"
                      onClick={() => {
                        if (fav.entityType === 'FOLDER') {
                          navigate(`/folders/${fav.entityId}`);
                        } else {
                          navigate(`/documents/${fav.entityId}`);
                        }
                      }}
                    >
                      Ouvrir <ArrowRight className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-red-600 hover:bg-red-50"
                      onClick={() => removeFavoriteMutation.mutate(fav.id)}
                    >
                      <Trash2 className="h-4.5 w-4.5" />
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
