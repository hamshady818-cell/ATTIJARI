import React, { useState, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { categoriesApi } from '../api/categoriesApi';
import { metadataApi, MetadataDefinition } from '../api/metadataApi';
import { CategoryItem } from '../types';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../utils/errorMessages';
import { Header } from '../components/layout/Header';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { ConfirmModal } from '../components/ui/ConfirmModal';
import {
  Layers,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  X,
  Check,
  RotateCcw,
  Archive,
  Search,
  Eye,
  Power,
  FolderKanban,
  Filter,
  Tag,
  Folder,
  FileText,
  Briefcase,
  Shield,
  Award,
  Bookmark,
  Grid,
  CheckCircle2,
  Sliders,
  FileCode,
  AlertCircle,
} from 'lucide-react';

const ICON_OPTIONS = [
  { value: 'FolderKanban', label: 'Dossier Projet (FolderKanban)', icon: FolderKanban },
  { value: 'Folder', label: 'Dossier Simple (Folder)', icon: Folder },
  { value: 'FileText', label: 'Document (FileText)', icon: FileText },
  { value: 'Briefcase', label: 'Affaires / Finance (Briefcase)', icon: Briefcase },
  { value: 'Shield', label: 'Sécurité / Juridique (Shield)', icon: Shield },
  { value: 'Award', label: 'Qualité / Audit (Award)', icon: Award },
  { value: 'Bookmark', label: 'Marque-page (Bookmark)', icon: Bookmark },
  { value: 'Grid', label: 'Grille (Grid)', icon: Grid },
];

export const CategoriesPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Tab State
  const [viewMode, setViewMode] = useState<'ACTIVE' | 'DELETED'>('ACTIVE');

  // Search & Filters State
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [parentFilter, setParentFilter] = useState<string>('ALL');

  // Query Active & Deleted Categories
  const { data: activeCategories = [], isLoading: isLoadingActive, isFetching: isFetchingActive } = useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.list(),
  });

  const { data: deletedCategories = [], isLoading: isLoadingDeleted, isFetching: isFetchingDeleted } = useQuery({
    queryKey: ['categories-deleted'],
    queryFn: () => categoriesApi.listDeleted(),
  });

  const isLoading = viewMode === 'ACTIVE' ? isLoadingActive : isLoadingDeleted;
  const isFetching = viewMode === 'ACTIVE' ? isFetchingActive : isFetchingDeleted;

  const currentCategoryList: CategoryItem[] = useMemo(() => {
    return viewMode === 'ACTIVE' ? activeCategories : deletedCategories;
  }, [viewMode, activeCategories, deletedCategories]);

  // Build parent lookup map for parent names
  const categoryMap = useMemo(() => {
    const map = new Map<string, CategoryItem>();
    activeCategories.forEach((c) => map.set(c.id, c));
    deletedCategories.forEach((c) => map.set(c.id, c));
    return map;
  }, [activeCategories, deletedCategories]);

  // Client-side filtered list
  const filteredList = useMemo(() => {
    return currentCategoryList.filter((item) => {
      // Search in name, description, path
      const query = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !query ||
        item.name.toLowerCase().includes(query) ||
        (item.description && item.description.toLowerCase().includes(query)) ||
        (item.path && item.path.toLowerCase().includes(query));

      // Status filter
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && item.active) ||
        (statusFilter === 'INACTIVE' && !item.active);

      // Parent filter
      const matchesParent =
        parentFilter === 'ALL' ||
        (parentFilter === 'ROOT' && !item.parentId) ||
        (parentFilter === 'HAS_PARENT' && Boolean(item.parentId));

      return matchesSearch && matchesStatus && matchesParent;
    });
  }, [currentCategoryList, searchQuery, statusFilter, parentFilter]);

  // Modals State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<CategoryItem | null>(null);
  const [editingCategory, setEditingCategory] = useState<CategoryItem | null>(null);

  // Detail Modal Linked Metadata Definitions
  const { data: categoryMetadataPage, isLoading: isLoadingCategoryMetadata } = useQuery({
    queryKey: ['metadata-definitions-category', selectedCategory?.id],
    queryFn: () => metadataApi.getByCategory(selectedCategory?.id, 0, 100),
    enabled: Boolean(selectedCategory && isDetailModalOpen),
  });

  const categoryMetadataList: MetadataDefinition[] = useMemo(() => {
    if (!categoryMetadataPage) return [];
    if (Array.isArray(categoryMetadataPage)) return categoryMetadataPage;
    return categoryMetadataPage.content || [];
  }, [categoryMetadataPage]);

  // Form Fields State
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [parentId, setParentId] = useState<string>('');
  const [color, setColor] = useState('#C8102E');
  const [icon, setIcon] = useState('FolderKanban');
  const [securityClass, setSecurityClass] = useState('');
  const [active, setActive] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Confirm Delete State
  const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);
  const [deletingCategory, setDeletingCategory] = useState<CategoryItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Action Loading States
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [restoringId, setRestoringId] = useState<string | null>(null);

  // Open Create Form Modal
  const openCreateModal = () => {
    setEditingCategory(null);
    setName('');
    setDescription('');
    setParentId('');
    setColor('#C8102E');
    setIcon('FolderKanban');
    setSecurityClass('');
    setActive(true);
    setIsFormModalOpen(true);
  };

  // Open Edit Form Modal
  const openEditModal = (cat: CategoryItem) => {
    setEditingCategory(cat);
    setName(cat.name);
    setDescription(cat.description || '');
    setParentId(cat.parentId || '');
    setColor(cat.color || '#C8102E');
    setIcon(cat.icon || 'FolderKanban');
    setSecurityClass(cat.securityClass || '');
    setActive(cat.active !== false);
    setIsFormModalOpen(true);
  };

  // Open Detail Modal
  const openDetailModal = (cat: CategoryItem) => {
    setSelectedCategory(cat);
    setIsDetailModalOpen(true);
  };

  // Form Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      toast.error('Le nom de la catégorie est obligatoire.');
      return;
    }

    if (editingCategory && parentId === editingCategory.id) {
      toast.error('Une catégorie ne peut pas être sélectionnée comme son propre parent.');
      return;
    }

    setIsSubmitting(true);
    try {
      if (editingCategory) {
        // Update Payload
        const updatePayload = {
          name: name.trim(),
          description: description.trim() || undefined,
          parentId: parentId || undefined,
          color: color || undefined,
          icon: icon || undefined,
          securityClass: securityClass.trim() || undefined,
          active,
        };

        await categoriesApi.update(editingCategory.id, updatePayload);
        toast.success(`Catégorie "${name.trim()}" modifiée avec succès`);
      } else {
        // Create Payload
        const createPayload = {
          name: name.trim(),
          description: description.trim() || undefined,
          parentId: parentId || undefined,
          color: color || undefined,
          icon: icon || undefined,
          securityClass: securityClass.trim() || undefined,
          active,
        };

        await categoriesApi.create(createPayload);
        toast.success(`Nouvelle catégorie "${name.trim()}" créée avec succès`);
      }

      await queryClient.invalidateQueries({ queryKey: ['categories'] });
      await queryClient.invalidateQueries({ queryKey: ['categories-deleted'] });
      setIsFormModalOpen(false);
    } catch (err: any) {
      toast.error(
        extractErrorMessage(
          err,
          editingCategory ? 'Échec de la modification de la catégorie.' : 'Échec de la création de la catégorie.'
        )
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Toggle Active Status
  const handleToggleActive = async (cat: CategoryItem) => {
    setTogglingId(cat.id);
    const newActiveState = !cat.active;
    try {
      await categoriesApi.toggleActive(cat.id, newActiveState);
      toast.success(
        newActiveState
          ? `Catégorie "${cat.name}" activée avec succès`
          : `Catégorie "${cat.name}" désactivée avec succès`
      );
      await queryClient.invalidateQueries({ queryKey: ['categories'] });
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec du changement de statut.'));
    } finally {
      setTogglingId(null);
    }
  };

  // Open Delete Confirm Modal
  const openDeleteConfirm = (cat: CategoryItem) => {
    setDeletingCategory(cat);
    setIsConfirmDeleteOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deletingCategory) return;
    setIsDeleting(true);
    try {
      await categoriesApi.remove(deletingCategory.id);
      toast.success(`Catégorie "${deletingCategory.name}" déplacée dans la corbeille`);
      await queryClient.invalidateQueries({ queryKey: ['categories'] });
      await queryClient.invalidateQueries({ queryKey: ['categories-deleted'] });
      setIsConfirmDeleteOpen(false);
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la suppression de la catégorie.'));
    } finally {
      setIsDeleting(false);
      setDeletingCategory(null);
    }
  };

  // Handle Restore
  const handleRestore = async (cat: CategoryItem) => {
    setRestoringId(cat.id);
    try {
      await categoriesApi.restore(cat.id);
      toast.success(`Catégorie "${cat.name}" restaurée avec succès`);
      await queryClient.invalidateQueries({ queryKey: ['categories'] });
      await queryClient.invalidateQueries({ queryKey: ['categories-deleted'] });
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la restauration de la catégorie.'));
    } finally {
      setRestoringId(null);
    }
  };

  // Dynamic Icon Resolver Component
  const renderCategoryIcon = (iconName?: string, customColor?: string, className = 'w-4 h-4') => {
    const found = ICON_OPTIONS.find((i) => i.value === iconName);
    const IconComp = found ? found.icon : FolderKanban;
    return <IconComp className={className} style={{ color: customColor || '#C8102E' }} />;
  };

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-brand-bg text-brand-text">
      <Header />

      <main className="flex-1 overflow-y-auto p-6 max-w-7xl mx-auto w-full space-y-6">
        {/* Banner Hero Header */}
        <div className="relative overflow-hidden bg-gradient-to-r from-neutral-900 via-neutral-800 to-amber-950 border border-neutral-700/60 rounded-xl p-6 shadow-xl text-white">
          {/* Top Red Attijari Accent Line */}
          <div className="absolute top-0 left-0 right-0 h-1 bg-[#C8102E]" />

          <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-amber-500/20 backdrop-blur-md border border-amber-500/30 text-xs font-semibold text-amber-300">
                <Layers className="w-3.5 h-3.5 text-amber-400" />
                <span>Administration GED — Classification Documentaire</span>
              </div>
              <h1 className="text-2xl font-extrabold tracking-tight font-display text-white flex items-center gap-3">
                <FolderKanban className="w-6 h-6 text-[#C8102E]" />
                Catégories de documents
              </h1>
              <p className="text-xs text-neutral-300 max-w-2xl leading-relaxed">
                Configurez les catégories utilisées pour organiser et classifier les documents de la GED Attijariwafa Bank. Définissez la hiérarchie, les métadonnées spécifiques et les statuts d'activation.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <Button
                variant="primary"
                size="md"
                icon={<Plus className="w-4 h-4" />}
                onClick={openCreateModal}
                className="shadow-lg bg-[#C8102E] hover:bg-[#a00d24] text-white border-none"
              >
                Nouvelle catégorie
              </Button>
            </div>
          </div>
        </div>

        {/* Search, Tabs & Filters Card */}
        <div className="bg-brand-surface border border-brand-border rounded-xl shadow-card overflow-hidden flex flex-col space-y-4 p-4">
          {/* Tabs & Search Header */}
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 pb-4 border-b border-brand-border">
            {/* View Mode Tabs */}
            <div className="flex items-center gap-2">
              <button
                onClick={() => setViewMode('ACTIVE')}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all ${
                  viewMode === 'ACTIVE'
                    ? 'bg-[#C8102E] text-white shadow-md'
                    : 'bg-brand-alt border border-brand-border text-brand-text hover:bg-brand-border/40'
                }`}
              >
                <Layers className="w-4 h-4" />
                <span>Catégories actives</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] ${viewMode === 'ACTIVE' ? 'bg-white/20 text-white' : 'bg-brand-border text-brand-muted'}`}>
                  {activeCategories.length}
                </span>
              </button>

              <button
                onClick={() => setViewMode('DELETED')}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all ${
                  viewMode === 'DELETED'
                    ? 'bg-amber-600 text-white shadow-md'
                    : 'bg-brand-alt border border-brand-border text-brand-text hover:bg-brand-border/40'
                }`}
              >
                <Archive className="w-4 h-4" />
                <span>Corbeille</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] ${viewMode === 'DELETED' ? 'bg-white/20 text-white' : 'bg-brand-border text-brand-muted'}`}>
                  {deletedCategories.length}
                </span>
              </button>
            </div>

            {/* Refreshing indicator */}
            <div className="flex items-center gap-4 text-xs font-medium text-brand-muted">
              {isFetching && !isLoading && (
                <div className="flex items-center gap-1.5 text-brand-primary font-semibold">
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  <span>Actualisation...</span>
                </div>
              )}
            </div>
          </div>

          {/* Search and Filters Controls */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 pt-1">
            {/* Search Input */}
            <div className="relative sm:col-span-2">
              <Search className="w-4 h-4 absolute left-3 top-2.5 text-brand-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Rechercher par nom, description ou chemin..."
                className="w-full pl-9 pr-8 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text placeholder-brand-muted"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-2.5 text-brand-muted hover:text-brand-text"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            {/* Status Filter */}
            <div className="relative">
              <Filter className="w-3.5 h-3.5 absolute left-3 top-3 text-brand-muted pointer-events-none" />
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full pl-8 pr-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text appearance-none"
              >
                <option value="ALL">Tous les statuts</option>
                <option value="ACTIVE">Actifs uniquement (●)</option>
                <option value="INACTIVE">Inactifs uniquement (○)</option>
              </select>
            </div>

            {/* Parent Filter */}
            <div className="relative">
              <select
                value={parentFilter}
                onChange={(e) => setParentFilter(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text"
              >
                <option value="ALL">Toutes les hiérarchies</option>
                <option value="ROOT">Catégories racines</option>
                <option value="HAS_PARENT">Catégories avec parent</option>
              </select>
            </div>
          </div>
        </div>

        {/* Data Table Container */}
        <div className="bg-brand-surface border border-brand-border rounded-xl shadow-card overflow-hidden flex flex-col">
          <div className="overflow-x-auto">
            <table className="w-full text-left table-dense">
              <thead>
                <tr className="bg-brand-alt/50 border-b border-brand-border text-xs font-bold text-brand-muted uppercase tracking-wider">
                  <th className="py-3 px-4">Catégorie</th>
                  <th className="py-3 px-4">Description</th>
                  <th className="py-3 px-4">Parent</th>
                  <th className="py-3 px-4">Chemin</th>
                  <th className="py-3 px-4 text-center">Métadonnées</th>
                  <th className="py-3 px-4 text-center">Statut</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border font-sans text-xs">
                {isLoading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <Loader2 className="w-7 h-7 text-[#C8102E] animate-spin" />
                        <span className="text-xs font-semibold">Chargement des catégories...</span>
                      </div>
                    </td>
                  </tr>
                ) : filteredList.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-3">
                        <div className="p-4 bg-brand-alt border border-brand-border rounded-full text-brand-muted">
                          {viewMode === 'ACTIVE' ? <Layers className="w-8 h-8" /> : <Archive className="w-8 h-8" />}
                        </div>
                        <p className="text-sm font-bold text-brand-text">
                          {viewMode === 'ACTIVE'
                            ? currentCategoryList.length === 0
                              ? 'Aucune catégorie configurée'
                              : 'Aucun résultat pour cette recherche'
                            : 'Aucune catégorie dans la corbeille'}
                        </p>
                        {viewMode === 'ACTIVE' && currentCategoryList.length === 0 && (
                          <Button
                            variant="primary"
                            size="sm"
                            icon={<Plus className="w-3.5 h-3.5" />}
                            onClick={openCreateModal}
                            className="mt-2 bg-[#C8102E] hover:bg-[#a00d24] text-white border-none"
                          >
                            Créer votre première catégorie
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredList.map((cat) => {
                    const parentCat = cat.parentId ? categoryMap.get(cat.parentId) : null;
                    return (
                      <tr key={cat.id} className="group hover:bg-brand-alt/40 transition-colors">
                        {/* Catégorie */}
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2.5">
                            <div
                              className="p-1.5 rounded-lg border flex items-center justify-center shrink-0"
                              style={{
                                backgroundColor: cat.color ? `${cat.color}15` : '#C8102E15',
                                borderColor: cat.color ? `${cat.color}40` : '#C8102E40',
                              }}
                            >
                              {renderCategoryIcon(cat.icon, cat.color, 'w-4 h-4')}
                            </div>
                            <div className="flex flex-col">
                              <span className="font-bold text-xs text-brand-text flex items-center gap-1.5">
                                {cat.name}
                                {cat.color && (
                                  <span
                                    className="w-2.5 h-2.5 rounded-full inline-block shrink-0"
                                    style={{ backgroundColor: cat.color }}
                                    title={`Couleur: ${cat.color}`}
                                  />
                                )}
                              </span>
                              {cat.securityClass && (
                                <span className="text-[9px] font-mono text-brand-muted uppercase">
                                  Classe: {cat.securityClass}
                                </span>
                              )}
                            </div>
                          </div>
                        </td>

                        {/* Description */}
                        <td className="py-3 px-4">
                          {cat.description ? (
                            <span className="text-[11px] text-brand-muted line-clamp-1 italic">
                              {cat.description}
                            </span>
                          ) : (
                            <span className="text-brand-muted text-[11px]">—</span>
                          )}
                        </td>

                        {/* Parent */}
                        <td className="py-3 px-4">
                          {parentCat ? (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-brand-alt border border-brand-border text-brand-text">
                              <FolderKanban className="w-3 h-3 text-brand-muted" />
                              <span>{parentCat.name}</span>
                            </span>
                          ) : (
                            <span className="text-brand-muted text-[11px] italic">Racine</span>
                          )}
                        </td>

                        {/* Chemin */}
                        <td className="py-3 px-4">
                          <span className="font-mono text-[10px] text-brand-muted bg-brand-alt/60 px-2 py-0.5 rounded border border-brand-border">
                            {cat.path || cat.name}
                          </span>
                        </td>

                        {/* Métadonnées count */}
                        <td className="py-3 px-4 text-center">
                          {cat.metadataCount && cat.metadataCount > 0 ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-500/10 text-blue-700 border border-blue-500/20">
                              <Sliders className="w-3 h-3" />
                              <span>{cat.metadataCount} métadonné{cat.metadataCount > 1 ? 'es' : 'e'}</span>
                            </span>
                          ) : (
                            <span className="text-brand-muted text-[11px]">Aucune</span>
                          )}
                        </td>

                        {/* Statut */}
                        <td className="py-3 px-4 text-center">
                          {cat.active !== false ? (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-700 border border-emerald-500/20">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                              <span>Actif</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold bg-slate-500/10 text-slate-500 border border-slate-500/20">
                              <span className="w-1.5 h-1.5 rounded-full bg-slate-400" />
                              <span>Inactif</span>
                            </span>
                          )}
                        </td>

                        {/* Actions */}
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {/* Voir */}
                            <Button
                              variant="ghost"
                              size="sm"
                              icon={<Eye className="w-3.5 h-3.5 text-brand-text" />}
                              onClick={() => openDetailModal(cat)}
                              title="Voir la fiche détaillée"
                              className="p-1.5 hover:bg-brand-alt"
                            />

                            {viewMode === 'ACTIVE' ? (
                              <>
                                {/* Modifier */}
                                <Button
                                  variant="outline"
                                  size="sm"
                                  icon={<Pencil className="w-3.5 h-3.5 text-brand-primary" />}
                                  onClick={() => openEditModal(cat)}
                                  title="Modifier"
                                >
                                  Modifier
                                </Button>

                                {/* Toggle Activer / Désactiver */}
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  icon={togglingId === cat.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Power className={`w-3.5 h-3.5 ${cat.active ? 'text-amber-600' : 'text-emerald-600'}`} />}
                                  onClick={() => handleToggleActive(cat)}
                                  disabled={togglingId === cat.id}
                                  title={cat.active ? 'Désactiver' : 'Activer'}
                                  className={cat.active ? 'hover:bg-amber-500/10 hover:text-amber-700' : 'hover:bg-emerald-500/10 hover:text-emerald-700'}
                                />

                                {/* Supprimer (Soft Delete) */}
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  icon={<Trash2 className="w-3.5 h-3.5 text-red-500" />}
                                  onClick={() => openDeleteConfirm(cat)}
                                  title="Supprimer (Déplacer vers corbeille)"
                                  className="hover:bg-red-500/10 hover:text-red-600"
                                />
                              </>
                            ) : (
                              /* Restaurer depuis corbeille */
                              <Button
                                variant="outline"
                                size="sm"
                                icon={restoringId === cat.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RotateCcw className="w-3.5 h-3.5 text-amber-600" />}
                                onClick={() => handleRestore(cat)}
                                disabled={restoringId === cat.id}
                                className="hover:bg-amber-500/10 hover:text-amber-600"
                              >
                                Restaurer
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODAL : Création / Modification                                         */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingCategory ? 'Modifier la catégorie' : 'Nouvelle catégorie de document'}
        maxWidth="lg"
      >
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          {/* Top Red Bar */}
          <div className="h-1 bg-[#C8102E] -mt-4 -mx-6 mb-4" />

          {/* Nom & Catégorie Parente */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-brand-text mb-1">
                Nom de la catégorie <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="ex: Factures, Contrats, RH, Juridique"
                required
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text font-semibold"
              />
            </div>

            <div>
              <label className="block font-bold text-brand-text mb-1">
                Catégorie parente
              </label>
              <select
                value={parentId}
                onChange={(e) => setParentId(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text"
              >
                <option value="">-- Aucune (Catégorie Racine) --</option>
                {activeCategories
                  .filter((c) => !editingCategory || c.id !== editingCategory.id)
                  .map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
              </select>
              <p className="text-[10px] text-brand-muted mt-1">Permet de créer une arborescence hiérarchique.</p>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className="block font-bold text-brand-text mb-1">
              Description
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={2}
              placeholder="ex: Classification des pièces comptables et factures fournisseurs..."
              className="w-full px-3 py-1.5 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text"
            />
          </div>

          {/* Couleur & Icône */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-brand-alt/40 p-3.5 border border-brand-border rounded-xl">
            <div>
              <label className="block font-bold text-brand-text mb-1 flex items-center justify-between">
                <span>Couleur d'accent</span>
                {color && (
                  <span className="inline-flex items-center gap-1 text-[10px] font-mono">
                    <span className="w-3 h-3 rounded-full border border-black/20" style={{ backgroundColor: color }} />
                    {color}
                  </span>
                )}
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="color"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="w-10 h-8 p-0.5 border border-brand-border rounded cursor-pointer bg-brand-surface"
                />
                <input
                  type="text"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  placeholder="#C8102E"
                  className="flex-1 px-3 py-1.5 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] font-mono"
                />
              </div>
            </div>

            <div>
              <label className="block font-bold text-brand-text mb-1 flex items-center gap-1.5">
                <span>Icône</span>
                <span className="p-1 rounded bg-brand-surface border border-brand-border ml-auto">
                  {renderCategoryIcon(icon, color, 'w-3.5 h-3.5')}
                </span>
              </label>
              <select
                value={icon}
                onChange={(e) => setIcon(e.target.value)}
                className="w-full px-3 py-1.5 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
              >
                {ICON_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Classe de Sécurité & Statut */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-brand-text mb-1">
                Classe de sécurité (Optionnel)
              </label>
              <input
                type="text"
                value={securityClass}
                onChange={(e) => setSecurityClass(e.target.value)}
                placeholder="ex: RESTRICTED, CONFIDENTIAL, PUBLIC"
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text font-mono"
              />
            </div>

            <div className="flex items-end pb-2">
              <label className="inline-flex items-center gap-2.5 cursor-pointer bg-brand-alt/50 p-2.5 border border-brand-border rounded-lg w-full">
                <input
                  type="checkbox"
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  className="w-4 h-4 text-emerald-600 border-brand-border rounded focus:ring-emerald-500"
                />
                <div className="flex flex-col">
                  <span className="font-bold text-emerald-700 text-xs">Catégorie active</span>
                  <span className="text-[10px] text-brand-muted">Disponible immédiatement lors des versements</span>
                </div>
              </label>
            </div>
          </div>

          {/* Modal Actions */}
          <div className="flex justify-end gap-2.5 pt-3 border-t border-brand-border">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsFormModalOpen(false)}
              disabled={isSubmitting}
            >
              Annuler
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={isSubmitting}
              icon={<Check className="w-4 h-4" />}
              className="bg-[#C8102E] hover:bg-[#a00d24] text-white border-none"
            >
              {editingCategory ? 'Enregistrer les modifications' : 'Créer la catégorie'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODAL : Consultation / Fiche détaillée (Voir)                           */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="Fiche détaillée de catégorie"
        maxWidth="lg"
      >
        {selectedCategory && (
          <div className="space-y-4 text-xs">
            {/* Top Red Bar */}
            <div className="h-1 bg-[#C8102E] -mt-4 -mx-6 mb-4" />

            {/* Category Banner Card */}
            <div
              className="p-4 rounded-xl border flex items-center justify-between shadow-xs"
              style={{
                backgroundColor: selectedCategory.color ? `${selectedCategory.color}10` : '#C8102E10',
                borderColor: selectedCategory.color ? `${selectedCategory.color}30` : '#C8102E30',
              }}
            >
              <div className="flex items-center gap-3">
                <div
                  className="p-2.5 rounded-xl border flex items-center justify-center bg-white shadow-xs"
                  style={{ borderColor: selectedCategory.color || '#C8102E' }}
                >
                  {renderCategoryIcon(selectedCategory.icon, selectedCategory.color, 'w-6 h-6')}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-brand-text flex items-center gap-2">
                    {selectedCategory.name}
                    {selectedCategory.color && (
                      <span
                        className="w-3 h-3 rounded-full border border-black/20"
                        style={{ backgroundColor: selectedCategory.color }}
                      />
                    )}
                  </h3>
                  <p className="font-mono text-[11px] text-brand-muted">Chemin : {selectedCategory.path || selectedCategory.name}</p>
                </div>
              </div>

              <div>
                {selectedCategory.active !== false ? (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-700 border border-emerald-500/20">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                    <span>Actif</span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-slate-500/10 text-slate-500 border border-slate-500/20">
                    <span className="w-2 h-2 rounded-full bg-slate-400" />
                    <span>Inactif</span>
                  </span>
                )}
              </div>
            </div>

            {/* Grid properties */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-3 bg-brand-surface border border-brand-border rounded-xl">
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Parent</span>
                <p className="font-semibold text-brand-text mt-0.5">
                  {selectedCategory.parentId ? categoryMap.get(selectedCategory.parentId)?.name || selectedCategory.parentId : 'Racine'}
                </p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Classe de sécurité</span>
                <p className="font-mono font-semibold text-brand-text mt-0.5">{selectedCategory.securityClass || '—'}</p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Créée le</span>
                <p className="font-mono text-brand-text mt-0.5">
                  {selectedCategory.createdAt ? new Date(selectedCategory.createdAt).toLocaleDateString('fr-FR') : '—'}
                </p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Mise à jour</span>
                <p className="font-mono text-brand-text mt-0.5">
                  {selectedCategory.updatedAt ? new Date(selectedCategory.updatedAt).toLocaleDateString('fr-FR') : '—'}
                </p>
              </div>
            </div>

            {/* Description */}
            {selectedCategory.description && (
              <div className="p-3 bg-brand-alt/40 border border-brand-border rounded-xl">
                <span className="text-[10px] font-bold uppercase text-brand-muted">Description</span>
                <p className="text-brand-text mt-1 leading-relaxed">{selectedCategory.description}</p>
              </div>
            )}

            {/* Section Métadonnées Associées */}
            <div className="p-4 bg-brand-surface border border-brand-border rounded-xl space-y-3">
              <div className="flex items-center justify-between border-b border-brand-border pb-2">
                <span className="text-xs font-bold uppercase tracking-wider text-brand-text flex items-center gap-2">
                  <Sliders className="w-4 h-4 text-[#C8102E]" />
                  <span>Métadonnées associées ({categoryMetadataList.length})</span>
                </span>
                <span className="text-[10px] text-brand-muted">Définitions s'appliquant spécifiquement à cette catégorie</span>
              </div>

              {isLoadingCategoryMetadata ? (
                <div className="py-6 text-center text-brand-muted flex items-center justify-center gap-2">
                  <Loader2 className="w-4 h-4 text-[#C8102E] animate-spin" />
                  <span>Chargement des métadonnées associées...</span>
                </div>
              ) : categoryMetadataList.length === 0 ? (
                <div className="py-6 text-center text-brand-muted italic text-[11px]">
                  Aucune métadonnée spécifique associée à cette catégorie.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left">
                    <thead>
                      <tr className="bg-brand-alt/50 border-b border-brand-border text-[10px] font-bold text-brand-muted uppercase">
                        <th className="py-2 px-3">Nom technique</th>
                        <th className="py-2 px-3">Libellé</th>
                        <th className="py-2 px-3">Type</th>
                        <th className="py-2 px-3 text-center">Obligatoire</th>
                        <th className="py-2 px-3 text-center">Statut</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-brand-border text-xs">
                      {categoryMetadataList.map((meta) => (
                        <tr key={meta.id} className="hover:bg-brand-alt/30">
                          <td className="py-2 px-3 font-mono text-xs font-bold text-[#C8102E]">{meta.name}</td>
                          <td className="py-2 px-3 font-semibold text-brand-text">{meta.label}</td>
                          <td className="py-2 px-3 font-mono text-[10px] uppercase">{meta.type}</td>
                          <td className="py-2 px-3 text-center">
                            {meta.required ? (
                              <span className="text-amber-700 font-bold">OUI</span>
                            ) : (
                              <span className="text-brand-muted">NON</span>
                            )}
                          </td>
                          <td className="py-2 px-3 text-center">
                            {meta.active ? (
                              <span className="text-emerald-700 font-bold">Actif</span>
                            ) : (
                              <span className="text-slate-400">Inactif</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-2">
              <Button variant="outline" size="sm" onClick={() => setIsDetailModalOpen(false)}>
                Fermer
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODAL : Confirmation de Suppression                                   */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      <ConfirmModal
        isOpen={isConfirmDeleteOpen}
        onClose={() => setIsConfirmDeleteOpen(false)}
        onConfirm={handleConfirmDelete}
        title="Supprimer la catégorie ?"
        message={`La catégorie "${deletingCategory?.name}" sera déplacée vers la corbeille. Les documents existants ne seront pas supprimés.`}
        confirmText="Déplacer vers la corbeille"
        cancelText="Annuler"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default CategoriesPage;
