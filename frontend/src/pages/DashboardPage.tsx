import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { refApi } from '../api/refApi';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';
import {
  Files,
  Folder,
  HardDrive,
  FileClock,
  Activity,
  PieChart,
  FileText,
  FileSpreadsheet,
  FileImage,
  FileCode,
  Layers,
  ArrowUpRight,
  ShieldCheck,
  TrendingUp,
} from 'lucide-react';
import { DocumentPreviewModal } from '../components/explorer/DocumentPreviewModal';
import { DocumentItem } from '../types';

export const DashboardPage: React.FC = () => {
  const [previewDocument, setPreviewDocument] = React.useState<DocumentItem | null>(null);

  // Independent pagination state for each table (client-side)
  const [uploadsPage, setUploadsPage]         = React.useState(0);
  const [uploadsPageSize, setUploadsPageSize] = React.useState(8);
  const [modifiedPage, setModifiedPage]           = React.useState(0);
  const [modifiedPageSize, setModifiedPageSize]   = React.useState(8);

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
    return new Date(dateStr).toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getFileIcon = (mimeType?: string, name?: string) => {
    const mime = mimeType?.toLowerCase() || '';
    const n = name?.toLowerCase() || '';

    if (mime.includes('pdf') || n.endsWith('.pdf')) {
      return <FileText className="w-4 h-4 text-red-500 shrink-0" />;
    }
    if (mime.includes('spreadsheet') || mime.includes('excel') || n.endsWith('.xlsx') || n.endsWith('.xls')) {
      return <FileSpreadsheet className="w-4 h-4 text-emerald-600 shrink-0" />;
    }
    if (mime.includes('word') || n.endsWith('.docx') || n.endsWith('.doc')) {
      return <FileCode className="w-4 h-4 text-blue-600 shrink-0" />;
    }
    if (mime.startsWith('image/')) {
      return <FileImage className="w-4 h-4 text-purple-500 shrink-0" />;
    }
    return <Files className="w-4 h-4 text-brand-muted shrink-0" />;
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

  // Storage calculation (Assuming 50 GB default quota display)
  const quotaBytes = 50 * 1024 * 1024 * 1024;
  const usedBytes = stats?.storageUsedBytes || 0;
  const storagePercentage = Math.min(100, Math.round((usedBytes / quotaBytes) * 100));

  // Category distribution
  const topCategories = stats?.topCategories || [];
  const totalCatDocs = topCategories.reduce((acc, cat) => acc + (cat.documentCount || 0), 0) || 1;

  const categoryColors = [
    'bg-brand-primary text-white',
    'bg-amber-500 text-white',
    'bg-blue-600 text-white',
    'bg-emerald-600 text-white',
    'bg-purple-600 text-white',
  ];

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg text-brand-text">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Banner Hero */}
        <div className="relative overflow-hidden bg-gradient-to-r from-neutral-900 via-neutral-800 to-red-950 border border-neutral-700/60 rounded-xl p-6 shadow-xl text-white">
          <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-brand-primary/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/10 text-xs font-semibold text-amber-300">
                <ShieldCheck className="w-3.5 h-3.5 text-amber-400" />
                <span>Plateforme GED Institutionnelle — Attijariwafa bank</span>
              </div>
              <h1 className="text-2xl font-extrabold tracking-tight font-display text-white">
                Tableau de Bord & Vue d'Ensemble
              </h1>
              <p className="text-xs text-neutral-300 max-w-2xl leading-relaxed">
                Supervision en temps réel des documents, répertoires et espaces de stockage sécurisés MinIO / PostgreSQL.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2.5 bg-neutral-900/80 border border-emerald-500/30 px-3.5 py-2 rounded-lg backdrop-blur-md shadow-inner">
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
                </span>
                <span className="text-xs font-semibold text-emerald-400 tracking-wide">
                  Service GED Actif
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Global Key Metrics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Documents */}
          <div className="group bg-brand-surface border border-brand-border rounded-xl p-5 shadow-card hover:shadow-popover hover:border-brand-primary/40 transition-all duration-200">
            <div className="flex items-center justify-between mb-3">
              <div className="p-3 bg-red-500/10 border border-red-500/20 text-brand-primary rounded-xl group-hover:scale-110 transition-transform">
                <Files className="w-5 h-5" />
              </div>
              <span className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                <TrendingUp className="w-3 h-3" /> Active
              </span>
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-brand-muted block">
              Total Documents
            </span>
            <span className="text-3xl font-extrabold font-mono text-brand-text mt-1 block">
              {isLoading ? '...' : (stats?.totalDocuments || 0).toLocaleString()}
            </span>
          </div>

          {/* Total Folders */}
          <div className="group bg-brand-surface border border-brand-border rounded-xl p-5 shadow-card hover:shadow-popover hover:border-amber-500/40 transition-all duration-200">
            <div className="flex items-center justify-between mb-3">
              <div className="p-3 bg-amber-500/10 border border-amber-500/20 text-amber-600 rounded-xl group-hover:scale-110 transition-transform">
                <Folder className="w-5 h-5" />
              </div>
              <span className="text-[11px] font-medium text-brand-muted">
                Arborescence
              </span>
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-brand-muted block">
              Total Répertoires
            </span>
            <span className="text-3xl font-extrabold font-mono text-brand-text mt-1 block">
              {isLoading ? '...' : (stats?.totalFolders || 0).toLocaleString()}
            </span>
          </div>

          {/* Storage Used */}
          <div className="group bg-brand-surface border border-brand-border rounded-xl p-5 shadow-card hover:shadow-popover hover:border-blue-500/40 transition-all duration-200">
            <div className="flex items-center justify-between mb-3">
              <div className="p-3 bg-blue-500/10 border border-blue-500/20 text-blue-600 rounded-xl group-hover:scale-110 transition-transform">
                <HardDrive className="w-5 h-5" />
              </div>
              <span className="text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full border border-blue-200">
                MinIO S3
              </span>
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-brand-muted block">
              Stockage Utilisé
            </span>
            <span className="text-3xl font-extrabold font-mono text-brand-text mt-1 block">
              {isLoading ? '...' : formatSize(stats?.storageUsedBytes)}
            </span>
            {/* Storage Progress Bar */}
            <div className="mt-3">
              <div className="h-1.5 w-full bg-brand-alt rounded-full overflow-hidden">
                <div
                  className="h-full bg-blue-600 rounded-full transition-all duration-500"
                  style={{ width: `${Math.max(4, storagePercentage)}%` }}
                />
              </div>
            </div>
          </div>

          {/* Categories */}
          <div className="group bg-brand-surface border border-brand-border rounded-xl p-5 shadow-card hover:shadow-popover hover:border-purple-500/40 transition-all duration-200">
            <div className="flex items-center justify-between mb-3">
              <div className="p-3 bg-purple-500/10 border border-purple-500/20 text-purple-600 rounded-xl group-hover:scale-110 transition-transform">
                <Layers className="w-5 h-5" />
              </div>
              <span className="text-[11px] font-medium text-brand-muted">
                Classification
              </span>
            </div>
            <span className="text-xs font-bold uppercase tracking-wider text-brand-muted block">
              Catégories Utilisées
            </span>
            <span className="text-3xl font-extrabold font-mono text-brand-text mt-1 block">
              {isLoading ? '...' : topCategories.length}
            </span>
          </div>
        </div>

        {/* Categories Distribution Breakdown */}
        {topCategories.length > 0 && (
          <div className="bg-brand-surface border border-brand-border rounded-xl p-5 shadow-card space-y-4">
            <div className="flex items-center justify-between border-b border-brand-border pb-3">
              <div className="flex items-center gap-2">
                <PieChart className="w-4 h-4 text-brand-primary" />
                <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                  Répartition des documents par catégorie
                </h2>
              </div>
              <span className="text-xs text-brand-muted font-medium">
                {topCategories.length} catégorie(s) enregistrée(s)
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {topCategories.map((cat, idx) => {
                const count = cat.documentCount || 0;
                const percent = Math.round((count / totalCatDocs) * 100);
                const badgeColor = categoryColors[idx % categoryColors.length];

                return (
                  <div
                    key={cat.categoryId || idx}
                    className="p-3.5 bg-brand-alt/50 border border-brand-border rounded-lg space-y-2 hover:bg-brand-alt transition-colors"
                  >
                    <div className="flex items-center justify-between text-xs">
                      <span className="font-bold text-brand-text truncate max-w-[180px]">
                        {cat.categoryName || 'Non classé'}
                      </span>
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${badgeColor}`}>
                        {count} doc{count > 1 ? 's' : ''} ({percent}%)
                      </span>
                    </div>
                    <div className="h-2 w-full bg-brand-border/60 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-brand-primary rounded-full transition-all duration-300"
                        style={{ width: `${Math.max(5, percent)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Tables Grid: Recent Uploads & Recently Modified */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Uploads */}
          <div className="bg-brand-surface border border-brand-border rounded-xl shadow-card p-5 space-y-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between border-b border-brand-border pb-3 mb-3">
                <div className="flex items-center gap-2">
                  <FileClock className="w-4 h-4 text-brand-primary" />
                  <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                    Derniers versements
                  </h2>
                </div>
                <span className="text-[11px] font-medium text-brand-muted">
                  {allUploads.length} total
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left table-dense">
                  <thead>
                    <tr>
                      <th>Document</th>
                      <th>Statut</th>
                      <th>Versé le</th>
                      <th className="text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-border font-sans">
                    {pagedUploads.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="text-center py-6 text-brand-muted italic">
                          Aucun versement récent
                        </td>
                      </tr>
                    ) : (
                      pagedUploads.map((doc) => (
                        <tr key={doc.id} className="group hover:bg-brand-primary-light/40 transition-colors">
                          <td>
                            <div className="flex items-center gap-2.5 max-w-[200px]">
                              {getFileIcon(doc.mimeType, doc.name)}
                              <span className="font-medium text-xs truncate text-brand-text group-hover:text-brand-primary transition-colors">
                                {doc.name}
                              </span>
                            </div>
                          </td>
                          <td>
                            <Badge status={doc.status} />
                          </td>
                          <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                            {formatDate(doc.createdAt)}
                          </td>
                          <td className="text-right">
                            <button
                              onClick={() => setPreviewDocument(doc as any)}
                              className="inline-flex items-center gap-1 px-2 py-1 bg-brand-alt hover:bg-brand-primary hover:text-white rounded-md text-brand-text font-semibold text-[11px] transition-all"
                            >
                              <span>Aperçu</span>
                              <ArrowUpRight className="w-3 h-3" />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="pt-3 border-t border-brand-border">
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
          </div>

          {/* Recently Modified */}
          <div className="bg-brand-surface border border-brand-border rounded-xl shadow-card p-5 space-y-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between border-b border-brand-border pb-3 mb-3">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-amber-500" />
                  <h2 className="text-xs font-bold uppercase tracking-wider text-brand-text">
                    Dernières modifications
                  </h2>
                </div>
                <span className="text-[11px] font-medium text-brand-muted">
                  {allModified.length} total
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left table-dense">
                  <thead>
                    <tr>
                      <th>Document</th>
                      <th>Statut</th>
                      <th>Modifié le</th>
                      <th className="text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-border font-sans">
                    {pagedModified.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="text-center py-6 text-brand-muted italic">
                          Aucune modification récente
                        </td>
                      </tr>
                    ) : (
                      pagedModified.map((doc) => (
                        <tr key={doc.id} className="group hover:bg-amber-500/10 transition-colors">
                          <td>
                            <div className="flex items-center gap-2.5 max-w-[200px]">
                              {getFileIcon(doc.mimeType, doc.name)}
                              <span className="font-medium text-xs truncate text-brand-text group-hover:text-amber-700 transition-colors">
                                {doc.name}
                              </span>
                            </div>
                          </td>
                          <td>
                            <Badge status={doc.status} />
                          </td>
                          <td className="font-mono text-[11px] text-brand-muted whitespace-nowrap">
                            {formatDate(doc.updatedAt)}
                          </td>
                          <td className="text-right">
                            <button
                              onClick={() => setPreviewDocument(doc as any)}
                              className="inline-flex items-center gap-1 px-2 py-1 bg-brand-alt hover:bg-amber-500 hover:text-white rounded-md text-brand-text font-semibold text-[11px] transition-all"
                            >
                              <span>Aperçu</span>
                              <ArrowUpRight className="w-3 h-3" />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="pt-3 border-t border-brand-border">
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

