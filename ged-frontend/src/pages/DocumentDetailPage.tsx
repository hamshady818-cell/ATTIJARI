import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import { explorerApi } from '../features/documents/explorerApi';
import { useAuth } from '../hooks/useAuth';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Badge } from '../components/ui/Badge';
import { toast } from '../components/ui/Toast';
import { mapErrorCodeToMessage } from '../api/client';
import type {
  DocumentResponseDto,
  PermissionResponseDto,
  CategoryResponseDto,
  ApiErrorResponse,
} from '../types';
import {
  ArrowLeft,
  Calendar,
  User as UserIcon,
  Tag as TagIcon,
  Shield,
  Trash2,
  Plus,
  Lock,
  Unlock,
  Download,
  Info,
} from 'lucide-react';

export const DocumentDetailPage: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>();
  const queryClient = useQueryClient();
  const { hasAnyRole } = useAuth();

  // Modal State for adding permission
  const [permModalOpen, setPermModalOpen] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [targetGroupId, setTargetGroupId] = useState('');
  const [canRead, setCanRead] = useState(true);
  const [canWrite, setCanWrite] = useState(false);
  const [canDelete, setCanDelete] = useState(false);
  const [canShareOrManage, setCanShareOrManage] = useState(false);

  // Queries
  const {
    data: document,
    isLoading: docLoading,
    error: docError,
  } = useQuery<DocumentResponseDto>({
    queryKey: ['document', documentId],
    queryFn: async () => {
      const res = await api.get(`/documents/${documentId}`);
      return res.data;
    },
    enabled: !!documentId,
  });

  const { data: permissions = [], isLoading: permLoading } = useQuery<PermissionResponseDto[]>({
    queryKey: ['documentPermissions', documentId],
    queryFn: async () => {
      const res = await api.get(`/documents/${documentId}/permissions`);
      return res.data;
    },
    enabled: !!documentId,
  });

  const { data: categories = [] } = useQuery<CategoryResponseDto[]>({
    queryKey: ['categories'],
    queryFn: () => explorerApi.getCategories(),
  });

  // Check user permissions to manage permissions
  const canManagePermissions = hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'MANAGER']);

  // Mutations
  const grantPermissionMutation = useMutation({
    mutationFn: async (variables: {
      userId: string | null;
      groupId: string | null;
      canRead: boolean;
      canWrite: boolean;
      canDelete: boolean;
      canShareOrManage: boolean;
    }) => {
      const res = await api.post(`/documents/${documentId}/permissions`, variables);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documentPermissions', documentId] });
      toast.success('Permission accordée avec succès.');
      setPermModalOpen(false);
      setTargetUserId('');
      setTargetGroupId('');
      setCanRead(true);
      setCanWrite(false);
      setCanDelete(false);
      setCanShareOrManage(false);
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible d\'accorder la permission.');
    },
  });

  const revokePermissionMutation = useMutation({
    mutationFn: async (permId: string) => {
      await api.delete(`/documents/${documentId}/permissions/${permId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documentPermissions', documentId] });
      toast.success('Permission révoquée.');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de révoquer la permission.');
    },
  });

  const handleGrantPermission = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetUserId.trim() && !targetGroupId.trim()) {
      toast.warning('Veuillez renseigner un ID Utilisateur ou ID Groupe.');
      return;
    }
    grantPermissionMutation.mutate({
      userId: targetUserId.trim() || null,
      groupId: targetGroupId.trim() || null,
      canRead,
      canWrite,
      canDelete,
      canShareOrManage,
    });
  };

  const handleDownload = () => {
    toast.info('Le téléchargement du document est en cours de préparation...');
    // Fallback: simulate download by opening the api endpoint or saving a mock text/blob
    try {
      const downloadUrl = `${api.defaults.baseURL}/documents/${documentId}/download`;
      // Open link
      window.open(downloadUrl, '_blank');
    } catch (e) {
      console.error(e);
    }
  };

  if (docError) {
    return (
      <div className="space-y-4">
        <Link to="/" className="inline-flex items-center gap-2 text-xs font-bold text-gray-500 hover:text-brand transition-colors">
          <ArrowLeft className="h-4 w-4" /> Retour aux documents
        </Link>
        <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded text-xs">
          Erreur lors du chargement des détails du document : {docError.message}
        </div>
      </div>
    );
  }

  const matchedCategory = categories.find((c) => c.id === document?.categoryId);

  return (
    <div className="space-y-6">
      {/* Back button */}
      <div>
        <Link
          to={document ? `/folders/${document.folderId || ''}` : '/'}
          className="inline-flex items-center gap-2 text-xs font-bold text-gray-500 hover:text-brand transition-colors select-none"
        >
          <ArrowLeft className="h-4 w-4" />
          Retour à l'explorateur
        </Link>
      </div>

      {docLoading ? (
        <div className="py-12 flex flex-col items-center justify-center gap-2">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-brand" />
          <span className="text-xs text-gray-400">Chargement du document...</span>
        </div>
      ) : (
        document && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left: Metadata Details */}
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-white border border-gray-200 rounded shadow-xs p-6 space-y-6">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 border-b border-gray-150 pb-5">
                  <div className="space-y-1">
                    <h2 className="text-lg font-bold text-gray-900 leading-tight">
                      {document.name}
                    </h2>
                    <p className="text-xs text-gray-400">ID: {document.id}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {document.isLocked ? (
                      <Badge variant="danger">
                        <Lock className="h-3 w-3 mr-1" /> Verrouillé
                      </Badge>
                    ) : (
                      <Badge variant="success">
                        <Unlock className="h-3 w-3 mr-1" /> Actif
                      </Badge>
                    )}
                    <Button variant="outline" size="sm" className="gap-1.5" onClick={handleDownload}>
                      <Download className="h-4 w-4" /> Télécharger
                    </Button>
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                  <div className="flex items-center gap-3 bg-gray-50/50 p-3 rounded border border-gray-150">
                    <Calendar className="h-4.5 w-4.5 text-gray-400" />
                    <div>
                      <p className="text-[10px] text-gray-400 font-bold uppercase">Création</p>
                      <p className="font-semibold text-gray-700">
                        {new Date(document.createdAt).toLocaleString('fr-FR')}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 bg-gray-50/50 p-3 rounded border border-gray-150">
                    <Calendar className="h-4.5 w-4.5 text-gray-400" />
                    <div>
                      <p className="text-[10px] text-gray-400 font-bold uppercase">Modification</p>
                      <p className="font-semibold text-gray-700">
                        {new Date(document.updatedAt).toLocaleString('fr-FR')}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 bg-gray-50/50 p-3 rounded border border-gray-150">
                    <UserIcon className="h-4.5 w-4.5 text-gray-400" />
                    <div>
                      <p className="text-[10px] text-gray-400 font-bold uppercase">Propriétaire ID</p>
                      <p className="font-semibold text-gray-700 truncate max-w-[180px]" title={document.ownerId}>
                        {document.ownerId}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 bg-gray-50/50 p-3 rounded border border-gray-150">
                    <TagIcon className="h-4.5 w-4.5 text-gray-400" />
                    <div>
                      <p className="text-[10px] text-gray-400 font-bold uppercase">Catégorie</p>
                      <p className="font-semibold text-gray-700">
                        {matchedCategory ? matchedCategory.name : 'Aucune catégorie'}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Tags block */}
                <div className="space-y-2 border-t border-gray-150 pt-5">
                  <h4 className="text-xs font-bold text-gray-500 uppercase">Tags associés</h4>
                  {document.tags.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {document.tags.map((tag, idx) => (
                        <Badge key={idx} variant="primary">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-gray-400">Aucun tag pour ce document.</p>
                  )}
                </div>
              </div>
            </div>

            {/* Right: Permissions Panel */}
            <div className="lg:col-span-1 space-y-6">
              <div className="bg-white border border-gray-200 rounded shadow-xs p-6 space-y-4">
                <div className="flex items-center justify-between border-b border-gray-150 pb-3">
                  <h3 className="text-sm font-bold text-gray-900 flex items-center gap-2 select-none">
                    <Shield className="h-4.5 w-4.5 text-brand" />
                    Permissions d'accès
                  </h3>
                  {canManagePermissions && (
                    <button
                      onClick={() => setPermModalOpen(true)}
                      className="text-brand hover:text-brand-hover p-1 hover:bg-red-50 rounded transition-colors cursor-pointer"
                      title="Accorder une permission"
                    >
                      <Plus className="h-5 w-5" />
                    </button>
                  )}
                </div>

                {permLoading ? (
                  <div className="py-6 text-center text-xs text-gray-400">
                    Chargement des permissions...
                  </div>
                ) : (
                  <div className="space-y-3">
                    {permissions.length === 0 ? (
                      <p className="text-xs text-gray-400 text-center py-4">
                        Aucune permission spécifique accordée.
                      </p>
                    ) : (
                      permissions.map((perm) => (
                        <div
                          key={perm.id}
                          className="border border-gray-150 rounded p-3 space-y-2 relative bg-gray-50/20"
                        >
                          <div className="flex flex-col">
                            <span className="text-[10px] font-bold text-gray-400 uppercase">
                              Cible
                            </span>
                            <span className="text-xs font-semibold text-gray-800 truncate pr-6">
                              {perm.userId ? `User: ${perm.userId}` : `Group: ${perm.groupId}`}
                            </span>
                          </div>

                          <div className="flex flex-wrap gap-1">
                            {perm.canRead && <Badge variant="success">Read</Badge>}
                            {perm.canWrite && <Badge variant="primary">Write</Badge>}
                            {perm.canDelete && <Badge variant="danger">Delete</Badge>}
                            {perm.canShareOrManage && <Badge variant="warning">Manage</Badge>}
                            {perm.inherited && <Badge variant="secondary">Hérité</Badge>}
                          </div>

                          {canManagePermissions && !perm.inherited && (
                            <button
                              onClick={() => revokePermissionMutation.mutate(perm.id)}
                              className="absolute top-2 right-2 text-gray-400 hover:text-red-600 transition-colors p-1 rounded hover:bg-red-50 cursor-pointer"
                              title="Révoquer la permission"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          )}
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )
      )}

      {/* GRANT PERMISSION MODAL */}
      <Modal
        isOpen={permModalOpen}
        onClose={() => setPermModalOpen(false)}
        title="Accorder une permission"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setPermModalOpen(false)}>
              Annuler
            </Button>
            <Button
              size="sm"
              onClick={handleGrantPermission}
              isLoading={grantPermissionMutation.isPending}
            >
              Accorder
            </Button>
          </>
        }
      >
        <form onSubmit={handleGrantPermission} className="space-y-4">
          <div className="bg-blue-50 text-blue-800 p-3 rounded text-[11px] leading-relaxed flex gap-2">
            <Info className="h-4.5 w-4.5 flex-shrink-0" />
            <span>
              Saisissez l'UUID de l'utilisateur ou du groupe auquel vous souhaitez accorder des droits d'accès.
            </span>
          </div>

          <Input
            label="ID Utilisateur (UUID)"
            placeholder="Ex: 550e8400-e29b-41d4-a716-446655440000"
            value={targetUserId}
            onChange={(e) => {
              setTargetUserId(e.target.value);
              if (e.target.value.trim()) setTargetGroupId(''); // Mutually exclusive visual help
            }}
          />

          <div className="relative flex py-1 items-center">
            <div className="flex-grow border-t border-gray-200"></div>
            <span className="flex-shrink mx-4 text-xs font-bold text-gray-400">OU</span>
            <div className="flex-grow border-t border-gray-200"></div>
          </div>

          <Input
            label="ID Groupe (UUID)"
            placeholder="Ex: d84bb25a-4712-4cf3-a602-98448286a11b"
            value={targetGroupId}
            onChange={(e) => {
              setTargetGroupId(e.target.value);
              if (e.target.value.trim()) setTargetUserId(''); // Mutually exclusive visual help
            }}
          />

          <div className="space-y-2 border-t border-gray-150 pt-4">
            <label className="text-xs font-bold text-gray-700 block uppercase tracking-wider mb-2">
              Droits d'accès
            </label>

            <div className="grid grid-cols-2 gap-3">
              <label className="flex items-center gap-2 p-2.5 border border-gray-200 rounded text-xs font-semibold hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={canRead}
                  onChange={(e) => setCanRead(e.target.checked)}
                  className="rounded text-brand focus:ring-brand h-4 w-4"
                />
                Lecture (Read)
              </label>

              <label className="flex items-center gap-2 p-2.5 border border-gray-200 rounded text-xs font-semibold hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={canWrite}
                  onChange={(e) => setCanWrite(e.target.checked)}
                  className="rounded text-brand focus:ring-brand h-4 w-4"
                />
                Écriture (Write)
              </label>

              <label className="flex items-center gap-2 p-2.5 border border-gray-200 rounded text-xs font-semibold hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={canDelete}
                  onChange={(e) => setCanDelete(e.target.checked)}
                  className="rounded text-brand focus:ring-brand h-4 w-4"
                />
                Suppression (Delete)
              </label>

              <label className="flex items-center gap-2 p-2.5 border border-gray-200 rounded text-xs font-semibold hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={canShareOrManage}
                  onChange={(e) => setCanShareOrManage(e.target.checked)}
                  className="rounded text-brand focus:ring-brand h-4 w-4"
                />
                Partager/Gérer
              </label>
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
};
