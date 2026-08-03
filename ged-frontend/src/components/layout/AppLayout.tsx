import React from 'react';
import { Outlet } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import { Logo } from './Logo';
import { Button } from '../ui/Button';
import { ShieldAlert } from 'lucide-react';

export const AppLayout: React.FC = () => {
  const { isAuthenticated, isLoading, login } = useAuth();

  // If Keycloak session is loading, show a neat bank styled spinner
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-brand" />
          <p className="text-sm font-semibold text-gray-500 tracking-wider">
            Connexion sécurisée en cours...
          </p>
        </div>
      </div>
    );
  }

  // If unauthenticated, display an clean login landing page and a sign-in redirect button
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white border border-gray-200 p-8 rounded shadow-2xl max-w-md w-full text-center flex flex-col items-center gap-6">
          <div className="h-14 w-auto flex items-center justify-center">
            <Logo className="h-12" />
          </div>
          
          <div className="space-y-2">
            <h1 className="text-xl font-bold text-gray-900 leading-tight">Portail de Gestion Documentaire</h1>
            <p className="text-xs text-gray-500 max-w-xs mx-auto leading-relaxed">
              Pour accéder aux documents et dossiers de la banque, vous devez vous authentifier via Keycloak.
            </p>
          </div>

          <Button onClick={login} className="w-full justify-center">
            S'authentifier avec Keycloak
          </Button>

          <div className="flex items-center gap-1.5 text-[10px] text-gray-400 justify-center">
            <ShieldAlert className="h-3.5 w-3.5 text-gray-400" />
            <span>Serveur de sécurité : localhost:8081</span>
          </div>
        </div>
      </div>
    );
  }

  // Render the secure app workspace
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-6 md:p-8">
          <div className="max-w-7xl mx-auto space-y-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
