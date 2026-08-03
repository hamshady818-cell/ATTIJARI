import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/client';
import { useAuth } from '../hooks/useAuth';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Modal } from '../components/ui/Modal';
import { Table, Thead, Tbody, Tr, Th, Td } from '../components/ui/Table';
import { Badge } from '../components/ui/Badge';
import { toast } from '../components/ui/Toast';
import { mapErrorCodeToMessage } from '../api/client';
import type {
  CategoryResponseDto,
  DepartmentResponseDto,
  MetadataDefinitionResponseDto,
  TagResponseDto,
  RoleResponseDto,
  AuditLogResponseDto,
  ApiErrorResponse,
  MetadataType,
} from '../types';
import {
  Layers,
  Building,
  Sliders,
  Tags as TagsIcon,
  Shield,
  History,
  Plus,
  Edit2,
  Trash2,
  Lock,
} from 'lucide-react';

export const AdminPage: React.FC = () => {
  const { tab = 'categories' } = useParams<{ tab: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasAnyRole } = useAuth();

  const isAuthorized = hasAnyRole(['SUPER_ADMIN', 'ADMIN', 'MANAGER']);

  // Shared Modal states
  const [activeModal, setActiveModal] = useState<'create' | 'edit' | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Form states
  const [catName, setCatName] = useState('');
  const [catParentId, setCatParentId] = useState('');

  const [deptName, setDeptName] = useState('');
  const [deptParentId, setDeptParentId] = useState('');

  const [metaName, setMetaName] = useState('');
  const [metaLabel, setMetaLabel] = useState('');
  const [metaType, setMetaType] = useState<MetadataType>('STRING');
  const [metaRequired, setMetaRequired] = useState(false);
  const [metaPattern, setMetaPattern] = useState('');

  const [tagName, setTagName] = useState('');
  const [tagDesc, setTagDesc] = useState('');

  // 1. Fetch Operations
  const { data: categories = [], isLoading: catLoading } = useQuery<CategoryResponseDto[]>({
    queryKey: ['categories'],
    queryFn: async () => {
      const res = await api.get('/categories');
      return res.data;
    },
    enabled: isAuthorized && tab === 'categories',
  });

  const { data: departments = [], isLoading: deptLoading } = useQuery<DepartmentResponseDto[]>({
    queryKey: ['departments'],
    queryFn: async () => {
      const res = await api.get('/departments');
      return res.data;
    },
    enabled: isAuthorized && tab === 'departments',
  });

  const { data: metadataDefs = [], isLoading: metaLoading } = useQuery<MetadataDefinitionResponseDto[]>({
    queryKey: ['metadataDefs'],
    queryFn: async () => {
      const res = await api.get('/metadata-definitions');
      return res.data;
    },
    enabled: isAuthorized && tab === 'metadata',
  });

  const { data: tags = [], isLoading: tagLoading } = useQuery<TagResponseDto[]>({
    queryKey: ['tags'],
    queryFn: async () => {
      const res = await api.get('/tags');
      return res.data;
    },
    enabled: isAuthorized && tab === 'tags',
  });

  const { data: roles = [], isLoading: roleLoading } = useQuery<RoleResponseDto[]>({
    queryKey: ['roles'],
    queryFn: async () => {
      const res = await api.get('/roles');
      return res.data;
    },
    enabled: isAuthorized && tab === 'roles',
  });

  const { data: auditLogs = [], isLoading: auditLoading } = useQuery<AuditLogResponseDto[]>({
    queryKey: ['auditLogs'],
    queryFn: async () => {
      const res = await api.get('/audit-logs');
      return res.data;
    },
    enabled: isAuthorized && tab === 'audit',
  });

  // 2. Mutation Operations
  const createMutation = useMutation({
    mutationFn: async (payload: any) => {
      const paths = {
        categories: '/categories',
        departments: '/departments',
        metadata: '/metadata-definitions',
        tags: '/tags',
      };
      const endpoint = (paths as any)[tab];
      if (endpoint) {
        const res = await api.post(endpoint, payload);
        return res.data;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [getQueryKey(tab)] });
      toast.success('Élément créé avec succès.');
      closeForm();
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Erreur de création.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (variables: { id: string; payload: any }) => {
      const paths = {
        categories: `/categories/${variables.id}`,
        departments: `/departments/${variables.id}`,
        metadata: `/metadata-definitions/${variables.id}`,
      };
      const endpoint = (paths as any)[tab];
      if (endpoint) {
        // Categories/Departments use PATCH, Metadata can use PATCH too
        const res = await api.patch(endpoint, variables.payload);
        return res.data;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [getQueryKey(tab)] });
      toast.success('Élément mis à jour.');
      closeForm();
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Erreur de mise à jour.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      const paths = {
        categories: `/categories/${id}`,
        departments: `/departments/${id}`,
        metadata: `/metadata-definitions/${id}`,
        tags: `/tags/${id}`,
      };
      const endpoint = (paths as any)[tab];
      if (endpoint) {
        await api.delete(endpoint);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [getQueryKey(tab)] });
      toast.success('Élément supprimé.');
    },
    onError: (err: any) => {
      const apiErr = err.response?.data as ApiErrorResponse;
      toast.error(apiErr ? mapErrorCodeToMessage(apiErr) : 'Impossible de supprimer cet élément.');
    },
  });

  const getQueryKey = (t: string) => {
    if (t === 'metadata') return 'metadataDefs';
    return t;
  };

  // Helper form functions
  const closeForm = () => {
    setActiveModal(null);
    setSelectedId(null);
    setCatName('');
    setCatParentId('');
    setDeptName('');
    setDeptParentId('');
    setMetaName('');
    setMetaLabel('');
    setMetaType('STRING');
    setMetaRequired(false);
    setMetaPattern('');
    setTagName('');
    setTagDesc('');
  };

  const handleCreateOpen = () => {
    setActiveModal('create');
  };

  const handleEditOpen = (item: any) => {
    setSelectedId(item.id);
    setActiveModal('edit');
    if (tab === 'categories') {
      setCatName(item.name);
      setCatParentId(item.parentId || '');
    } else if (tab === 'departments') {
      setDeptName(item.name);
      setDeptParentId(item.parentId || '');
    } else if (tab === 'metadata') {
      setMetaName(item.name);
      setMetaLabel(item.label);
      setMetaType(item.type);
      setMetaRequired(item.required);
      setMetaPattern(item.validationPattern || '');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let payload: any = {};

    if (tab === 'categories') {
      payload = { name: catName, parentId: catParentId || null };
    } else if (tab === 'departments') {
      payload = { name: deptName, parentId: deptParentId || null };
    } else if (tab === 'metadata') {
      payload = {
        name: metaName,
        label: metaLabel,
        type: metaType,
        required: metaRequired,
        validationPattern: metaPattern || null,
      };
    } else if (tab === 'tags') {
      payload = { name: tagName, description: tagDesc };
    }

    if (activeModal === 'create') {
      createMutation.mutate(payload);
    } else if (activeModal === 'edit' && selectedId) {
      updateMutation.mutate({ id: selectedId, payload });
    }
  };

  if (!isAuthorized) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 p-8 rounded-md text-center max-w-lg mx-auto space-y-4">
        <Lock className="h-10 w-10 text-red-500 mx-auto" />
        <h2 className="text-base font-bold">Accès Non Autorisé</h2>
        <p className="text-xs leading-relaxed text-red-600">
          Vous ne disposez pas des privilèges administrateur nécessaires pour accéder à ces fonctionnalités système.
        </p>
      </div>
    );
  }

  const menuItems = [
    { key: 'categories', label: 'Catégories', icon: <Layers className="h-4 w-4" /> },
    { key: 'departments', label: 'Départements', icon: <Building className="h-4 w-4" /> },
    { key: 'metadata', label: 'Métadonnées', icon: <Sliders className="h-4 w-4" /> },
    { key: 'tags', label: 'Tags', icon: <TagsIcon className="h-4 w-4" /> },
    { key: 'roles', label: 'Rôles', icon: <Shield className="h-4 w-4" /> },
    { key: 'audit', label: 'Audit logs', icon: <History className="h-4 w-4" /> },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div>
        <h1 className="text-xl font-bold text-gray-900 tracking-tight">Panneau d'administration</h1>
        <p className="text-xs text-gray-400">
          Gérez le référentiel des catégories, départements, métadonnées, tags et consultez les journaux d'audit.
        </p>
      </div>

      {/* Tabs list container */}
      <div className="flex border-b border-gray-200 bg-white rounded-t border-t border-x px-4 pt-2 gap-1 select-none">
        {menuItems.map((item) => {
          const active = tab === item.key;
          return (
            <button
              key={item.key}
              onClick={() => navigate(`/admin/${item.key}`)}
              className={`flex items-center gap-2 px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
                active
                  ? 'border-brand text-brand font-bold'
                  : 'border-transparent text-gray-500 hover:text-gray-900 hover:border-gray-200'
              }`}
            >
              {item.icon}
              {item.label}
            </button>
          );
        })}
      </div>

      {/* Tab Workspaces Content */}
      <div className="bg-white border border-gray-200 border-t-0 rounded-b p-6 shadow-xs min-h-[400px]">
        {/* TOP WORKBAR */}
        {tab !== 'roles' && tab !== 'audit' && (
          <div className="flex justify-end mb-4">
            <Button size="sm" className="gap-1.5" onClick={handleCreateOpen}>
              <Plus className="h-4 w-4" />
              Ajouter
            </Button>
          </div>
        )}

        {/* 1. CATEGORIES WORKSPACE */}
        {tab === 'categories' && (
          catLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>ID</Th>
                  <Th>Nom</Th>
                  <Th>Chemin (Path)</Th>
                  <Th>ID Parent</Th>
                  <Th className="text-center w-28">Actions</Th>
                </tr>
              </Thead>
              <Tbody>
                {categories.length === 0 && (
                  <Tr><Td colSpan={5} className="text-center text-gray-400">Aucune catégorie.</Td></Tr>
                )}
                {categories.map((cat) => (
                  <Tr key={cat.id}>
                    <Td className="font-semibold text-gray-800 text-xs">{cat.id}</Td>
                    <Td className="font-semibold text-gray-900">{cat.name}</Td>
                    <Td className="text-gray-500">{cat.path || '—'}</Td>
                    <Td className="text-gray-400">{cat.parentId || '—'}</Td>
                    <Td className="flex justify-center gap-1.5">
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-gray-150" onClick={() => handleEditOpen(cat)}>
                        <Edit2 className="h-4 w-4 text-gray-500" />
                      </Button>
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-red-50" onClick={() => deleteMutation.mutate(cat.id)}>
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}

        {/* 2. DEPARTMENTS WORKSPACE */}
        {tab === 'departments' && (
          deptLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>ID</Th>
                  <Th>Nom</Th>
                  <Th>ID Parent</Th>
                  <Th className="text-center w-28">Actions</Th>
                </tr>
              </Thead>
              <Tbody>
                {departments.length === 0 && (
                  <Tr><Td colSpan={4} className="text-center text-gray-400">Aucun département.</Td></Tr>
                )}
                {departments.map((d) => (
                  <Tr key={d.id}>
                    <Td className="font-semibold text-gray-800 text-xs">{d.id}</Td>
                    <Td className="font-semibold text-gray-900">{d.name}</Td>
                    <Td className="text-gray-400">{d.parentId || '—'}</Td>
                    <Td className="flex justify-center gap-1.5">
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-gray-150" onClick={() => handleEditOpen(d)}>
                        <Edit2 className="h-4 w-4 text-gray-500" />
                      </Button>
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-red-50" onClick={() => deleteMutation.mutate(d.id)}>
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}

        {/* 3. METADATA WORKSPACE */}
        {tab === 'metadata' && (
          metaLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>Nom</Th>
                  <Th>Libellé</Th>
                  <Th>Type</Th>
                  <Th>Requis</Th>
                  <Th>Format regex</Th>
                  <Th className="text-center w-28">Actions</Th>
                </tr>
              </Thead>
              <Tbody>
                {metadataDefs.length === 0 && (
                  <Tr><Td colSpan={6} className="text-center text-gray-400">Aucune définition de métadonnées.</Td></Tr>
                )}
                {metadataDefs.map((def) => (
                  <Tr key={def.id}>
                    <Td className="font-semibold text-gray-850 text-xs">{def.name}</Td>
                    <Td className="font-semibold text-gray-900">{def.label}</Td>
                    <Td>
                      <Badge variant="primary">{def.type}</Badge>
                    </Td>
                    <Td>
                      {def.required ? (
                        <Badge variant="danger">Oui</Badge>
                      ) : (
                        <Badge variant="secondary">Non</Badge>
                      )}
                    </Td>
                    <Td className="font-mono text-xs text-gray-500">{def.validationPattern || 'Aucun'}</Td>
                    <Td className="flex justify-center gap-1.5">
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-gray-150" onClick={() => handleEditOpen(def)}>
                        <Edit2 className="h-4 w-4 text-gray-500" />
                      </Button>
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-red-50" onClick={() => deleteMutation.mutate(def.id)}>
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}

        {/* 4. TAGS WORKSPACE */}
        {tab === 'tags' && (
          tagLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>ID</Th>
                  <Th>Nom</Th>
                  <Th>Description</Th>
                  <Th className="text-center w-20">Actions</Th>
                </tr>
              </Thead>
              <Tbody>
                {tags.length === 0 && (
                  <Tr><Td colSpan={4} className="text-center text-gray-400">Aucun tag.</Td></Tr>
                )}
                {tags.map((tag) => (
                  <Tr key={tag.id}>
                    <Td className="font-semibold text-gray-800 text-xs">{tag.id}</Td>
                    <Td>
                      <Badge variant="primary">{tag.name}</Badge>
                    </Td>
                    <Td className="text-gray-500">{tag.description || '—'}</Td>
                    <Td className="flex justify-center">
                      <Button variant="ghost" size="sm" className="p-1 hover:bg-red-50" onClick={() => deleteMutation.mutate(tag.id)}>
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}

        {/* 5. ROLES WORKSPACE (Read Only) */}
        {tab === 'roles' && (
          roleLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>ID</Th>
                  <Th>Nom du Rôle</Th>
                  <Th>Description</Th>
                </tr>
              </Thead>
              <Tbody>
                {roles.length === 0 && (
                  <Tr><Td colSpan={3} className="text-center text-gray-400">Aucun rôle récupéré.</Td></Tr>
                )}
                {roles.map((r) => (
                  <Tr key={r.id}>
                    <Td className="font-semibold text-gray-800 text-xs">{r.id}</Td>
                    <Td>
                      <Badge variant="primary">{r.name}</Badge>
                    </Td>
                    <Td className="text-gray-500">{r.description || '—'}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}

        {/* 6. AUDIT LOGS WORKSPACE (Read Only) */}
        {tab === 'audit' && (
          auditLoading ? (
            <div className="py-8 text-center text-xs text-gray-400">Chargement...</div>
          ) : (
            <Table>
              <Thead>
                <tr>
                  <Th>Date & Heure</Th>
                  <Th>Action</Th>
                  <Th>Exécuté par</Th>
                  <Th>Type d'entité</Th>
                  <Th>ID Entité</Th>
                  <Th>Détails</Th>
                </tr>
              </Thead>
              <Tbody>
                {auditLogs.length === 0 && (
                  <Tr><Td colSpan={6} className="text-center text-gray-400">Aucune activité enregistrée.</Td></Tr>
                )}
                {auditLogs.map((log) => (
                  <Tr key={log.id} className="text-xs">
                    <Td className="whitespace-nowrap text-gray-550">
                      {new Date(log.timestamp).toLocaleString('fr-FR')}
                    </Td>
                    <Td className="font-semibold text-gray-900">{log.action}</Td>
                    <Td className="text-gray-700 truncate max-w-[130px]" title={log.performedBy}>
                      {log.performedBy}
                    </Td>
                    <Td>
                      <Badge variant="secondary">{log.entityType}</Badge>
                    </Td>
                    <Td className="text-gray-400">{log.entityId || '—'}</Td>
                    <Td className="text-gray-600 truncate max-w-[200px]" title={log.details}>
                      {log.details}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        )}
      </div>

      {/* ADMIN EDIT / CREATE MODALS */}
      <Modal
        isOpen={activeModal !== null}
        onClose={closeForm}
        title={`${activeModal === 'create' ? 'Créer' : 'Modifier'} — ${
          menuItems.find((i) => i.key === tab)?.label
        }`}
        footer={
          <>
            <Button variant="outline" size="sm" onClick={closeForm}>
              Annuler
            </Button>
            <Button
              size="sm"
              onClick={handleSubmit}
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              Sauvegarder
            </Button>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          {tab === 'categories' && (
            <>
              <Input
                label="Nom de la catégorie"
                placeholder="Ex: Comptabilité, Audit, RH..."
                value={catName}
                onChange={(e) => setCatName(e.target.value)}
                required
              />
              <Input
                label="ID Catégorie parente (Optionnel)"
                placeholder="Ex: parent UUID"
                value={catParentId}
                onChange={(e) => setCatParentId(e.target.value)}
              />
            </>
          )}

          {tab === 'departments' && (
            <>
              <Input
                label="Nom du département"
                placeholder="Ex: Secrétariat Général, DSI, Trésorerie..."
                value={deptName}
                onChange={(e) => setDeptName(e.target.value)}
                required
              />
              <Input
                label="ID Département parent (Optionnel)"
                placeholder="Ex: parent UUID"
                value={deptParentId}
                onChange={(e) => setDeptParentId(e.target.value)}
              />
            </>
          )}

          {tab === 'metadata' && (
            <>
              <Input
                label="Nom technique"
                placeholder="Ex: invoiceNumber, reportDate"
                value={metaName}
                onChange={(e) => setMetaName(e.target.value)}
                required
                disabled={activeModal === 'edit'} // Lock metadata tech name in edit
              />
              <Input
                label="Libellé à afficher"
                placeholder="Ex: Numéro de Facture, Date du Rapport"
                value={metaLabel}
                onChange={(e) => setMetaLabel(e.target.value)}
                required
              />
              <div className="flex flex-col gap-1 text-left">
                <label className="text-xs font-semibold text-gray-700">Type de donnée</label>
                <select
                  value={metaType}
                  onChange={(e) => setMetaType(e.target.value as MetadataType)}
                  className="px-3 py-2 border border-gray-300 rounded text-sm text-gray-800 bg-white focus:outline-none focus:ring-1 focus:ring-brand focus:border-brand"
                >
                  <option value="STRING">Chaine de caractères (String)</option>
                  <option value="INTEGER">Nombre entier (Integer)</option>
                  <option value="DATE">Date</option>
                  <option value="BOOLEAN">Booléen (Oui/Non)</option>
                </select>
              </div>
              <label className="flex items-center gap-2 p-2 border border-gray-200 rounded text-xs font-semibold hover:bg-gray-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={metaRequired}
                  onChange={(e) => setMetaRequired(e.target.checked)}
                  className="rounded text-brand focus:ring-brand h-4 w-4"
                />
                Cette métadonnée est obligatoire
              </label>
              <Input
                label="Format Regex de validation (Optionnel)"
                placeholder="Ex: ^FAC-\d{4}$"
                value={metaPattern}
                onChange={(e) => setMetaPattern(e.target.value)}
              />
            </>
          )}

          {tab === 'tags' && (
            <>
              <Input
                label="Nom du tag"
                placeholder="Ex: Urgent, Confidentiel..."
                value={tagName}
                onChange={(e) => setTagName(e.target.value)}
                required
              />
              <Input
                label="Description"
                placeholder="Ex: Concerne les documents à traitement rapide"
                value={tagDesc}
                onChange={(e) => setTagDesc(e.target.value)}
              />
            </>
          )}
        </form>
      </Modal>
    </div>
  );
};
