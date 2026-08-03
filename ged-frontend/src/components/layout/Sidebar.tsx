import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import {
  Folder,
  Star,
  Trash2,
  Bell,
  Settings,
  Layers,
  Building,
  Sliders,
  Tags,
  Shield,
  History,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { hasAnyRole } = useAuth();
  const isAdminOrManager = hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'MANAGER']);

  const mainNavItems = [
    { to: '/', label: 'Mes documents', icon: <Folder className="h-4.5 w-4.5" /> },
    { to: '/favorites', label: 'Favoris', icon: <Star className="h-4.5 w-4.5" /> },
    { to: '/trash', label: 'Corbeille', icon: <Trash2 className="h-4.5 w-4.5" /> },
    { to: '/notifications', label: 'Notifications', icon: <Bell className="h-4.5 w-4.5" /> },
  ];

  const adminNavItems = [
    { to: '/admin/categories', label: 'Catégories', icon: <Layers className="h-4.5 w-4.5" /> },
    { to: '/admin/departments', label: 'Départements', icon: <Building className="h-4.5 w-4.5" /> },
    { to: '/admin/metadata', label: 'Métadonnées', icon: <Sliders className="h-4.5 w-4.5" /> },
    { to: '/admin/tags', label: 'Tags', icon: <Tags className="h-4.5 w-4.5" /> },
    { to: '/admin/roles', label: 'Rôles', icon: <Shield className="h-4.5 w-4.5" /> },
    { to: '/admin/audit', label: 'Audit logs', icon: <History className="h-4.5 w-4.5" /> },
  ];

  return (
    <aside className="w-64 bg-white border-r border-gray-200 min-h-[calc(100vh-4rem)] flex flex-col flex-shrink-0 select-none">
      {/* Main Navigation */}
      <nav className="flex-1 py-6 px-4 space-y-1">
        {mainNavItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 text-xs font-semibold rounded transition-colors ${
                isActive
                  ? 'bg-brand/5 text-brand font-bold border-l-2 border-brand -ml-[2px]'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              }`
            }
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}

        {/* Administration Section */}
        {isAdminOrManager && (
          <div className="pt-6 mt-6 border-t border-gray-150">
            <span className="px-3 text-[10px] font-bold text-gray-400 uppercase tracking-wider flex items-center gap-1.5 mb-2">
              <Settings className="h-3 w-3" />
              Administration
            </span>
            <div className="space-y-1">
              {adminNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2 text-xs font-semibold rounded transition-colors ${
                      isActive
                        ? 'bg-brand/5 text-brand font-bold border-l-2 border-brand -ml-[2px]'
                        : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                    }`
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              ))}
            </div>
          </div>
        )}
      </nav>
      {/* Footer Info */}
      <div className="p-4 border-t border-gray-150 text-[10px] text-gray-400 text-center font-medium">
        GED Attijariwafa Bank v1.0.0
      </div>
    </aside>
  );
};
