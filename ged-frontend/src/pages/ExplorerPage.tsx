import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { explorerApi } from '../features/documents/explorerApi';
import { useAuth } from '../hooks/useAuth';
import { Breadcrumb, useBreadcrumbStore } from '../components/layout/Breadcrumb';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { toast } from '../components/ui/Toast';
import { mapErrorCodeToMessage } from '../api/client';
import type { ApiErrorResponse } from '../types';
import {
  Folder,
  FileText,
  MoreVertical,
  Plus,
  Upload,
  Star,
  Trash,
  Lock,
  Unlock,
  FolderOpen,
} from 'lucide-react';

export const ExplorerPage: React.FC = () => {
  const { folderId } = useParams<{ folderId?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasRole, hasAnyRole } = useAuth();

  // Role Checks
  const isViewer = hasRole('VIEWER') && !hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'MANAGER', 'USER']);
  const canModify = !isViewer;

  // Breadcrumb tracking
  const { pushFolder, clear } = useBreadcrumbStore();

  // State for modals
  const [createFolderOpen, setCreateFolderOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [uploadName, setUploadName] = useState('');
  const [uploadCategoryId, setUploadCategoryId] = useState('');

  // Active context menu item
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);

  // Queries
  const {
    data: content,
    isLoading: contentLoading,
    error: contentError,
  } = useQuery({
    queryKey: ['folderContent', folderId],
    queryFn: () => explorerApi.getFolderContent(folderId),
  });

  const { data: favorites = [] } = useQuery({
    queryKey: ['favorites'],
    queryFn: explorerApi.getFavorites,
  });

  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: () => explorerApi.getCategories(),
  });

  // Track breadcrumbs based on active folder response
  useEffect(() => {
    if (content?.currentFolder) {
      pushFolder(content.currentFolder.id, content.currentFolder.name);
    } else if (!folderId) {
      clear();
    }
  }, [content, folderId, pushFolder, clear]);

  // Mutations
  const createFolderMutation = useMutation({
    mutationFn: (name: string) => explorerApi.createFolder(name, folderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folderContent', folderId] });
      toast.success('Dossier créé avec succès.');
      setCreateFolderOpen(false);
      setNewFolderName('');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de créer le dossier.');
    },
  });

  const uploadDocumentMutation = useMutation({
    mutationFn: (variables: { file: File; name: string; categoryId?: string }) =>
      explorerApi.uploadDocument(variables.file, variables.name, folderId, variables.categoryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folderContent', folderId] });
      toast.success('Document importé avec succès.');
      setUploadOpen(false);
      setUploadFile(null);
      setUploadName('');
      setUploadCategoryId('');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : "Erreur lors de l'import du fichier.");
    },
  });

  const deleteFolderMutation = useMutation({
    mutationFn: explorerApi.deleteFolder,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folderContent', folderId] });
      toast.success('Dossier supprimé.');
      setActiveMenuId(null);
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de supprimer le dossier (vérifiez qu\'il soit vide).');
    },
  });

  const deleteDocumentMutation = useMutation({
    mutationFn: explorerApi.deleteDocument,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['folderContent', folderId] });
      toast.success('Document supprimé.');
      setActiveMenuId(null);
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de supprimer le document.');
    },
  });

  const addFavoriteMutation = useMutation({
    mutationFn: (variables: { entityType: 'DOCUMENT' | 'FOLDER'; entityId: string }) =>
      explorerApi.addFavorite(variables.entityType, variables.entityId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      toast.success('Ajouté aux favoris.');
      setActiveMenuId(null);
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Erreur favoris.');
    },
  });

  const removeFavoriteMutation = useMutation({
    mutationFn: explorerApi.removeFavorite,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      toast.success('Retiré des favoris.');
      setActiveMenuId(null);
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Erreur favoris.');
    },
  });

  // Action helpers
  const handleCreateFolder = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;
    createFolderMutation.mutate(newFolderName.trim());
  };

  const handleUploadFile = (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) return;
    uploadDocumentMutation.mutate({
      file: uploadFile,
      name: uploadName || uploadFile.name,
      categoryId: uploadCategoryId || undefined,
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploadFile(file);
      setUploadName(file.name);
    }
  };

  const toggleFavorite = (entityType: 'DOCUMENT' | 'FOLDER', entityId: string) => {
    const fav = favorites.find((f) => f.entityId === entityId);
    if (fav) {
      removeFavoriteMutation.mutate(fav.id);
    } else {
      addFavoriteMutation.mutate({ entityType, entityId });
    }
  };

  const isFavorite = (entityId: string) => favorites.some((f) => f.entityId === entityId);

  if (contentError) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded text-xs">
        Erreur de chargement du contenu : {contentError.message}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Banner Area */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-gray-900 tracking-tight">
            {content?.currentFolder ? content.currentFolder.name : 'Mes documents'}
          </h1>
          <Breadcrumb />
        </div>

        {/* Global Toolbar Options */}
        {canModify && (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              className="gap-2"
              onClick={() => setCreateFolderOpen(true)}
            >
              <Plus className="h-4 w-4" />
              Nouveau dossier
            </Button>
            <Button size="sm" className="gap-2" onClick={() => setUploadOpen(true)}>
              <Upload className="h-4 w-4" />
              Importer
            </Button>
          </div>
        )}
      </div>

      {/* Directory Grid View */}
      {contentLoading ? (
        <div className="py-12 flex flex-col items-center justify-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brand" />
          <span className="text-xs text-gray-400">Chargement de l'explorateur...</span>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded shadow-xs overflow-hidden">
          {/* Table view of elements */}
          <div className="w-full overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 text-left text-xs">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3.5 font-bold text-gray-500 uppercase tracking-wider">Nom</th>
                  <th className="px-6 py-3.5 font-bold text-gray-500 uppercase tracking-wider hidden sm:table-cell">Créé le</th>
                  <th className="px-6 py-3.5 font-bold text-gray-500 uppercase tracking-wider hidden md:table-cell">Statut / Version</th>
                  <th className="px-6 py-3.5 font-bold text-gray-500 uppercase tracking-wider w-20 text-center">Actions</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-150">
                {/* SUB FOLDERS LIST */}
                {content?.subFolders.length === 0 && content?.documents.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-6 py-12 text-center text-gray-400 font-medium">
                      Ce dossier est vide.
                    </td>
                  </tr>
                )}

                {content?.subFolders.map((sub) => {
                  const fav = isFavorite(sub.id);
                  return (
                    <tr key={sub.id} className="hover:bg-gray-50/70 transition-colors group">
                      <td className="px-6 py-3.5 whitespace-nowrap">
                        <div className="flex items-center gap-3">
                          <button
                            onClick={() => toggleFavorite('FOLDER', sub.id)}
                            className={`cursor-pointer transition-colors ${
                              fav ? 'text-yellow-400' : 'text-gray-300 hover:text-yellow-400'
                            }`}
                          >
                            <Star className="h-4.5 w-4.5 fill-current" />
                          </button>
                          <div
                            onClick={() => navigate(`/folders/${sub.id}`)}
                            className="flex items-center gap-2.5 font-semibold text-gray-900 cursor-pointer hover:text-brand"
                          >
                            <Folder className="h-5 w-5 text-yellow-500 fill-yellow-500/10 flex-shrink-0" />
                            <span className="truncate max-w-xs md:max-w-md">{sub.name}</span>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-gray-500 hidden sm:table-cell">
                        {new Date(sub.createdAt).toLocaleDateString('fr-FR')}
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-gray-400 hidden md:table-cell">
                        —
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-center relative">
                        <button
                          onClick={() => setActiveMenuId(activeMenuId === sub.id ? null : sub.id)}
                          className="text-gray-400 hover:text-gray-600 p-1 rounded cursor-pointer"
                        >
                          <MoreVertical className="h-4.5 w-4.5" />
                        </button>
                        {activeMenuId === sub.id && (
                          <>
                            <div className="fixed inset-0 z-10" onClick={() => setActiveMenuId(null)} />
                            <div className="absolute right-6 mt-1 w-44 bg-white border border-gray-200 rounded shadow-md py-1 z-20 text-left">
                              <button
                                onClick={() => navigate(`/folders/${sub.id}`)}
                                className="w-full px-4 py-2 hover:bg-gray-50 text-xs text-gray-700 flex items-center gap-2 cursor-pointer"
                              >
                                <FolderOpen className="h-4 w-4 text-gray-400" />
                                Ouvrir
                              </button>
                              {canModify && (
                                <button
                                  onClick={() => deleteFolderMutation.mutate(sub.id)}
                                  className="w-full px-4 py-2 hover:bg-red-50 text-xs text-red-600 flex items-center gap-2 cursor-pointer"
                                >
                                  <Trash className="h-4 w-4" />
                                  Supprimer
                                </button>
                              )}
                            </div>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })}

                {/* DOCUMENTS LIST */}
                {content?.documents.map((doc) => {
                  const fav = isFavorite(doc.id);
                  return (
                    <tr key={doc.id} className="hover:bg-gray-50/70 transition-colors group">
                      <td className="px-6 py-3.5 whitespace-nowrap">
                        <div className="flex items-center gap-3">
                          <button
                            onClick={() => toggleFavorite('DOCUMENT', doc.id)}
                            className={`cursor-pointer transition-colors ${
                              fav ? 'text-yellow-400' : 'text-gray-300 hover:text-yellow-400'
                            }`}
                          >
                            <Star className="h-4.5 w-4.5 fill-current" />
                          </button>
                          <div
                            onClick={() => navigate(`/documents/${doc.id}`)}
                            className="flex items-center gap-2.5 font-medium text-gray-700 cursor-pointer hover:text-brand"
                          >
                            <FileText className="h-5 w-5 text-gray-400 flex-shrink-0" />
                            <span className="truncate max-w-xs md:max-w-md">{doc.name}</span>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-gray-500 hidden sm:table-cell">
                        {new Date(doc.createdAt).toLocaleDateString('fr-FR')}
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-gray-500 hidden md:table-cell">
                        <div className="flex items-center gap-2">
                          {doc.isLocked ? (
                            <span className="flex items-center gap-1 text-red-600 font-semibold">
                              <Lock className="h-3 w-3" /> Verrouillé
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 text-green-600 font-medium">
                              <Unlock className="h-3 w-3" /> Actif
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-3.5 whitespace-nowrap text-center relative">
                        <button
                          onClick={() => setActiveMenuId(activeMenuId === doc.id ? null : doc.id)}
                          className="text-gray-400 hover:text-gray-600 p-1 rounded cursor-pointer"
                        >
                          <MoreVertical className="h-4.5 w-4.5" />
                        </button>
                        {activeMenuId === doc.id && (
                          <>
                            <div className="fixed inset-0 z-10" onClick={() => setActiveMenuId(null)} />
                            <div className="absolute right-6 mt-1 w-44 bg-white border border-gray-200 rounded shadow-md py-1 z-20 text-left">
                              <button
                                onClick={() => navigate(`/documents/${doc.id}`)}
                                className="w-full px-4 py-2 hover:bg-gray-50 text-xs text-gray-700 flex items-center gap-2 cursor-pointer"
                              >
                                <FileText className="h-4 w-4 text-gray-400" />
                                Détails
                              </button>
                              {canModify && (
                                <button
                                  onClick={() => deleteDocumentMutation.mutate(doc.id)}
                                  className="w-full px-4 py-2 hover:bg-red-50 text-xs text-red-600 flex items-center gap-2 cursor-pointer"
                                >
                                  <Trash className="h-4 w-4" />
                                  Supprimer
                                </button>
                              )}
                            </div>
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* CREATE FOLDER MODAL */}
      <Modal
        isOpen={createFolderOpen}
        onClose={() => setCreateFolderOpen(false)}
        title="Créer un nouveau dossier"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setCreateFolderOpen(false)}>
              Annuler
            </Button>
            <Button
              size="sm"
              onClick={handleCreateFolder}
              isLoading={createFolderMutation.isPending}
            >
              Créer le dossier
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateFolder} className="space-y-4">
          <Input
            label="Nom du dossier"
            placeholder="Ex: Factures AWB, Rapports 2026..."
            value={newFolderName}
            onChange={(e) => setNewFolderName(e.target.value)}
            autoFocus
            required
          />
        </form>
      </Modal>

      {/* UPLOAD DOCUMENT MODAL */}
      <Modal
        isOpen={uploadOpen}
        onClose={() => setUploadOpen(false)}
        title="Importer un document"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setUploadOpen(false)}>
              Annuler
            </Button>
            <Button
              size="sm"
              onClick={handleUploadFile}
              disabled={!uploadFile}
              isLoading={uploadDocumentMutation.isPending}
            >
              Uploader le document
            </Button>
          </>
        }
      >
        <form onSubmit={handleUploadFile} className="space-y-4">
          {/* File Picker drag & drop styling */}
          <div className="flex flex-col items-center justify-center border-2 border-dashed border-gray-300 rounded-lg p-6 bg-gray-50 hover:bg-gray-100/50 transition-colors relative cursor-pointer">
            <input
              type="file"
              onChange={handleFileChange}
              className="absolute inset-0 opacity-0 cursor-pointer"
              required
            />
            <Upload className="h-8 w-8 text-gray-400 mb-2" />
            <span className="text-xs font-semibold text-gray-700">
              {uploadFile ? uploadFile.name : 'Sélectionnez un fichier ou glissez-déposez le'}
            </span>
            <span className="text-[10px] text-gray-400 mt-1">
              PDF, DOCX, XLSX, PNG, JPG, TIFF (Max: 50 Mo)
            </span>
          </div>

          {uploadFile && (
            <>
              <Input
                label="Nom d'affichage du document"
                value={uploadName}
                onChange={(e) => setUploadName(e.target.value)}
                required
              />

              <div className="flex flex-col gap-1 w-full text-left">
                <label className="text-xs font-semibold text-gray-700">
                  Catégorie du document (Optionnelle)
                </label>
                <select
                  value={uploadCategoryId}
                  onChange={(e) => setUploadCategoryId(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded text-sm text-gray-800 bg-white focus:outline-none focus:ring-1 focus:ring-brand focus:border-brand"
                >
                  <option value="">Aucune catégorie</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}
        </form>
      </Modal>
    </div>
  );
};
