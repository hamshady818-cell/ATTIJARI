import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { refApi } from '../api/refApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';
import { Files, Folder, HardDrive, FileClock, Activity } from 'lucide-react';
import { documentApi } from '../api/documentApi';
import { DocumentPreviewModal } from '../components/explorer/DocumentPreviewModal';
import { DocumentItem } from '../types';

export const DashboardPage: React.FC = () => {
  const [previewDocument, setPreviewDocument] = React.useState<DocumentItem | null>(null);

  // Independent pagination state for each table (client-side)
  const [uploadsPage, setUploadsPage]         = React.useState(0);
  const [uploadsPageSize, setUploadsPageSize] = React.useState(10);
  const [modifiedPage, setModifiedPage]           = React.useState(0);
  const [modifiedPageSize, setModifiedPageSize]   = React.useState(10);

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

  // Client-side slicing for Recent Uploads
  const allUploads   = stats?.recentUploads   || [];
  const pagedUploads = allUploads.slice(uploadsPage * uploadsPageSize, (uploadsPage + 1) * uploadsPageSize);
  const uploadsTotalPages = Math.ceil(allUploads.length / uploadsPageSize) || 1;
  const uploadsIsFirst = uploadsPage === 0;
  const uploadsIsLast  = uploadsPage >= uploadsTotalPages - 1;

  // Client-side slicing for Recently Modified
  const allModified   = stats?.recentlyModified   || [];
  const pagedModified = allModified.slice(modifiedPage * modifiedPageSize, (modifiedPage + 1) * modifiedPageSize);
  const modifiedTotalPages = Math.ceil(allModified.length / modifiedPageSize) || 1;
  const modifiedIsFirst = modifiedPage === 0;
  const modifiedIsLast  = modifiedPage >= modifiedTotalPages - 1;

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Title */}
        <div className="flex items-center justify-between bg-brand-surface p-4 border border-brand-border rounded-lg shadow-card">
          <div>
            <h1 className="text-base font-bold uppercase tracking-wider text-brand-text">
              Tableau de Bord — Métriques de la GED
            </h1>
            <p className="text-xs text-brand-muted mt-0.5">
              Statistiques globales issues de la base de données PostgreSQL / MinIO en temps réel
            </p>
          </div>
          <div className="flex items-center gap-2 font-mono text-xs bg-emerald-50 border border-emerald-200 px-3 py-1.5 rounded-md text-emerald-800 font-medium">
            <Activity className="w-4 h-4 text-emerald-600" />
            <span>Système opérationnel</span>
          </div>
        </div>

        {/* Global Key Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-5 flex items-center gap-4 hover:shadow-popover transition-shadow">
            <div className="p-3 bg-brand-primary-light border border-brand-primary/20 text-brand-primary rounded-full">
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

          <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-5 flex items-center gap-4 hover:shadow-popover transition-shadow">
            <div className="p-3 bg-brand-alt border border-brand-border text-brand-text rounded-full">
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

          <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-5 flex items-center gap-4 hover:shadow-popover transition-shadow">
            <div className="p-3 bg-amber-50 border border-amber-200 text-amber-700 rounded-full">
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
          <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-4 space-y-3">
            <div className="flex items-center gap-2 border-b border-brand-border pb-2.5">
              <FileClock className="w-4 h-4 text-brand-primary" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                Derniers versements
              </h2>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left table-dense">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Statut</th>
                    <th>Date</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-border font-sans">
                  {pagedUploads.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="text-center py-4 text-brand-muted italic">
                        Aucun versement récent
                      </td>
                    </tr>
                  ) : (
                    pagedUploads.map((doc) => (
                      <tr key={doc.id}>
                        <td className="font-medium text-xs truncate max-w-[150px]">{doc.name}</td>
                        <td>
                          <Badge status={doc.status} />
                        </td>
                        <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                          {formatDate(doc.createdAt)}
                        </td>
                        <td>
                          <button
                            onClick={() => setPreviewDocument(doc as any)}
                            className="text-brand-primary font-semibold hover:underline text-[11px]"
                          >
                            Voir
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination
              page={uploadsPage}
              pageSize={uploadsPageSize}
              totalElements={allUploads.length}
              totalPages={uploadsTotalPages}
              isFirst={uploadsIsFirst}
              isLast={uploadsIsLast}
              isLoading={isLoading}
              onPageChange={setUploadsPage}
              onPageSizeChange={(size) => { setUploadsPageSize(size); setUploadsPage(0); }}
              label="documents"
            />
          </div>

          {/* Recently Modified */}
          <div className="bg-brand-surface border border-brand-border rounded-lg shadow-card p-4 space-y-3">
            <div className="flex items-center gap-2 border-b border-brand-border pb-2.5">
              <Activity className="w-4 h-4 text-brand-text" />
              <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                Dernières modifications
              </h2>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left table-dense">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Statut</th>
                    <th>Modifié le</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-border font-sans">
                  {pagedModified.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="text-center py-4 text-brand-muted italic">
                        Aucune modification récente
                      </td>
                    </tr>
                  ) : (
                    pagedModified.map((doc) => (
                      <tr key={doc.id}>
                        <td className="font-medium text-xs truncate max-w-[150px]">{doc.name}</td>
                        <td>
                          <Badge status={doc.status} />
                        </td>
                        <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                          {formatDate(doc.updatedAt)}
                        </td>
                        <td>
                          <button
                            onClick={() => setPreviewDocument(doc as any)}
                            className="text-brand-primary font-semibold hover:underline text-[11px]"
                          >
                            Voir
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <Pagination
              page={modifiedPage}
              pageSize={modifiedPageSize}
              totalElements={allModified.length}
              totalPages={modifiedTotalPages}
              isFirst={modifiedIsFirst}
              isLast={modifiedIsLast}
              isLoading={isLoading}
              onPageChange={setModifiedPage}
              onPageSizeChange={(size) => { setModifiedPageSize(size); setModifiedPage(0); }}
              label="documents"
            />
          </div>
        </div>
      </main>

      {/* Interactive Document Preview Modal */}
      {previewDocument && (
        <DocumentPreviewModal
          document={previewDocument}
          onClose={() => setPreviewDocument(null)}
        />
      )}
    </div>
  );
};
