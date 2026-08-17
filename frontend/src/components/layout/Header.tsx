import React, { useState, useRef, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { FolderTree, LayoutDashboard, Trash2, Sliders, Layers, Search, ShieldCheck, ChevronDown, LogOut, User } from 'lucide-react';
import { NotificationPanel } from './NotificationPanel';
import { AttijariLogo } from '../ui/AttijariLogo';
import { useAuth } from '../../context/AuthContext';

interface HeaderProps {
  onSearchChange?: (val: string) => void;
  searchValue?: string;
}

export const Header: React.FC<HeaderProps> = ({ onSearchChange, searchValue = '' }) => {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const { user, logout } = useAuth();

  // Nom réel de l'utilisateur extrait du token Keycloak via AuthContext
  const username = user?.name || user?.username || 'Agent GED';
  const email = user?.email || '';

  const handleLogout = () => {
    logout();
  };

  // Fermeture du menu déroulant si clic à l'extérieur
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };
    if (isUserMenuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isUserMenuOpen]);

  return (
    <header className="sticky top-0 z-40 bg-brand-surface border-b border-brand-border shadow-flat">
      {/* Top Red-Orange Attijari Accent Bar */}
      <div className="h-1 bg-brand-primary w-full" />

      <div className="flex items-center justify-between px-5 h-13">
        {/* Left: Branding & App Title */}
        <div className="flex items-center gap-5">
          <div className="flex items-center gap-3">
            <AttijariLogo className="w-8 h-8" />
            <div>
              <span className="font-bold text-xs tracking-wider uppercase text-brand-text block">GED-AWB</span>
              <span className="text-[10px] font-mono text-brand-muted uppercase bg-brand-alt px-1.5 py-0.5 rounded-sm border border-brand-border inline-block">
                Attijariwafa Bank
              </span>
            </div>
          </div>

          <div className="h-5 w-px bg-brand-border" />

          {/* Navigation Links */}
          <nav className="flex items-center gap-1.5">
            <NavLink
              to="/"
              className={({ isActive }) =>
                `flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all duration-150 ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text hover:bg-brand-alt'
                }`
              }
            >
              <FolderTree className="w-4 h-4" />
              <span>Explorateur</span>
            </NavLink>

            <NavLink
              to="/dashboard"
              className={({ isActive }) =>
                `flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all duration-150 ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text hover:bg-brand-alt'
                }`
              }
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Tableau de bord</span>
            </NavLink>

            <NavLink
              to="/trash"
              className={({ isActive }) =>
                `flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all duration-150 ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text hover:bg-brand-alt'
                }`
              }
            >
              <Trash2 className="w-4 h-4" />
              <span>Corbeille</span>
            </NavLink>

            <NavLink
              to="/metadata-definitions"
              className={({ isActive }) =>
                `flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all duration-150 ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text hover:bg-brand-alt'
                }`
              }
            >
              <Sliders className="w-4 h-4" />
              <span>Métadonnées</span>
            </NavLink>

            <NavLink
              to="/categories"
              className={({ isActive }) =>
                `flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md transition-all duration-150 ${
                  isActive
                    ? 'bg-brand-primary text-white shadow-xs'
                    : 'text-brand-text hover:bg-brand-alt'
                }`
              }
            >
              <Layers className="w-4 h-4" />
              <span>Catégories</span>
            </NavLink>
          </nav>
        </div>

        {/* Center: Global Search Input */}
        {onSearchChange && (
          <div className="flex-1 max-w-md mx-6">
            <div className="relative flex items-center">
              <Search className="w-4 h-4 absolute left-3 text-brand-muted pointer-events-none" />
              <input
                type="text"
                placeholder="Rechercher par nom, mot-clé, catégorie..."
                value={searchValue}
                onChange={(e) => onSearchChange(e.target.value)}
                className="w-full bg-brand-bg border border-brand-border text-xs text-brand-text pl-9 pr-3 py-1.5 rounded-md focus:outline-none focus:bg-white focus:border-brand-primary focus:ring-2 focus:ring-brand-primary/20 transition-all duration-150"
              />
            </div>
          </div>
        )}

        {/* Right: Quick User & Notifications Widget */}
        <div className="flex items-center gap-3 text-xs">
          {/* Notifications — dropdown interactif */}
          <NotificationPanel />

          <div className="h-5 w-px bg-brand-border" />

          {/* User Profile Dropdown */}
          <div className="relative" ref={userMenuRef}>
            <button
              onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
              className="flex items-center gap-2.5 px-3 py-1.5 bg-brand-alt hover:bg-brand-border/60 border border-brand-border rounded-md transition-colors text-left focus:outline-none"
              title="Profil & Déconnexion"
            >
              <ShieldCheck className="w-4 h-4 text-emerald-700 shrink-0" />
              <div className="flex flex-col text-left">
                <span className="font-semibold text-xs leading-none text-brand-text max-w-[120px] truncate">
                  {username}
                </span>
                <span className="text-[9px] text-brand-muted leading-tight mt-0.5">Keycloak SSO</span>
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-brand-muted transition-transform duration-200 ${isUserMenuOpen ? 'rotate-180' : ''}`} />
            </button>

            {/* Menu Déroulant Profil */}
            {isUserMenuOpen && (
              <div className="absolute right-0 top-full mt-2 w-60 bg-brand-surface border border-brand-border shadow-popover z-50 animate-in fade-in slide-in-from-top-2 duration-150 rounded-lg overflow-hidden">
                {/* En-tête du menu */}
                <div className="p-3.5 bg-brand-alt border-b border-brand-border space-y-1">
                  <div className="flex items-center gap-2">
                    <User className="w-4 h-4 text-brand-primary" />
                    <span className="font-bold text-xs text-brand-text truncate">{username}</span>
                  </div>
                  {email && (
                    <p className="text-[10px] text-brand-muted font-mono truncate">{email}</p>
                  )}
                  <span className="inline-block text-[9px] bg-emerald-50 text-emerald-800 border border-emerald-200 font-bold px-2 py-0.5 rounded-md uppercase mt-1">
                    Session Keycloak SSO Active
                  </span>
                </div>

                {/* Contenu du menu */}
                <div className="p-1.5">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-3 py-2 text-xs text-red-600 hover:bg-red-50 font-medium transition-colors rounded-md text-left"
                  >
                    <LogOut className="w-4 h-4 shrink-0 text-red-600" />
                    <span>Déconnexion</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
