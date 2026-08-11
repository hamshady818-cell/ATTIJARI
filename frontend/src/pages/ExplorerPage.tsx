import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import { documentApi } from '../api/documentApi';
import { refApi } from '../api/refApi';
import { Header } from '../components/layout/Header';
import { FolderTreeSidebar } from '../components/layout/FolderTreeSidebar';
import { DocumentTable } from '../components/explorer/DocumentTable';
import { DocumentFilterDrawer } from '../components/explorer/DocumentFilterDrawer';
import { BulkActionToolbar } from '../components/explorer/BulkActionToolbar';
import { DocumentDetailPanel } from '../components/explorer/DocumentDetailPanel';
import { DocumentPreviewModal } from '../components/explorer/DocumentPreviewModal';
import { MoveDocumentModal } from '../components/explorer/MoveDocumentModal';
import { UploadModal } from '../components/explorer/UploadModal';
import { CreateFolderModal } from '../components/explorer/CreateFolderModal';
import { DeleteFolderModal } from '../components/explorer/DeleteFolderModal';
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
  Trash2,
} from 'lucide-react';

export const ExplorerPage: React.FC = () => {
  const [selectedFolderId, setSelectedFolderId] = useState<string | undefined>();
  const [activeFilterType, setActiveFilterType] = useState<'all' | 'folder' | 'drafts'>('folder');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);
  const queryClient = useQueryClient();

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
  const [previewDocument, setPreviewDocument] = useState<DocumentItem | DocumentSearchResult | null>(null);
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [isCreateFolderOpen, setIsCreateFolderOpen] = useState(false);

  // Move Modal & Drag-and-Drop state
  const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
  const [moveModalDocIds, setMoveModalDocIds] = useState<string[]>([]);
  const [moveModalDocNames, setMoveModalDocNames] = useState<string[]>([]);
  const [dragOverSubfolderId, setDragOverSubfolderId] = useState<string | null>(null);

  // Delete Folder Modal state
  const [deleteFolderTarget, setDeleteFolderTarget] = useState<FolderItem | null>(null);
  const [isDeleteFolderModalOpen, setIsDeleteFolderModalOpen] = useState(false);
  const [isDeletingFolder, setIsDeletingFolder] = useState(false);
  const [deleteFolderDocCount, setDeleteFolderDocCount] = useState(0);

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

  // Query All Folders flat list for tree sidebar navigation and modals
  const { data: allFoldersData = [], refetch: refetchAllFolders } = useQuery<FolderItem[]>({
    queryKey: ['all-folders-tree'],
    queryFn: folderApi.getAllFolders,
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
    queryClient.invalidateQueries({ queryKey: ['all-folders-tree'] });
    queryClient.invalidateQueries({ queryKey: ['folder-content'] });
    queryClient.invalidateQueries({ queryKey: ['search-documents'] });
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
      if (activeDocument && selectedDocIds.includes(activeDocument.id)) {
        setActiveDocument(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      handleRefresh();
    } catch (err: any) {
      alert('Erreur lors de la suppression en masse: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleMoveDocument = async (documentIds: string[], targetFolderId?: string, moveToRoot = false) => {
    try {
      await documentApi.bulkMove(documentIds, targetFolderId, moveToRoot);
      setSelectedDocIds([]);
      if (activeDocument && documentIds.includes(activeDocument.id)) {
        setActiveDocument(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      handleRefresh();
    } catch (err: any) {
      alert('Erreur lors du déplacement: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleOpenMoveSingle = (doc: DocumentItem | DocumentSearchResult) => {
    setMoveModalDocIds([doc.id]);
    setMoveModalDocNames([doc.name]);
    setIsMoveModalOpen(true);
  };

  const handleBulkMove = async () => {
    const selectedDocs = displayedDocuments.filter((d) => selectedDocIds.includes(d.id));
    setMoveModalDocIds(selectedDocIds);
    setMoveModalDocNames(selectedDocs.map((d) => d.name));
    setIsMoveModalOpen(true);
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
      if (activeDocument?.id === id) {
        setActiveDocument(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      handleRefresh();
    } catch (err: any) {
      alert('Erreur de suppression: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCheckoutSingle = async (id: string) => {
    const doc =
      folderContent?.documents?.find((d) => d.id === id) ||
      searchResults?.content?.find((d) => d.id === id);

    const isLocked = Boolean(doc?.isLocked ?? (doc as any)?.locked);

    if (isLocked) {
      alert('Ce document est déjà verrouillé.');
      return;
    }

    try {
      await documentApi.checkout(id);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      handleRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || '';
      if (msg.includes('already checked out') || msg.includes('verrouillé')) {
        alert('Ce document est déjà verrouillé par un autre utilisateur.');
      } else {
        alert('Erreur de verrouillage: ' + (msg || 'Une erreur est survenue'));
      }
    }
  };

  const handleCheckinSingle = async (id: string) => {
    try {
      await documentApi.checkin(id);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      handleRefresh();
    } catch (err: any) {
      alert('Erreur de déverrouillage: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDeleteFolder = (folder: FolderItem) => {
    const docsInThisFolder =
      selectedFolderId === folder.id
        ? (folderContent?.documents?.length ?? 0)
        : (folderContent?.subFolders?.some((sf) => sf.id === folder.id)
          ? 0
          : 0);
    setDeleteFolderTarget(folder);
    setDeleteFolderDocCount(docsInThisFolder);
    setIsDeleteFolderModalOpen(true);
  };

  const executeFolderDeletion = async (force: boolean) => {
    if (!deleteFolderTarget) return;
    setIsDeletingFolder(true);
    try {
      await folderApi.deleteFolder(deleteFolderTarget.id, force);
      setIsDeleteFolderModalOpen(false);
      if (selectedFolderId === deleteFolderTarget.id) {
        setSelectedFolderId(undefined);
      }
      setDeleteFolderTarget(null);
      await queryClient.invalidateQueries({ queryKey: ['all-folders-tree'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      handleRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Erreur inconnue';
      alert('Erreur lors de la suppression du dossier\u00a0: ' + msg);
    } finally {
      setIsDeletingFolder(false);
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
          onMoveDocument={handleMoveDocument}
          onDeleteFolder={handleDeleteFolder}
        />

        {/* Center Content Workspace */}
        <main className="flex-1 flex flex-col overflow-y-auto p-5">
          {/* Breadcrumb Path & Action Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 bg-brand-surface p-3.5 border border-brand-border rounded-lg shadow-card">
            {/* Breadcrumb */}
            <div className="flex items-center gap-2 text-xs text-brand-muted">
              <Home className="w-4 h-4 text-brand-muted shrink-0" />
              <ChevronRight className="w-3.5 h-3.5 text-brand-border shrink-0" />
              <span className="font-semibold text-brand-text">Racine GED</span>
              {folderContent?.currentFolder && (
                <>
                  <ChevronRight className="w-3.5 h-3.5 text-brand-border shrink-0" />
                  <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
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
              <div className="text-[10px] font-bold uppercase tracking-wider text-brand-muted mb-2.5">
                Sous-dossiers ({folderContent.subFolders.length}) — Glissez des documents pour déplacer
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
                {folderContent.subFolders.map((sub) => {
                  const isDragOver = dragOverSubfolderId === sub.id;
                  return (
                    <div
                      key={sub.id}
                      className={`group relative flex items-center gap-2.5 p-3 text-left text-xs font-medium border rounded-lg transition-all cursor-pointer shadow-card ${
                        isDragOver
                          ? 'bg-brand-primary-light border-brand-primary text-brand-primary font-bold scale-105 shadow-popover'
                          : 'bg-brand-surface border-brand-border hover:border-brand-primary/50 hover:shadow-popover text-brand-text'
                      }`}
                      onClick={() => setSelectedFolderId(sub.id)}
                      onDragOver={(e) => {
                        e.preventDefault();
                        e.dataTransfer.dropEffect = 'move';
                        setDragOverSubfolderId(sub.id);
                      }}
                      onDragLeave={() => setDragOverSubfolderId(null)}
                      onDrop={(e) => {
                        e.preventDefault();
                        setDragOverSubfolderId(null);
                        try {
                          const dataStr = e.dataTransfer.getData('text/plain');
                          if (!dataStr) return;
                          const data = JSON.parse(dataStr);
                          if (data && Array.isArray(data.documentIds)) {
                            handleMoveDocument(data.documentIds, sub.id);
                          }
                        } catch { /* ignore */ }
                      }}
                    >
                      <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
                      <span className="truncate flex-1 font-semibold">{sub.name}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteFolder(sub);
                        }}
                        className="opacity-0 group-hover:opacity-100 p-1 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light rounded-md transition-all shrink-0"
                        title={`Supprimer le dossier "${sub.name}"`}
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  );
                })}
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
              onPreviewDocument={(doc) => setPreviewDocument(doc)}
              onMoveSingleDocument={handleOpenMoveSingle}
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
          onPreview={(doc) => setPreviewDocument(doc)}
        />
      )}

      {/* Move Document Modal */}
      {isMoveModalOpen && (
        <MoveDocumentModal
          isOpen={isMoveModalOpen}
          onClose={() => setIsMoveModalOpen(false)}
          documentIds={moveModalDocIds}
          documentNames={moveModalDocNames}
          folders={allFoldersData}
          currentFolderId={selectedFolderId}
          onSuccess={() => {
            setSelectedDocIds([]);
            handleRefresh();
          }}
        />
      )}

      {/* Interactive Document Preview Modal */}
      {previewDocument && (
        <DocumentPreviewModal
          document={previewDocument}
          onClose={() => setPreviewDocument(null)}
        />
      )}

      {/* Delete Folder Confirmation Modal */}
      <DeleteFolderModal
        isOpen={isDeleteFolderModalOpen}
        folder={deleteFolderTarget}
        documentCount={deleteFolderDocCount}
        hasSubfolders={!!deleteFolderTarget && allFoldersData.some((f) => f.parentId === deleteFolderTarget.id)}
        isDeleting={isDeletingFolder}
        onCancel={() => {
          setIsDeleteFolderModalOpen(false);
          setDeleteFolderTarget(null);
        }}
        onConfirm={executeFolderDeletion}
      />
    </div>
  );
};
