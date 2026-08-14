import { apiClient } from './client';
import { NotificationItem } from '../types';

/** Raw shape returned by GET /api/v1/notifications (matches NotificationResponseDto). */
interface NotificationResponseDto {
  id: string;
  type: string;
  title: string;
  body?: string;
  entityType?: string;
  entityId?: string;
  channel?: string;
  status: string;
  readAt?: string;
  sentAt?: string;
  createdAt: string;
}

/** Maps the backend DTO to the frontend NotificationItem, deriving the `read` boolean. */
function mapDto(dto: NotificationResponseDto): NotificationItem {
  return {
    id: dto.id,
    type: dto.type as NotificationItem['type'],
    title: dto.title,
    body: dto.body,
    entityType: dto.entityType,
    entityId: dto.entityId,
    channel: dto.channel,
    status: dto.status as NotificationItem['status'],
    readAt: dto.readAt,
    sentAt: dto.sentAt,
    createdAt: dto.createdAt,
    read: dto.status === 'READ',
  };
}

export const notificationApi = {
  /**
   * Récupère la liste des notifications de l'utilisateur courant.
   * En cas d'erreur (réseau, auth, etc.), retourne un tableau vide
   * et loggue l'erreur dans la console — ne masque plus les vrais problèmes backend.
   */
  list: async (): Promise<NotificationItem[]> => {
    try {
      const res = await apiClient.get<NotificationResponseDto[]>('/notifications');
      return res.data.map(mapDto);
    } catch (err) {
      console.error('[notificationApi.list] Failed to fetch notifications:', err);
      return [];
    }
  },

  /**
   * Marque une notification spécifique comme lue.
   * Propage l'erreur — l'appelant (useMutation) gère le cas d'échec.
   */
  markAsRead: async (id: string): Promise<void> => {
    await apiClient.patch(`/notifications/${id}/read`);
  },

  /**
   * Marque toutes les notifications comme lues.
   * Propage l'erreur — l'appelant (useMutation) gère le cas d'échec.
   */
  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch('/notifications/read-all');
  },
};
