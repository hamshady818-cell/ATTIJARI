import { create } from 'zustand';
import { X, CheckCircle, AlertTriangle, AlertCircle, Info } from 'lucide-react';
import React, { useEffect } from 'react';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastItem {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface ToastStore {
  toasts: ToastItem[];
  addToast: (message: string, type: ToastType, duration?: number) => void;
  removeToast: (id: string) => void;
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  addToast: (message, type, duration = 4000) => {
    const id = Math.random().toString(36).substring(2, 9);
    set((state) => ({
      toasts: [...state.toasts, { id, message, type, duration }],
    }));
  },
  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },
}));

// Quick access helpers
export const toast = {
  success: (msg: string, duration?: number) => useToastStore.getState().addToast(msg, 'success', duration),
  error: (msg: string, duration?: number) => useToastStore.getState().addToast(msg, 'error', duration),
  warning: (msg: string, duration?: number) => useToastStore.getState().addToast(msg, 'warning', duration),
  info: (msg: string, duration?: number) => useToastStore.getState().addToast(msg, 'info', duration),
};

export const ToastContainer: React.FC = () => {
  const toasts = useToastStore((state) => state.toasts);

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      {toasts.map((t) => (
        <ToastCard key={t.id} toast={t} />
      ))}
    </div>
  );
};

const ToastCard: React.FC<{ toast: ToastItem }> = ({ toast: t }) => {
  const removeToast = useToastStore((state) => state.removeToast);

  useEffect(() => {
    const timer = setTimeout(() => {
      removeToast(t.id);
    }, t.duration);
    return () => clearTimeout(timer);
  }, [t.id, t.duration, removeToast]);

  const icons = {
    success: <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0" />,
    error: <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0" />,
    warning: <AlertTriangle className="h-5 w-5 text-yellow-600 flex-shrink-0" />,
    info: <Info className="h-5 w-5 text-blue-600 flex-shrink-0" />,
  };

  const bgStyles = {
    success: 'bg-white border-l-4 border-green-500 shadow-lg text-gray-800',
    error: 'bg-white border-l-4 border-red-500 shadow-lg text-gray-800',
    warning: 'bg-white border-l-4 border-yellow-500 shadow-lg text-gray-800',
    info: 'bg-white border-l-4 border-blue-500 shadow-lg text-gray-800',
  };

  return (
    <div
      className={`flex items-start gap-3 p-4 rounded border border-gray-150 pointer-events-auto transition-all duration-300 transform translate-y-0 ${bgStyles[t.type]}`}
      role="alert"
    >
      {icons[t.type]}
      <div className="flex-1 text-sm font-medium">{t.message}</div>
      <button
        onClick={() => removeToast(t.id)}
        className="text-gray-400 hover:text-gray-600 transition-colors flex-shrink-0 cursor-pointer"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};
