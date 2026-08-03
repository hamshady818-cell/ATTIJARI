import React from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, Home } from 'lucide-react';
import { create } from 'zustand';

interface BreadcrumbItem {
  id: string;
  name: string;
}

interface BreadcrumbStore {
  trail: BreadcrumbItem[];
  setTrail: (trail: BreadcrumbItem[]) => void;
  pushFolder: (id: string, name: string) => void;
  clear: () => void;
}

export const useBreadcrumbStore = create<BreadcrumbStore>((set) => ({
  trail: [],
  setTrail: (trail) => set({ trail }),
  pushFolder: (id, name) =>
    set((state) => {
      const idx = state.trail.findIndex((t) => t.id === id);
      if (idx !== -1) {
        return { trail: state.trail.slice(0, idx + 1) };
      }
      return { trail: [...state.trail, { id, name }] };
    }),
  clear: () => set({ trail: [] }),
}));

export const Breadcrumb: React.FC = () => {
  const trail = useBreadcrumbStore((state) => state.trail);

  return (
    <nav className="flex items-center gap-1.5 text-xs text-gray-500 py-2 select-none" aria-label="Breadcrumb">
      {/* Root/Home Link */}
      <Link
        to="/"
        className="flex items-center gap-1 text-gray-600 hover:text-brand font-medium transition-colors"
      >
        <Home className="h-4 w-4" />
        <span>Racine</span>
      </Link>

      {trail.map((item, index) => {
        const isLast = index === trail.length - 1;
        return (
          <React.Fragment key={item.id}>
            <ChevronRight className="h-3.5 w-3.5 text-gray-400 flex-shrink-0" />
            {isLast ? (
              <span className="font-bold text-gray-800 truncate max-w-[150px]">
                {item.name}
              </span>
            ) : (
              <Link
                to={`/folders/${item.id}`}
                className="hover:text-brand font-medium transition-colors truncate max-w-[150px]"
              >
                {item.name}
              </Link>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};
