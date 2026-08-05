import React from 'react';
import { NavLink } from 'react-router-dom';
import { FolderTree, LayoutDashboard, Trash2, Bell, Search, ShieldCheck } from 'lucide-react';

interface HeaderProps {
  onSearchChange?: (val: string) => void;
  searchValue?: string;
}

export const Header: React.FC<HeaderProps> = ({ onSearchChange, searchValue = '' }) => {
  return (
    <header className="sticky top-0 z-40 bg-brand-surface border-b border-brand-border">
      {/* Top Red-Orange Attijari Accent Bar */}
      <div className="h-1 bg-brand-primary w-full" />

      <div className="flex items-center justify-between px-4 h-12">
        {/* Left: Branding & App Title */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 bg-brand-primary flex items-center justify-center text-white font-bold text-xs rounded-none">
              AW
            </div>
            <div>
              <span className="font-bold text-xs tracking-wider uppercase text-brand-text">GED-AWB</span>
              <span className="ml-2 text-[10px] font-mono text-brand-muted uppercase bg-brand-alt px-1.5 py-0.5 border border-brand-border">
                Attijariwafa Bank
              </span>
            </div>
          </div>

          <div className="h-4 w-px bg-brand-border" />

          {/* Navigation Links */}
          <nav className="flex items-center gap-1">
            <NavLink
              to="/"
              className={({ isActive }) =>
                `flex items-center gap-1.5 px-3 py-1 text-xs font-medium border transition-colors ${
                  isActive
                    ? 'bg-brand-primary text-white border-brand-primary'
                    : 'text-brand-text border-transparent hover:bg-brand-alt'
                }`
              }
            >
              <FolderTree className="w-3.5 h-3.5" />
              <span>Explorateur</span>
            </NavLink>

            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                `flex items-center gap-1.5 px-3 py-1 text-xs font-medium border transition-colors ${
                  isActive
                    ? 'bg-brand-primary text-white border-brand-primary'
                    : 'text-brand-text border-transparent hover:bg-brand-alt'
                }`
              }
            >
              <LayoutDashboard className="w-3.5 h-3.5" />
              <span>Tableau de bord</span>
            </NavLink>

            <NavLink
              to="/trash"
              className={({ isActive }) =>
                `flex items-center gap-1.5 px-3 py-1 text-xs font-medium border transition-colors ${
                  isActive
                    ? 'bg-brand-primary text-white border-brand-primary'
                    : 'text-brand-text border-transparent hover:bg-brand-alt'
                }`
              }
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Corbeille</span>
            </NavLink>
          </nav>
        </div>

        {/* Center: Global Search Input */}
        {onSearchChange && (
          <div className="flex-1 max-w-md mx-6">
            <div className="relative flex items-center">
              <Search className="w-3.5 h-3.5 absolute left-2.5 text-brand-muted pointer-events-none" />
              <input
                type="text"
                placeholder="Rechercher par nom, mot-clé, catégorie..."
                value={searchValue}
                onChange={(e) => onSearchChange(e.target.value)}
                className="w-full bg-brand-bg border border-brand-border text-xs text-brand-text pl-8 pr-3 py-1 rounded-sm focus:outline-none focus:bg-white focus:border-brand-primary transition-colors"
              />
            </div>
          </div>
        )}

        {/* Right: Quick User & Notifications Widget */}
        <div className="flex items-center gap-3 text-xs">
          <button
            title="Notifications"
            className="p-1.5 text-brand-muted hover:text-brand-text hover:bg-brand-alt border border-brand-border rounded-sm relative"
          >
            <Bell className="w-3.5 h-3.5" />
            <span className="absolute -top-1 -right-1 w-2 h-2 bg-brand-primary rounded-none" />
          </button>

          <div className="h-4 w-px bg-brand-border" />

          {/* User profile */}
          <div className="flex items-center gap-2 px-2 py-1 bg-brand-alt border border-brand-border">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-700" />
            <div className="flex flex-col text-left">
              <span className="font-semibold text-[11px] leading-none text-brand-text">Agent GED</span>
              <span className="text-[9px] text-brand-muted leading-tight">Keycloak SSO</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
