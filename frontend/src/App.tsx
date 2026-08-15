import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ExplorerPage } from './pages/ExplorerPage';
import { DashboardPage } from './pages/DashboardPage';
import { TrashPage } from './pages/TrashPage';
import { LoginPage } from './pages/LoginPage';
import { ProtectedRoute } from './components/auth/ProtectedRoute';

import { Toaster } from 'react-hot-toast';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: 'var(--color-bg-surface, #FFFFFF)',
            color: 'var(--color-text-main, #1A1D20)',
            border: '1px solid var(--color-border, #DEE2E6)',
            fontSize: '13px',
            fontFamily: 'Inter, system-ui, sans-serif',
            boxShadow: '0 4px 12px rgba(0, 0, 0, 0.08)',
            borderRadius: '8px',
            padding: '10px 14px',
          },
          success: {
            iconTheme: {
              primary: 'var(--color-status-published, #198754)',
              secondary: '#FFFFFF',
            },
            style: {
              borderLeft: '4px solid var(--color-status-published, #198754)',
            },
          },
          error: {
            iconTheme: {
              primary: 'var(--color-primary, #C8102E)',
              secondary: '#FFFFFF',
            },
            style: {
              borderLeft: '4px solid var(--color-primary, #C8102E)',
            },
          },
        }}
      />
      <BrowserRouter>
        <Routes>
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <ExplorerPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/trash"
            element={
              <ProtectedRoute>
                <TrashPage />
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
