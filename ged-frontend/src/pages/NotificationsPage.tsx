import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { toast } from '../components/ui/Toast';
import { Bell, Check, Calendar, ArrowRight } from 'lucide-react';
import type { NotificationResponseDto, ApiErrorResponse } from '../types';
import { mapErrorCodeToMessage } from '../api/client';
import { useNavigate } from 'react-router-dom';

export const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Query notifications
  const { data: notifications = [], isLoading } = useQuery<NotificationResponseDto[]>({
    queryKey: ['notifications'],
    queryFn: async () => {
      const res = await api.get('/notifications');
      return res.data;
    },
  });

  // Mark as read mutation
  const readMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/notifications/${id}/read`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Notification marquée comme lue.');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de modifier le statut.');
    },
  });

  const handleEntityClick = (entityType: string, entityId: string) => {
    if (entityType === 'DOCUMENT') {
      navigate(`/documents/${entityId}`);
    } else if (entityType === 'FOLDER') {
      navigate(`/folders/${entityId}`);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2 select-none">
          <Bell className="h-5 w-5 text-brand" />
          Mes Notifications
        </h1>
        <p className="text-xs text-gray-400">
          Recevez des alertes concernant les documents partagés et les activités de votre département.
        </p>
      </div>

      {isLoading ? (
        <div className="py-12 flex flex-col items-center justify-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brand" />
          <span className="text-xs text-gray-400">Chargement des notifications...</span>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded shadow-xs overflow-hidden">
          {notifications.length === 0 ? (
            <div className="py-12 text-center text-gray-400 font-medium text-xs">
              Aucune notification pour le moment.
            </div>
          ) : (
            <div className="divide-y divide-gray-150">
              {notifications.map((notif) => {
                const isUnread = notif.status !== 'READ';
                return (
                  <div
                    key={notif.id}
                    className={`flex flex-col sm:flex-row sm:items-start justify-between px-6 py-5 transition-colors gap-4 ${
                      isUnread ? 'bg-brand/5/10 bg-brand/[0.02] border-l-2 border-brand -ml-[2px]' : ''
                    }`}
                  >
                    <div className="space-y-1.5 flex-1 text-left">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-bold text-gray-800 text-xs">{notif.title}</span>
                        {isUnread && (
                          <Badge variant="primary">Nouveau</Badge>
                        )}
                        <Badge variant="secondary">{notif.type}</Badge>
                      </div>

                      <p className="text-xs text-gray-600 leading-relaxed">{notif.body}</p>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[10px] text-gray-400 mt-2">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3.5 w-3.5" />
                          Reçu le : {new Date(notif.sentAt).toLocaleString('fr-FR')}
                        </span>
                        {notif.entityId && (
                          <button
                            onClick={() => handleEntityClick(notif.entityType, notif.entityId)}
                            className="text-brand hover:underline font-semibold flex items-center gap-0.5 cursor-pointer"
                          >
                            Voir l'élément lié <ArrowRight className="h-3 w-3" />
                          </button>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-start">
                      {isUnread && (
                        <Button
                          variant="outline"
                          size="sm"
                          className="gap-1 px-2.5 py-1 text-xs"
                          onClick={() => readMutation.mutate(notif.id)}
                          isLoading={readMutation.isPending && readMutation.variables === notif.id}
                        >
                          <Check className="h-3.5 w-3.5" /> Marquer lu
                        </Button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
