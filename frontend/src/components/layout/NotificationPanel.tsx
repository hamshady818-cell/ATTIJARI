import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, X, Check, CheckCheck, Upload, Edit, Trash2, Share2, Lock, Unlock, MessageSquare, Cpu } from 'lucide-react';
import { notificationApi } from '../../api/notificationApi';
import { NotificationItem, NotificationType } from '../../types';

// ─── Icon par type de notification ────────────────────────────────────────────
const NotificationIcon: React.FC<{ type: NotificationType }> = ({ type }) => {
  const props = { className: 'w-3.5 h-3.5 shrink-0' };
  switch (type) {
    case 'DOCUMENT_UPLOADED':   return <Upload {...props} className="w-3.5 h-3.5 text-brand-primary shrink-0" />;
    case 'DOCUMENT_UPDATED':    return <Edit {...props} className="w-3.5 h-3.5 text-brand-secondary shrink-0" />;
    case 'DOCUMENT_DELETED':    return <Trash2 {...props} className="w-3.5 h-3.5 text-brand-primary shrink-0" />;
    case 'DOCUMENT_SHARED':     return <Share2 {...props} className="w-3.5 h-3.5 text-brand-secondary shrink-0" />;
    case 'CHECKOUT_REQUESTED':  return <Lock {...props} className="w-3.5 h-3.5 text-brand-secondary shrink-0" />;
    case 'CHECKIN_DONE':        return <Unlock {...props} className="w-3.5 h-3.5 text-emerald-700 shrink-0" />;
    case 'COMMENT_ADDED':       return <MessageSquare {...props} className="w-3.5 h-3.5 text-brand-primary shrink-0" />;
    default:                    return <Cpu {...props} className="w-3.5 h-3.5 text-brand-muted shrink-0" />;
  }
};

// ─── Formateur de date relative ───────────────────────────────────────────────
function formatRelative(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return "À l'instant";
  if (minutes < 60) return `il y a ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `il y a ${hours}h`;
  const days = Math.floor(hours / 24);
  return `il y a ${days}j`;
}

// ─── Composant principal ──────────────────────────────────────────────────────
export const NotificationPanel: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

  // Charger les notifications
  const { data: notifications = [] } = useQuery<NotificationItem[]>({
    queryKey: ['notifications'],
    queryFn: notificationApi.list,
    refetchInterval: 30_000, // Poll toutes les 30 secondes
  });

  const unreadCount = notifications.filter((n) => !n.read).length;

  // Mutation : marquer une seule notif comme lue
  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Mutation : marquer toutes comme lues
  const markAllReadMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Fermer le panel au clic en dehors
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [isOpen]);

  return (
    <div className="relative" ref={panelRef}>
      {/* ── Bouton Bell ── */}
      <button
        id="notification-bell-button"
        title={`Notifications${unreadCount > 0 ? ` (${unreadCount} non lues)` : ''}`}
        onClick={() => setIsOpen((prev) => !prev)}
        className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-brand-border rounded-sm relative transition-colors"
      >
        <Bell className="w-3.5 h-3.5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-4 h-4 bg-brand-primary rounded-none flex items-center justify-center text-[9px] text-white font-bold leading-none">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* ── Panel dropdown ── */}
      {isOpen && (
        <div
          id="notification-panel"
          className="absolute right-0 top-full mt-1 w-80 bg-brand-surface border border-brand-border shadow-popover z-50 animate-in fade-in slide-in-from-top-2 duration-150"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-3 py-2 border-b border-brand-border bg-brand-alt">
            <div className="flex items-center gap-1.5">
              <Bell className="w-3.5 h-3.5 text-brand-primary" />
              <span className="text-xs font-bold uppercase tracking-wider text-brand-text">
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="text-[10px] bg-brand-primary text-white px-1.5 font-bold leading-4">
                  {unreadCount} nouvelles
                </span>
              )}
            </div>
            <div className="flex items-center gap-1">
              {unreadCount > 0 && (
                <button
                  onClick={() => markAllReadMutation.mutate()}
                  title="Tout marquer comme lu"
                  className="p-1 text-brand-muted hover:text-brand-primary rounded-sm transition-colors"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                </button>
              )}
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 text-brand-muted hover:text-brand-text rounded-sm transition-colors"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Liste des notifications */}
          <div className="max-h-80 overflow-y-auto divide-y divide-brand-border">
            {notifications.length === 0 ? (
              <div className="py-8 text-center text-xs text-brand-muted italic">
                <Bell className="w-6 h-6 mx-auto mb-2 opacity-30" />
                Aucune notification
              </div>
            ) : (
              notifications.map((notif) => (
                <div
                  key={notif.id}
                  className={`flex items-start gap-2.5 px-3 py-2.5 text-xs transition-colors ${
                    notif.read
                      ? 'bg-brand-surface text-brand-muted'
                      : 'bg-brand-primary-light text-brand-text border-l-2 border-brand-primary'
                  }`}
                >
                  {/* Icône type */}
                  <div className="mt-0.5">
                    <NotificationIcon type={notif.type} />
                  </div>

                  {/* Corps */}
                  <div className="flex-1 min-w-0">
                    <p className={`leading-snug ${notif.read ? 'font-normal' : 'font-medium'}`}>
                      {notif.title}
                    </p>
                    {notif.body && (
                      <p className="text-[10px] text-brand-muted mt-0.5 leading-snug truncate">
                        {notif.body}
                      </p>
                    )}
                    <span className="text-[10px] text-brand-muted mt-0.5 block">
                      {formatRelative(notif.createdAt)}
                    </span>
                  </div>

                  {/* Bouton marquer comme lu */}
                  {!notif.read && (
                    <button
                      onClick={() => markReadMutation.mutate(notif.id)}
                      title="Marquer comme lu"
                      className="p-0.5 text-brand-muted hover:text-brand-primary shrink-0 mt-0.5 transition-colors"
                    >
                      <Check className="w-3 h-3" />
                    </button>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="px-3 py-2 border-t border-brand-border bg-brand-alt text-center">
              <button
                onClick={() => setIsOpen(false)}
                className="text-[11px] text-brand-muted hover:text-brand-primary transition-colors"
              >
                Fermer
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
