import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import { documentApi } from '../api/documentApi';
import { refApi } from '../api/refApi';
import { Header } from '../components/layout/Header';
import { FolderTreeSidebar } from '../components/layout/FolderTreeSidebar';
import { DocumentTable } from '../components/explorer/DocumentTable';
import { DocumentFilterDrawer } from '../components/explorer/DocumentFilterDrawer';
import { BulkActionToolbar } from '../components/explorer/BulkActionToolbar';
import { DocumentDetailPanel } from '../components/explorer/DocumentDetailPanel';
import { UploadModal } from '../components/explorer/UploadModal';
import { CreateFolderModal } from '../components/explorer/CreateFolderModal';
import { Button } from '../components/ui/Button';
import {
  DocumentItem,
  DocumentSearchResult,
  FolderItem,
  SearchFilterParams,
} from '../types';
import {
  Upload,
  FolderPlus,
  Filter,
  RefreshCw,
  FolderOpen,
  ChevronRight,
  Home,
} from 'lucide-react';

export const ExplorerPage: React.FC = () => {
  const [selectedFolderId, setSelectedFolderId] = useState<string | undefined>();
  const [activeFilterType, setActiveFilterType] = useState<'all' | 'folder' | 'drafts'>('folder');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);

  // Search & Filter State
  const [filters, setFilters] = useState<SearchFilterParams>({
    keyword: '',
    status: '',
    categoryId: '',
    tagName: '',
    page: 0,
    size: 25,
    sortBy: 'createdAt',
    sortDirection: 'DESC',
  });

  // Table selections & Slide-over modal states
  const [selectedDocIds, setSelectedDocIds] = useState<string[]>([]);
  const [activeDocument, setActiveDocument] = useState<DocumentItem | DocumentSearchResult | null>(null);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);

  // TanStack Query for Categories & Ref data
  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: refApi.getCategories,
  });

  // Query Folder Content / Root Content
  const {
    data: folderContent,
    refetch: refetchFolderContent,
    isLoading: isLoadingFolderContent,
  } = useQuery({
    queryKey: ['folder-content', selectedFolderId, activeFilterType],
    queryFn: async () => {
      if (activeFilterType === 'drafts') {
        const p = await documentApi.search({ status: 'DRAFT', size: 50 });
        return { currentFolder: undefined, subFolders: [], documents: p.content as any };
      }
      if (activeFilterType === 'all') {
        const p = await documentApi.search({ size: 50 });
        return { currentFolder: undefined, subFolders: [], documents: p.content as any };
      }
      return selectedFolderId ? folderApi.getFolderContent(selectedFolderId) : folderApi.getRootContent();
    },
  });

  // Query All Folders for tree sidebar navigation
  const { data: allFoldersData = [], refetch: refetchAllFolders } = useQuery<FolderItem[]>({
    queryKey: ['all-folders-tree'],
    queryFn: async () => {
      const root = await folderApi.getRootContent();
      return root.subFolders || [];
    },
  });

  // Dynamic search query when filters are applied
  const isSearchActive =
    !!filters.keyword || !!filters.status || !!filters.categoryId || !!filters.tagName;

  const { data: searchResults, refetch: refetchSearch } = useQuery({
    queryKey: ['search-documents', filters],
    queryFn: () => documentApi.search(filters),
    enabled: isSearchActive,
  });

  const displayedDocuments = isSearchActive
    ? searchResults?.content || []
    : folderContent?.documents || [];

  const handleRefresh = () => {
    refetchFolderContent();
    refetchAllFolders();
    if (isSearchActive) refetchSearch();
  };

  // Checkbox Selection handlers
  const handleToggleSelect = (id: string) => {
    setSelectedDocIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const handleToggleSelectAll = () => {
    if (selectedDocIds.length === displayedDocuments.length) {
      setSelectedDocIds([]);
    } else {
      setSelectedDocIds(displayedDocuments.map((d: any) => d.id));
    }
  };

  // Bulk Actions
  const handleBulkDelete = async () => {
    if (!confirm(`Supprimer ${selectedDocIds.length} document(s) ?`)) return;
    try {
      await documentApi.bulkDelete(selectedDocIds);
      setSelectedDocIds([]);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur lors de la suppression en masse: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleBulkMove = async () => {
    const target = prompt('Entrez l\'ID du dossier de destination (ou laissez vide pour la racine) :');
    if (target === null) return;
    try {
      await documentApi.bulkMove(selectedDocIds, target || undefined, !target);
      setSelectedDocIds([]);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur lors du déplacement en masse: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleBulkTag = async () => {
    const tagsInput = prompt('Entrez les étiquettes séparées par des virgules (ex: urgent, finance) :');
    if (!tagsInput) return;
    const tagList = tagsInput.split(',').map((t) => t.trim()).filter(Boolean);
    try {
      await documentApi.bulkTag(selectedDocIds, tagList);
      setSelectedDocIds([]);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur d\'étiquetage en masse: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteSingle = async (id: string) => {
    if (!confirm('Voulez-vous mettre ce document à la corbeille ?')) return;
    try {
      await documentApi.delete(id);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur de suppression: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCheckoutSingle = async (id: string) => {
    try {
      await documentApi.checkout(id);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur de verrouillage: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCheckinSingle = async (id: string) => {
    try {
      await documentApi.checkin(id);
      handleRefresh();
    } catch (err: any) {
      alert('Erreur de déverrouillage: ' + (err.response?.data?.message || err.message));
    }
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg">
      {/* Header Bar */}
      <Header
        searchValue={filters.keyword}
        onSearchChange={(val) => setFilters((prev) => ({ ...prev, keyword: val, page: 0 }))}
      />

      {/* Main Workstation View */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Folder Tree Sidebar */}
        <FolderTreeSidebar
          folders={allFoldersData}
          selectedFolderId={selectedFolderId}
          onSelectFolder={(id) => {
            setSelectedFolderId(id);
            setFilters((prev) => ({ ...prev, keyword: '', page: 0 }));
          }}
          onCreateFolderClick={() => setIsCreateFolderOpen(true)}
          activeFilterType={activeFilterType}
          onSelectFilterType={setActiveFilterType}
        />

        {/* Center Content Workspace */}
        <main className="flex-1 flex flex-col overflow-y-auto p-4">
          {/* Breadcrumb Path & Action Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 bg-brand-surface p-3 border border-brand-border">
            {/* Breadcrumb */}
            <div className="flex items-center gap-1.5 text-xs text-brand-muted">
              <Home className="w-3.5 h-3.5 text-brand-muted shrink-0" />
              <ChevronRight className="w-3 h-3 text-brand-border shrink-0" />
              <span className="font-semibold text-brand-text">Racine GED</span>
              {folderContent?.currentFolder && (
                <>
                  <ChevronRight className="w-3 h-3 text-brand-border shrink-0" />
                  <FolderOpen className="w-3.5 h-3.5 text-brand-primary shrink-0" />
                  <span className="font-bold text-brand-primary">
                    {folderContent.currentFolder.name}
                  </span>
                </>
              )}
            </div>

            {/* Quick Actions Header Toolbar */}
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                icon={<Filter className="w-3.5 h-3.5" />}
                onClick={() => setShowFilterDrawer(!showFilterDrawer)}
              >
                Filtres {isSearchActive && '*(actifs)'}
              </Button>

              <Button
                variant="secondary"
                size="sm"
                icon={<FolderPlus className="w-3.5 h-3.5" />}
                onClick={() => setIsCreateFolderOpen(true)}
              >
                Nouveau dossier
              </Button>

              <Button
                variant="primary"
                size="sm"
                icon={<Upload className="w-3.5 h-3.5" />}
                onClick={() => setIsUploadOpen(true)}
              >
                Verser document(s)
              </Button>

              <Button
                variant="ghost"
                size="sm"
                icon={<RefreshCw className="w-3.5 h-3.5" />}
                onClick={handleRefresh}
                title="Actualiser"
              />
            </div>
          </div>

          {/* Filter Drawer Panel */}
          {showFilterDrawer && (
            <DocumentFilterDrawer
              filters={filters}
              categories={categories}
              onChange={setFilters}
              onReset={() =>
                setFilters({
                  keyword: '',
                  status: '',
                  categoryId: '',
                  tagName: '',
                  page: 0,
                  size: 25,
                  sortBy: 'createdAt',
                  sortDirection: 'DESC',
                })
              }
            />
          )}

          {/* Bulk Action Toolbar */}
          <BulkActionToolbar
            selectedCount={selectedDocIds.length}
            onClearSelection={() => setSelectedDocIds([])}
            onBulkDelete={handleBulkDelete}
            onBulkMove={handleBulkMove}
            onBulkTag={handleBulkTag}
          />

          {/* Subfolders Grid if any */}
          {folderContent?.subFolders && folderContent.subFolders.length > 0 && !isSearchActive && (
            <div className="mb-4">
              <div className="text-[10px] font-bold uppercase tracking-wider text-brand-muted mb-2">
                Sous-dossiers ({folderContent.subFolders.length})
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-2">
                {folderContent.subFolders.map((sub) => (
                  <button
                    key={sub.id}
                    onClick={() => setSelectedFolderId(sub.id)}
                    className="flex items-center gap-2 p-2 bg-brand-surface border border-brand-border hover:border-brand-primary text-left text-xs font-medium truncate transition-colors"
                  >
                    <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
                    <span className="truncate">{sub.name}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Documents Table */}
          <div className="flex-1">
            <DocumentTable
              documents={displayedDocuments}
              selectedIds={selectedDocIds}
              onToggleSelect={handleToggleSelect}
              onToggleSelectAll={handleToggleSelectAll}
              onSelectDocument={(doc) => setActiveDocument(doc)}
              onDeleteDocument={handleDeleteSingle}
              onCheckoutDocument={handleCheckoutSingle}
              onCheckinDocument={handleCheckinSingle}
              sortBy={filters.sortBy}
              sortDirection={filters.sortDirection}
              onSort={(field) =>
                setFilters((prev) => ({
                  ...prev,
                  sortBy: field,
                  sortDirection: prev.sortBy === field && prev.sortDirection === 'DESC' ? 'ASC' : 'DESC',
                }))
              }
            />
          </div>
        </main>
      </div>

      {/* Upload Modal */}
      <UploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        folders={allFoldersData}
        categories={categories}
        defaultFolderId={selectedFolderId}
        onSuccess={handleRefresh}
      />

      {/* Create Folder Modal */}
      <CreateFolderModal
        isOpen={isCreateFolderOpen}
        onClose={() => setIsCreateFolderOpen(false)}
        folders={allFoldersData}
        defaultParentId={selectedFolderId}
        onSuccess={handleRefresh}
      />

      {/* Slide-over Detail Panel */}
      {activeDocument && (
        <DocumentDetailPanel
          document={activeDocument}
          onClose={() => setActiveDocument(null)}
          onRefresh={handleRefresh}
        />
      )}
    </div>
  );
};
