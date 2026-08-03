import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, type AuthProviderProps } from 'react-oidc-context';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AppLayout } from './components/layout/AppLayout';
import { ExplorerPage } from './pages/ExplorerPage';
import { DocumentDetailPage } from './pages/DocumentDetailPage';
import { FavoritesPage } from './pages/FavoritesPage';
import { TrashPage } from './pages/TrashPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { AdminPage } from './pages/AdminPage';
import { ToastContainer } from './components/ui/Toast';

// Keycloak OAuth2 / OIDC Client Setup
const oidcConfig: AuthProviderProps = {
  authority: 'http://localhost:8081/realms/ged-awb',
  client_id: 'ged-frontend',
  redirect_uri: window.location.origin,
  onSigninCallback: () => {
    // Clear code and state from URL after callback redirect
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};

// Create a client for React Query caching
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false, // Avoid excessive refetches on browser tab switch
      retry: 1, // Only retry failed requests once
    },
  },
});

const App: React.FC = () => {
  return (
    <AuthProvider {...oidcConfig}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            {/* Main Application Layout (requires authentication) */}
            <Route path="/" element={<AppLayout />}>
              <Route index element={<ExplorerPage />} />
              <Route path="folders/:folderId" element={<ExplorerPage />} />
              <Route path="documents/:documentId" element={<DocumentDetailPage />} />
              <Route path="favorites" element={<FavoritesPage />} />
              <Route path="trash" element={<TrashPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="admin/:tab" element={<AdminPage />} />
              <Route path="admin" element={<Navigate to="/admin/categories" replace />} />
              {/* Fallback route */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
        {/* Toast Alert overlay */}
        <ToastContainer />
      </QueryClientProvider>
    </AuthProvider>
  );
};

export default App;
