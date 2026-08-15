import React, { useState, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import { documentApi } from '../api/documentApi';
import { refApi } from '../api/refApi';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../utils/errorMessages';
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
import { BulkTagModal } from '../components/explorer/BulkTagModal';
import { ConfirmModal } from '../components/ui/ConfirmModal';
import { Button } from '../components/ui/Button';
import { Pagination } from '../components/ui/Pagination';
import {
  BulkActionResult,
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

  // Pagination state for the folder-documents query
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);

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

  // Confirm Delete Modals state
  const [deleteSingleDoc, setDeleteSingleDoc] = useState<{ id: string; name: string } | null>(null);
  const [isDeletingSingle, setIsDeletingSingle] = useState(false);
  const [isBulkDeleteModalOpen, setIsBulkDeleteModalOpen] = useState(false);
  const [isDeletingBulk, setIsDeletingBulk] = useState(false);
  // Tag Modal state
  const [isBulkTagOpen, setIsBulkTagOpen] = useState(false);

  // TanStack Query for Categories & Ref data
  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: refApi.getCategories,
  });

  // Query Folder Content / Root Content — used ONLY for subFolders and currentFolder metadata
  const {
    data: folderContent,
    refetch: refetchFolderContent,
    isLoading: isLoadingFolderContent,
  } = useQuery({
    queryKey: ['folder-content', selectedFolderId, activeFilterType],
    queryFn: async () => {
      if (activeFilterType === 'drafts' || activeFilterType === 'all') {
        return { currentFolder: undefined, subFolders: [], documents: [] };
      }
      return selectedFolderId ? folderApi.getFolderContent(selectedFolderId) : folderApi.getRootContent();
    },
  });

  // Paginated documents query (replaces the non-paginated folderContent.documents)
  const isSearchActive =
    !!filters.keyword || !!filters.status || !!filters.categoryId || !!filters.tagName;

  const {
    data: folderDocsPage,
    isLoading: isLoadingFolderDocs,
  } = useQuery({
    queryKey: ['folder-documents', selectedFolderId, activeFilterType, page, pageSize],
    queryFn: () => {
      if (activeFilterType === 'drafts')
        return documentApi.search({ status: 'DRAFT', page, size: pageSize });
      if (activeFilterType === 'all')
        return documentApi.search({ page, size: pageSize });
      return documentApi.search({ folderId: selectedFolderId, page, size: pageSize });
    },
    enabled: !isSearchActive,
  });

  // Pagination metadata derived from the paginated query
  const totalElements = folderDocsPage?.totalElements ?? 0;
  const totalPages   = folderDocsPage?.totalPages   ?? 0;
  const isFirst      = folderDocsPage?.first  ?? page === 0;
  const isLast       = folderDocsPage?.last   ?? (totalPages === 0 || page >= totalPages - 1);
  const isLoadingDocs = isLoadingFolderContent || isLoadingFolderDocs;

  // Query All Folders flat list for tree sidebar navigation and modals
  const { data: allFoldersData = [], refetch: refetchAllFolders } = useQuery<FolderItem[]>({
    queryKey: ['all-folders-tree'],
    queryFn: folderApi.getAllFolders,
  });

  const { data: searchResults, refetch: refetchSearch } = useQuery({
    queryKey: ['search-documents', filters],
    queryFn: () => documentApi.search(filters),
    enabled: isSearchActive,
  });

  const displayedDocuments = isSearchActive
    ? searchResults?.content || []
    : folderDocsPage?.content || [];

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ['all-folders-tree'] });
    queryClient.invalidateQueries({ queryKey: ['folder-content'] });
    queryClient.invalidateQueries({ queryKey: ['folder-documents'] });
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
  const handleBulkDelete = () => {
    setIsBulkDeleteModalOpen(true);
  };

  const confirmBulkDelete = async () => {
    setIsDeletingBulk(true);
    try {
      const result = await documentApi.bulkDelete(selectedDocIds);
      setSelectedDocIds([]);
      if (activeDocument && selectedDocIds.includes(activeDocument.id)) {
        setActiveDocument(null);
      }
      // Edge case: if deleting the last item on a non-first page, go back one page
      if (displayedDocuments.length <= selectedDocIds.length && page > 0) {
        setPage((p) => p - 1);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      if (result.processedCount > 0) {
        toast.success(
          `${result.processedCount} document(s) supprimé(s) avec succès`
        );
      }
      if (result.skippedNames.length > 0) {
        toast.error(
          `${result.skippedNames.length} document(s) verrouillé(s) ignoré(s) : ${result.skippedNames.join(', ')}`
        );
      }
      handleRefresh();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la suppression en masse.'));
    } finally {
      setIsDeletingBulk(false);
      setIsBulkDeleteModalOpen(false);
    }
  };

  const handleMoveDocument = async (documentIds: string[], targetFolderId?: string, moveToRoot = false) => {
    try {
      const result = await documentApi.bulkMove(documentIds, targetFolderId, moveToRoot);
      setSelectedDocIds([]);
      if (activeDocument && documentIds.includes(activeDocument.id)) {
        setActiveDocument(null);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      if (result.processedCount > 0) {
        toast.success(
          `${result.processedCount} document(s) déplacé(s) avec succès`
        );
      }
      if (result.skippedNames.length > 0) {
        toast.error(
          `${result.skippedNames.length} document(s) verrouillé(s) non déplacé(s) : ${result.skippedNames.join(', ')}`
        );
      }
      handleRefresh();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec du déplacement des documents.'));
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

  const handleBulkTag = () => {
    setIsBulkTagOpen(true);
  };

  const handleBulkTagSuccess = (result: BulkActionResult) => {
    setSelectedDocIds([]);
    if (result.processedCount > 0) {
      toast.success(`${result.processedCount} document(s) étiqueté(s) avec succès`);
    }
    if (result.skippedNames.length > 0) {
      toast.error(`${result.skippedNames.length} document(s) verrouillé(s) non étiqueté(s) : ${result.skippedNames.join(', ')}`);
    }
    handleRefresh();
  };

  const handleDeleteSingle = (id: string) => {
    const doc = displayedDocuments.find((d) => d.id === id);
    setDeleteSingleDoc({ id, name: doc?.name || 'ce document' });
  };

  const confirmDeleteSingle = async () => {
    if (!deleteSingleDoc) return;
    setIsDeletingSingle(true);
    try {
      await documentApi.delete(deleteSingleDoc.id);
      if (activeDocument?.id === deleteSingleDoc.id) {
        setActiveDocument(null);
      }
      // Edge case: if this was the last document on a non-first page, go back one page
      if (displayedDocuments.length === 1 && page > 0) {
        setPage((p) => p - 1);
      }
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['folder-documents'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      toast.success('Document supprimé avec succès');
      handleRefresh();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la suppression du document.'));
    } finally {
      setIsDeletingSingle(false);
      setDeleteSingleDoc(null);
    }
  };

  const handleCheckoutSingle = async (id: string) => {
    const doc =
      folderContent?.documents?.find((d) => d.id === id) ||
      searchResults?.content?.find((d) => d.id === id);

    const isLocked = Boolean(doc?.isLocked ?? (doc as any)?.locked);

    if (isLocked) {
      toast.error('Ce document est déjà verrouillé.');
      return;
    }

    try {
      await documentApi.checkout(id);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      toast.success('Document verrouillé pour édition');
      handleRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || '';
      if (msg.includes('already checked out') || msg.includes('verrouillé')) {
        toast.error('Ce document est déjà verrouillé par un autre utilisateur.');
      } else {
        toast.error(msg || 'Erreur de verrouillage.');
      }
    }
  };

  const handleCheckinSingle = async (id: string) => {
    try {
      await documentApi.checkin(id);
      await queryClient.invalidateQueries({ queryKey: ['folder-content'] });
      await queryClient.invalidateQueries({ queryKey: ['search-documents'] });
      toast.success('Document déverrouillé avec succès');
      handleRefresh();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec du déverrouillage du document.'));
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
      toast.success('Dossier supprimé avec succès');
      handleRefresh();
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Erreur inconnue';
      toast.error(msg || 'Échec de la suppression du dossier.');
    } finally {
      setIsDeletingFolder(false);
    }
  };

  const handleFolderSelect = (folderId?: string) => {
    setSelectedFolderId(folderId);
    setActiveFilterType('folder');
    setPage(0);
    setFilters((prev) => ({ ...prev, keyword: '', page: 0 }));
  };

  const folderPath = useMemo(() => {
    if (!folderContent?.currentFolder) return [];
    const path: FolderItem[] = [];
    let current: FolderItem | undefined = folderContent.currentFolder;
    const foldersById = new Map(allFoldersData.map((f) => [f.id, f]));
    while (current) {
      path.unshift(current);
      current = current.parentId ? foldersById.get(current.parentId) : undefined;
    }
    return path;
  }, [folderContent?.currentFolder, allFoldersData]);

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
          onSelectFolder={handleFolderSelect}
          onCreateFolderClick={() => setIsCreateFolderOpen(true)}
          activeFilterType={activeFilterType}
          onSelectFilterType={(ft) => {
            setActiveFilterType(ft);
            setPage(0); // Reset page on filter type change
          }}
          onMoveDocument={handleMoveDocument}
          onDeleteFolder={handleDeleteFolder}
        />

        {/* Center Content Workspace */}
        <main className="flex-1 flex flex-col overflow-y-auto p-5">
          {/* Breadcrumb Path & Action Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4 bg-brand-surface p-3.5 border border-brand-border rounded-lg shadow-card">
            {/* Breadcrumb */}
            <div className="flex items-center gap-2 text-xs text-brand-muted flex-wrap">
              <button
                onClick={() => handleFolderSelect(undefined)}
                className="flex items-center gap-1.5 font-semibold text-brand-text hover:text-brand-primary transition-colors cursor-pointer"
              >
                <Home className="w-4 h-4 text-brand-muted shrink-0" />
                <span>Racine GED</span>
              </button>

              {folderPath.map((folder, index) => (
                <React.Fragment key={folder.id}>
                  <ChevronRight className="w-3.5 h-3.5 text-brand-border shrink-0" />
                  {index === folderPath.length - 1 ? (
                    <span className="font-bold text-brand-primary flex items-center gap-1.5">
                      <FolderOpen className="w-4 h-4" />
                      {folder.name}
                    </span>
                  ) : (
                    <button
                      onClick={() => handleFolderSelect(folder.id)}
                      className="font-semibold text-brand-muted hover:text-brand-primary transition-colors cursor-pointer"
                    >
                      {folder.name}
                    </button>
                  )}
                </React.Fragment>
              ))}
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
            {/* Pagination — only shown in folder/all/drafts mode, not during search */}
            {!isSearchActive && (
              <div className="bg-brand-surface border border-brand-border rounded-b-lg border-t-0">
                <Pagination
                  page={page}
                  pageSize={pageSize}
                  totalElements={totalElements}
                  totalPages={totalPages}
                  isFirst={isFirst}
                  isLast={isLast}
                  isLoading={isLoadingDocs}
                  onPageChange={setPage}
                  onPageSizeChange={(size) => { setPageSize(size); setPage(0); }}
                  label="documents"
                />
              </div>
            )}
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

      {/* Single Document Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!deleteSingleDoc}
        onClose={() => setDeleteSingleDoc(null)}
        onConfirm={confirmDeleteSingle}
        title="Mettre à la corbeille"
        message={`Voulez-vous vraiment mettre le document "${deleteSingleDoc?.name}" à la corbeille ?`}
        confirmText="Mettre à la corbeille"
        cancelText="Annuler"
        variant="danger"
        isLoading={isDeletingSingle}
      />

      {/* Bulk Documents Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={isBulkDeleteModalOpen}
        onClose={() => setIsBulkDeleteModalOpen(false)}
        onConfirm={confirmBulkDelete}
        title="Suppression en masse"
        message={`Voulez-vous vraiment mettre ${selectedDocIds.length} document(s) sélectionné(s) à la corbeille ?`}
        confirmText="Tout mettre à la corbeille"
        cancelText="Annuler"
        variant="danger"
        isLoading={isDeletingBulk}
      />
      {/* Bulk Tag Input Modal */}
      <BulkTagModal
        isOpen={isBulkTagOpen}
        onClose={() => setIsBulkTagOpen(false)}
        documentIds={selectedDocIds}
        documentCount={selectedDocIds.length}
        onSuccess={handleBulkTagSuccess}
      />
    </div>
  );
};
