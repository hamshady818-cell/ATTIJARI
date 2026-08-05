import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { refApi } from '../api/refApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Files, Folder, HardDrive, FileClock, Activity } from 'lucide-react';
import { documentApi } from '../api/documentApi';

export const DashboardPage: React.FC = () => {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: refApi.getDashboardStats,
  });

  const formatSize = (bytes?: number) => {
    if (!bytes) return '0 Mo';
    const mb = bytes / (1024 * 1024);
    if (mb < 1024) return `${mb.toFixed(1)} Mo`;
    return `${(mb / 1024).toFixed(2)} Go`;
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('fr-FR');
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Title */}
        <div className="flex items-center justify-between border-b border-brand-border pb-3">
          <div>
            <h1 className="text-base font-bold uppercase tracking-wider text-brand-text">
              Tableau de Bord — Métriques de la GED
            </h1>
            <p className="text-xs text-brand-muted">
              Statistiques globales issues de la base de données PostgreSQL / MinIO en temps réel
            </p>
          </div>
          <div className="flex items-center gap-2 font-mono text-xs bg-brand-alt border border-brand-border px-3 py-1 text-brand-text">
            <Activity className="w-3.5 h-3.5 text-emerald-600" />
            <span>Système opérationnel</span>
          </div>
        </div>

        {/* Global Key Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-brand-surface border border-brand-border p-4 flex items-center gap-4">
            <div className="p-3 bg-brand-alt border border-brand-border text-brand-primary">
              <Files className="w-6 h-6" />
            </div>
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-brand-muted block">
                Total Documents
              </span>
              <span className="text-2xl font-bold font-mono text-brand-text">
                {isLoading ? '...' : stats?.totalDocuments || 0}
              </span>
            </div>
          </div>

          <div className="bg-brand-surface border border-brand-border p-4 flex items-center gap-4">
            <div className="p-3 bg-brand-alt border border-brand-border text-brand-text">
              <Folder className="w-6 h-6" />
            </div>
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-brand-muted block">
                Total Répertoires
              </span>
              <span className="text-2xl font-bold font-mono text-brand-text">
                {isLoading ? '...' : stats?.totalFolders || 0}
              </span>
            </div>
          </div>

          <div className="bg-brand-surface border border-brand-border p-4 flex items-center gap-4">
            <div className="p-3 bg-brand-alt border border-brand-border text-amber-700">
              <HardDrive className="w-6 h-6" />
            </div>
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-brand-muted block">
                Espace Stockage Utilisé
              </span>
              <span className="text-2xl font-bold font-mono text-brand-text">
                {isLoading ? '...' : formatSize(stats?.storageUsedBytes)}
              </span>
            </div>
          </div>
        </div>

        {/* Tables Grid: Recent Uploads & Recently Modified */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Uploads */}
          <div className="bg-brand-surface border border-brand-border p-4 space-y-3">
            <div className="flex items-center gap-2 border-b border-brand-border pb-2">
              <FileClock className="w-4 h-4 text-brand-primary" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                Derniers versements
              </h2>
            </div>

            <table className="w-full text-left table-dense">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Statut</th>
                  <th>Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border">
                {stats?.recentUploads?.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="text-center py-4 text-brand-muted italic">
                      Aucun versement récent
                    </td>
                  </tr>
                ) : (
                  stats?.recentUploads?.map((doc) => (
                    <tr key={doc.id}>
                      <td className="font-medium text-xs truncate max-w-[150px]">{doc.name}</td>
                      <td>
                        <Badge status={doc.status} />
                      </td>
                      <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                        {formatDate(doc.createdAt)}
                      </td>
                      <td>
                        <a
                          href={documentApi.previewUrl(doc.id)}
                          target="_blank"
                          rel="noreferrer"
                          className="text-brand-primary font-semibold hover:underline text-[11px]"
                        >
                          Voir
                        </a>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Recently Modified */}
          <div className="bg-brand-surface border border-brand-border p-4 space-y-3">
            <div className="flex items-center gap-2 border-b border-brand-border pb-2">
              <Activity className="w-4 h-4 text-brand-text" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                Dernières modifications
              </h2>
            </div>

            <table className="w-full text-left table-dense">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Statut</th>
                  <th>Modifié le</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border">
                {stats?.recentlyModified?.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="text-center py-4 text-brand-muted italic">
                      Aucune modification récente
                    </td>
                  </tr>
                ) : (
                  stats?.recentlyModified?.map((doc) => (
                    <tr key={doc.id}>
                      <td className="font-medium text-xs truncate max-w-[150px]">{doc.name}</td>
                      <td>
                        <Badge status={doc.status} />
                      </td>
                      <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                        {formatDate(doc.updatedAt)}
                      </td>
                      <td>
                        <a
                          href={documentApi.previewUrl(doc.id)}
                          target="_blank"
                          rel="noreferrer"
                          className="text-brand-primary font-semibold hover:underline text-[11px]"
                        >
                          Voir
                        </a>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
};
