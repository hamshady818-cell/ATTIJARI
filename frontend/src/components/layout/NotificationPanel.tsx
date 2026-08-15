import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Bell,
  X,
  Check,
  CheckCheck,
  Upload,
  Edit,
  Trash2,
  Share2,
  Lock,
  Unlock,
  MessageSquare,
  Cpu,
  Clock,
  Inbox,
  CheckCircle2,
} from 'lucide-react';
import { notificationApi } from '../../api/notificationApi';
import { NotificationItem, NotificationType } from '../../types';

// ─── Icon & Color Badge Helper by Notification Type & Read Status ─────────────
const NotificationTypeBadge: React.FC<{ type: NotificationType; isRead: boolean }> = ({ type, isRead }) => {
  const iconProps = { className: 'w-4 h-4 shrink-0' };

  if (isRead) {
    return (
      <div className="p-2 rounded-xl bg-gray-100 border border-gray-200 text-gray-400 shrink-0">
        <Bell {...iconProps} />
      </div>
    );
  }

  switch (type) {
    case 'DOCUMENT_UPLOADED':
      return (
        <div className="p-2 rounded-xl bg-red-500/10 text-brand-primary border border-red-500/30 shrink-0 shadow-xs">
          <Upload {...iconProps} />
        </div>
      );
    case 'DOCUMENT_UPDATED':
      return (
        <div className="p-2 rounded-xl bg-amber-500/10 text-amber-600 border border-amber-500/30 shrink-0 shadow-xs">
          <Edit {...iconProps} />
        </div>
      );
    case 'DOCUMENT_DELETED':
      return (
        <div className="p-2 rounded-xl bg-red-500/10 text-red-600 border border-red-500/30 shrink-0 shadow-xs">
          <Trash2 {...iconProps} />
        </div>
      );
    case 'DOCUMENT_SHARED':
      return (
        <div className="p-2 rounded-xl bg-blue-500/10 text-blue-600 border border-blue-500/30 shrink-0 shadow-xs">
          <Share2 {...iconProps} />
        </div>
      );
    case 'DOCUMENT_EXPIRED':
      return (
        <div className="p-2 rounded-xl bg-red-500/10 text-red-700 border border-red-500/30 shrink-0 shadow-xs">
          <Clock {...iconProps} />
        </div>
      );
    case 'CHECKOUT_REQUESTED':
      return (
        <div className="p-2 rounded-xl bg-amber-500/10 text-amber-700 border border-amber-500/30 shrink-0 shadow-xs">
          <Lock {...iconProps} />
        </div>
      );
    case 'CHECKIN_DONE':
      return (
        <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-600 border border-emerald-500/30 shrink-0 shadow-xs">
          <Unlock {...iconProps} />
        </div>
      );
    case 'COMMENT_ADDED':
      return (
        <div className="p-2 rounded-xl bg-purple-500/10 text-purple-600 border border-purple-500/30 shrink-0 shadow-xs">
          <MessageSquare {...iconProps} />
        </div>
      );
    default:
      return (
        <div className="p-2 rounded-xl bg-gray-500/10 text-brand-muted border border-gray-500/30 shrink-0 shadow-xs">
          <Cpu {...iconProps} />
        </div>
      );
  }
};

// ─── Relative Time Formatter ──────────────────────────────────────────────────
function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return "À l'instant";
  if (minutes < 60) return `il y a ${minutes} min`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `il y a ${hours}h`;
  const days = Math.floor(hours / 24);
  if (days === 1) return 'Hier';
  return `il y a ${days}j`;
}

export const NotificationPanel: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'all' | 'unread'>('all');
  const panelRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

  // Fetch notifications
  const { data: notifications = [] } = useQuery<NotificationItem[]>({
    queryKey: ['notifications'],
    queryFn: notificationApi.list,
    refetchInterval: 15_000,
  });

  const unreadCount = notifications.filter((n) => !n.read).length;
  const displayedNotifications =
    activeTab === 'unread' ? notifications.filter((n) => !n.read) : notifications;

  // Single Notification Mark Read Mutation (Optimistic Update)
  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onMutate: async (id: string) => {
      await queryClient.cancelQueries({ queryKey: ['notifications'] });
      const previous = queryClient.getQueryData<NotificationItem[]>(['notifications']);
      if (previous) {
        queryClient.setQueryData<NotificationItem[]>(
          ['notifications'],
          previous.map((n) => (n.id === id ? { ...n, read: true, status: 'READ' } : n))
        );
      }
      return { previous };
    },
    onError: (_err, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['notifications'], context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Mark All Notifications Read Mutation (Optimistic Update)
  const markAllReadMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['notifications'] });
      const previous = queryClient.getQueryData<NotificationItem[]>(['notifications']);
      if (previous) {
        queryClient.setQueryData<NotificationItem[]>(
          ['notifications'],
          previous.map((n) => ({ ...n, read: true, status: 'READ' }))
        );
      }
      return { previous };
    },
    onError: (_err, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['notifications'], context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Click Outside Listener
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  return (
    <div className="relative" ref={panelRef}>
      {/* ── Bell Trigger Button ── */}
      <button
        id="notification-bell-button"
        title={`Notifications${unreadCount > 0 ? ` (${unreadCount} non lues)` : ''}`}
        onClick={() => setIsOpen((prev) => !prev)}
        className={`p-2 rounded-xl border transition-all duration-200 relative shadow-xs ${
          isOpen
            ? 'bg-brand-primary-light border-brand-primary text-brand-primary'
            : 'bg-brand-surface border-brand-border hover:border-brand-primary/40 hover:bg-brand-alt text-brand-muted hover:text-brand-text'
        }`}
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute -top-1.5 -right-1.5 min-w-5 h-5 px-1 bg-brand-primary rounded-full flex items-center justify-center text-[10px] text-white font-black leading-none shadow-md border-2 border-brand-surface animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* ── Dropdown Panel ── */}
      {isOpen && (
        <div
          id="notification-panel"
          className="absolute right-0 top-full mt-2.5 w-96 bg-brand-surface border border-brand-border rounded-xl shadow-modal z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-150"
        >
          {/* Header */}
          <div className="p-4 border-b border-brand-border bg-gradient-to-r from-neutral-900 via-neutral-800 to-red-950 text-white">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-xl bg-white/10 text-amber-400 border border-white/10">
                  <Bell className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-white">
                    Centre de Notifications
                  </h3>
                  <p className="text-[10px] text-neutral-300">GED Attijariwafa bank</p>
                </div>
              </div>

              <button
                onClick={() => setIsOpen(false)}
                className="p-1 text-neutral-400 hover:text-white rounded-md transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Filter Tabs & Quick Actions */}
            <div className="flex items-center justify-between pt-1">
              <div className="flex gap-1 p-0.5 bg-black/30 rounded-lg border border-white/10 text-xs">
                <button
                  onClick={() => setActiveTab('all')}
                  className={`px-3 py-1 rounded-md text-[11px] font-bold transition-all ${
                    activeTab === 'all'
                      ? 'bg-brand-primary text-white shadow-xs'
                      : 'text-neutral-300 hover:text-white'
                  }`}
                >
                  Toutes ({notifications.length})
                </button>
                <button
                  onClick={() => setActiveTab('unread')}
                  className={`px-3 py-1 rounded-md text-[11px] font-bold transition-all ${
                    activeTab === 'unread'
                      ? 'bg-brand-primary text-white shadow-xs'
                      : 'text-neutral-300 hover:text-white'
                  }`}
                >
                  Non lues ({unreadCount})
                </button>
              </div>

              {unreadCount > 0 && (
                <button
                  onClick={() => markAllReadMutation.mutate()}
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 text-[11px] font-semibold text-white bg-white/10 hover:bg-white/20 border border-white/10 rounded-md transition-all"
                  title="Tout marquer comme lu"
                >
                  <CheckCheck className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Tout lire</span>
                </button>
              )}
            </div>
          </div>

          {/* Notification List */}
          <div className="max-h-96 overflow-y-auto divide-y divide-brand-border">
            {displayedNotifications.length === 0 ? (
              <div className="py-12 text-center text-xs text-brand-muted">
                <div className="flex flex-col items-center justify-center gap-2">
                  <div className="p-3 bg-brand-alt border border-brand-border rounded-full text-brand-muted">
                    <Inbox className="w-6 h-6" />
                  </div>
                  <p className="font-semibold text-brand-text">
                    {activeTab === 'unread'
                      ? 'Toutes les notifications sont lues'
                      : 'Aucune notification enregistrée'}
                  </p>
                  <p className="text-[11px] text-brand-muted">
                    Vous recevrez des alertes lors d'actions sur vos documents.
                  </p>
                </div>
              </div>
            ) : (
              displayedNotifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => {
                    if (!notif.read) markReadMutation.mutate(notif.id);
                  }}
                  className={`flex items-start gap-3.5 p-3.5 text-xs cursor-pointer transition-all ${
                    notif.read
                      ? 'bg-brand-surface/70 text-brand-muted border-l-4 border-l-transparent hover:bg-brand-alt/60'
                      : 'bg-red-500/5 text-brand-text border-l-4 border-l-brand-primary shadow-2xs hover:bg-red-500/10'
                  }`}
                >
                  {/* Notification Type Icon Badge */}
                  <NotificationTypeBadge type={notif.type} isRead={notif.read} />

                  {/* Notification Details */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-1 mb-1">
                      <h4
                        className={`text-xs truncate ${
                          notif.read ? 'font-semibold text-brand-muted' : 'font-extrabold text-brand-text'
                        }`}
                      >
                        {notif.title}
                      </h4>

                      {/* Read / Unread Status Pill */}
                      {!notif.read ? (
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-black bg-brand-primary text-white shrink-0 shadow-xs">
                          <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                          NOUVEAU
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-[9px] font-semibold bg-gray-100 text-gray-500 border border-gray-200 shrink-0">
                          <CheckCircle2 className="w-2.5 h-2.5 text-emerald-600" />
                          Lu
                        </span>
                      )}
                    </div>

                    {notif.body && (
                      <p className={`text-[11px] leading-relaxed line-clamp-2 ${notif.read ? 'text-brand-muted/80 font-normal' : 'text-brand-text font-medium'}`}>
                        {notif.body}
                      </p>
                    )}

                    <div className="flex items-center justify-between mt-2 pt-1.5 border-t border-brand-border/40 text-[10px]">
                      <span className="text-brand-muted/70 font-mono">
                        {formatRelativeTime(notif.createdAt)}
                      </span>

                      {!notif.read && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            markReadMutation.mutate(notif.id);
                          }}
                          className="text-brand-primary hover:underline font-bold inline-flex items-center gap-1"
                        >
                          <Check className="w-3 h-3 text-brand-primary" />
                          <span>Marquer comme lu</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-3 border-t border-brand-border bg-brand-alt/50 flex items-center justify-between text-[11px] text-brand-muted">
            <span className="font-mono">
              {notifications.length} notification{notifications.length > 1 ? 's' : ''} ({unreadCount} non lue{unreadCount > 1 ? 's' : ''})
            </span>
            <button
              onClick={() => setIsOpen(false)}
              className="font-bold text-brand-text hover:text-brand-primary transition-colors"
            >
              Fermer
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
