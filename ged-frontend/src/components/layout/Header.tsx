import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Logo } from './Logo';
import { Bell, Search, LogOut, ChevronDown } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../api/client';
import type { NotificationResponseDto } from '../../types';
import { useNavigate, Link } from 'react-router-dom';

export const Header: React.FC = () => {
  const { user, roles, logout, isAuthenticated } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const navigate = useNavigate();

  // Fetch notifications using React Query
  const { data: notifications = [] } = useQuery<NotificationResponseDto[]>({
    queryKey: ['notifications'],
    queryFn: async () => {
      const res = await api.get('/notifications');
      return res.data;
    },
    enabled: isAuthenticated,
    refetchInterval: 10000, // Poll every 10 seconds for notifications
  });

  const unreadCount = notifications.filter((n) => n.status !== 'READ').length;

  return (
    <header className="bg-white border-b border-gray-200 h-16 px-6 flex items-center justify-between sticky top-0 z-30 select-none">
      {/* Left: Brand Logo */}
      <div className="flex items-center gap-2">
        <Link to="/">
          <Logo className="h-10" />
        </Link>
      </div>

      {/* Middle: Mock Search Bar */}
      <div className="hidden md:flex items-center max-w-md w-full relative">
        <span className="absolute left-3 text-gray-400">
          <Search className="h-4 w-4" />
        </span>
        <input
          type="text"
          placeholder="Rechercher des documents, dossiers, tags..."
          className="w-full bg-gray-50 border border-gray-200 rounded pl-9 pr-4 py-1.5 text-xs text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-brand focus:border-brand focus:bg-white transition-colors"
        />
      </div>

      {/* Right: Actions and User details */}
      <div className="flex items-center gap-5">
        {/* Notifications Trigger */}
        <button
          onClick={() => navigate('/notifications')}
          className="relative p-1.5 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
          title="Notifications"
        >
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute top-0.5 right-0.5 bg-brand text-white text-[9px] font-bold h-4 w-4 rounded-full flex items-center justify-center ring-2 ring-white">
              {unreadCount}
            </span>
          )}
        </button>

        {/* User Account Dropdown */}
        {user && (
          <div className="relative">
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="flex items-center gap-2 hover:bg-gray-50 p-1.5 rounded transition-colors cursor-pointer"
            >
              <div className="h-8 w-8 rounded-full bg-brand/10 border border-brand/20 flex items-center justify-center text-brand font-bold text-sm uppercase">
                {user.username.substring(0, 2)}
              </div>
              <div className="hidden sm:flex flex-col text-left">
                <span className="text-xs font-semibold text-gray-900 leading-none">
                  {user.firstName} {user.lastName}
                </span>
                <span className="text-[10px] text-gray-400 font-medium">
                  {roles[0] || 'VIEWER'}
                </span>
              </div>
              <ChevronDown className="h-4 w-4 text-gray-400 hidden sm:block" />
            </button>

            {dropdownOpen && (
              <>
                {/* Click overlay to close dropdown */}
                <div
                  className="fixed inset-0 z-30"
                  onClick={() => setDropdownOpen(false)}
                />
                <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded shadow-lg py-1 z-40 animate-in fade-in slide-in-from-top-1 duration-200">
                  <div className="px-4 py-2 border-b border-gray-150">
                    <p className="text-xs text-gray-400">Connecté en tant que</p>
                    <p className="text-xs font-bold text-gray-700 truncate">
                      {user.email}
                    </p>
                  </div>
                  <button
                    onClick={() => {
                      setDropdownOpen(false);
                      logout();
                    }}
                    className="w-full text-left px-4 py-2 text-xs text-red-600 hover:bg-red-50 flex items-center gap-2 transition-colors cursor-pointer"
                  >
                    <LogOut className="h-4 w-4" />
                    Déconnexion
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </header>
  );
};
