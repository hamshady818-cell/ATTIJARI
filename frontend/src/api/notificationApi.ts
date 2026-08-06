import { apiClient } from './client';
import { NotificationItem } from '../types';

export const notificationApi = {
  /**
   * Récupère la liste des notifications de l'utilisateur courant.
   * Retourne des données de démonstration en cas d'erreur (endpoint optionnel).
   */
  list: async (): Promise<NotificationItem[]> => {
    try {
      const res = await apiClient.get<NotificationItem[]>('/notifications');
      return res.data;
    } catch {
      // Endpoint non encore déployé : retourner des données de démonstration
      return getDemoNotifications();
    }
  },

  /**
   * Marque une notification spécifique comme lue.
   */
  markAsRead: async (id: string): Promise<void> => {
    try {
      await apiClient.patch(`/notifications/${id}/read`);
    } catch {
      // Silently fail if endpoint not available
    }
  },

  /**
   * Marque toutes les notifications comme lues.
   */
  markAllAsRead: async (): Promise<void> => {
    try {
      await apiClient.patch('/notifications/read-all');
    } catch {
      // Silently fail if endpoint not available
    }
  },
};

/**
 * Données de démonstration pour afficher le panel quand le backend
 * n'a pas encore d'endpoint /notifications.
 */
function getDemoNotifications(): NotificationItem[] {
  return [
    {
      id: 'demo-1',
      type: 'DOCUMENT_UPLOADED',
      message: 'Nouveau document "Rapport Q2 2026.pdf" versé dans Finances.',
      relatedDocumentName: 'Rapport Q2 2026.pdf',
      read: false,
      createdAt: new Date(Date.now() - 5 * 60_000).toISOString(),
    },
    {
      id: 'demo-2',
      type: 'CHECKOUT_REQUESTED',
      message: 'Document "Contrat fournisseur" verrouillé par Ahmed K.',
      relatedDocumentName: 'Contrat fournisseur',
      read: false,
      createdAt: new Date(Date.now() - 30 * 60_000).toISOString(),
    },
    {
      id: 'demo-3',
      type: 'DOCUMENT_UPDATED',
      message: 'Statut du document "Politique RH v3" changé → PUBLIÉ.',
      relatedDocumentName: 'Politique RH v3',
      read: true,
      createdAt: new Date(Date.now() - 2 * 3_600_000).toISOString(),
    },
    {
      id: 'demo-4',
      type: 'SYSTEM',
      message: 'Sauvegarde automatique effectuée avec succès.',
      read: true,
      createdAt: new Date(Date.now() - 24 * 3_600_000).toISOString(),
    },
  ];
}
