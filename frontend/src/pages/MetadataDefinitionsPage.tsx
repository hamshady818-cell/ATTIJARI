import React, { useState, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { metadataApi, MetadataDefinition, MetadataType } from '../api/metadataApi';
import { categoriesApi } from '../api/categoriesApi';
import { CategoryItem } from '../types';
import { toast } from 'react-hot-toast';
import { extractErrorMessage } from '../utils/errorMessages';
import { Header } from '../components/layout/Header';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { ConfirmModal } from '../components/ui/ConfirmModal';
import {
  Sliders,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  Tag,
  X,
  FileCode,
  Check,
  ListFilter,
  RotateCcw,
  Archive,
  ChevronLeft,
  ChevronRight,
  Search,
  Eye,
  Power,
  Info,
  Layers,
  ArrowUpDown,
  Filter,
  CheckCircle2,
  XCircle,
  FolderKanban,
  Globe,
  Calendar,
} from 'lucide-react';

const METADATA_TYPES: { value: MetadataType; label: string; description: string; badgeColor: string }[] = [
  { value: 'STRING', label: 'Texte court (STRING)', description: 'Chaîne de caractères simple (ex: Numéro de facture, Référence)', badgeColor: 'bg-slate-100 text-slate-800 border-slate-200' },
  { value: 'TEXT', label: 'Texte long (TEXT)', description: 'Texte multi-lignes pour descriptions ou commentaires', badgeColor: 'bg-slate-100 text-slate-800 border-slate-200' },
  { value: 'NUMBER', label: 'Nombre (NUMBER)', description: 'Valeur numérique entière ou décimale (ex: Montant HT, Quantité)', badgeColor: 'bg-amber-50 text-amber-800 border-amber-200' },
  { value: 'INTEGER', label: 'Nombre entier (INTEGER)', description: 'Valeur numérique sans décimales', badgeColor: 'bg-amber-50 text-amber-800 border-amber-200' },
  { value: 'DECIMAL', label: 'Nombre décimal (DECIMAL)', description: 'Montant avec décimales', badgeColor: 'bg-amber-50 text-amber-800 border-amber-200' },
  { value: 'DATE', label: 'Date (DATE)', description: 'Date au format JJ/MM/AAAA (ex: Date de validité)', badgeColor: 'bg-purple-50 text-purple-800 border-purple-200' },
  { value: 'DATETIME', label: 'Date & Heure (DATETIME)', description: 'Horodatage complet avec heure', badgeColor: 'bg-purple-50 text-purple-800 border-purple-200' },
  { value: 'BOOLEAN', label: 'Case à cocher (BOOLEAN)', description: 'Indicateur Vrai/Faux (ex: Confidentiel, Traité)', badgeColor: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
  { value: 'SELECT', label: 'Liste déroulante (SELECT)', description: 'Choix unique parmi une liste d\'options prédéfinies', badgeColor: 'bg-blue-50 text-blue-800 border-blue-200' },
  { value: 'MULTI_SELECT', label: 'Choix multiples (MULTI_SELECT)', description: 'Sélection de plusieurs options parmi une liste prédéfinie', badgeColor: 'bg-indigo-50 text-indigo-800 border-indigo-200' },
  { value: 'URL', label: 'Lien Web (URL)', description: 'Adresse web cliquable (http/https)', badgeColor: 'bg-sky-50 text-sky-800 border-sky-200' },
];

export const MetadataDefinitionsPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Tab & Pagination State
  const [viewMode, setViewMode] = useState<'ACTIVE' | 'DELETED'>('ACTIVE');
  const [page, setPage] = useState(0);
  const pageSize = 20;

  // Search & Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Query Categories for Selection & Table Scope Display
  const { data: categories = [] } = useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.list(),
  });

  const categoryMap = useMemo(() => {
    const map = new Map<string, CategoryItem>();
    categories.forEach((c) => map.set(c.id, c));
    return map;
  }, [categories]);

  // Query Active Definitions
  const { data: activePageData, isLoading: isLoadingActive, isFetching: isFetchingActive } = useQuery({
    queryKey: ['metadata-definitions-active', page],
    queryFn: () => metadataApi.list(page, pageSize),
  });

  // Query Deleted Definitions (Corbeille)
  const { data: deletedPageData, isLoading: isLoadingDeleted, isFetching: isFetchingDeleted } = useQuery({
    queryKey: ['metadata-definitions-deleted', page],
    queryFn: () => metadataApi.listDeleted(page, pageSize),
  });

  const activeMetadataList: MetadataDefinition[] = useMemo(() => {
    if (Array.isArray(activePageData)) return activePageData;
    return activePageData?.content || [];
  }, [activePageData]);

  const deletedMetadataList: MetadataDefinition[] = useMemo(() => {
    if (Array.isArray(deletedPageData)) return deletedPageData;
    return deletedPageData?.content || [];
  }, [deletedPageData]);

  const activeTotalElements = Array.isArray(activePageData) ? activePageData.length : (activePageData?.totalElements || 0);
  const deletedTotalElements = Array.isArray(deletedPageData) ? deletedPageData.length : (deletedPageData?.totalElements || 0);

  const currentMetadataList = viewMode === 'ACTIVE' ? activeMetadataList : deletedMetadataList;
  const totalElements = viewMode === 'ACTIVE' ? activeTotalElements : deletedTotalElements;
  const totalPages = viewMode === 'ACTIVE'
    ? (Array.isArray(activePageData) ? 1 : (activePageData?.totalPages || 0))
    : (Array.isArray(deletedPageData) ? 1 : (deletedPageData?.totalPages || 0));

  const isLoading = viewMode === 'ACTIVE' ? isLoadingActive : isLoadingDeleted;
  const isFetching = viewMode === 'ACTIVE' ? isFetchingActive : isFetchingDeleted;

  // Client-side filtered list
  const filteredList = useMemo(() => {
    return currentMetadataList.filter((item) => {
      // Search by name or label
      const matchesSearch =
        !searchQuery.trim() ||
        item.name.toLowerCase().includes(searchQuery.toLowerCase().trim()) ||
        item.label.toLowerCase().includes(searchQuery.toLowerCase().trim()) ||
        (item.description && item.description.toLowerCase().includes(searchQuery.toLowerCase().trim()));

      // Filter by type
      const matchesType = typeFilter === 'ALL' || item.type === typeFilter;

      // Filter by status (active / inactive)
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && item.active) ||
        (statusFilter === 'INACTIVE' && !item.active);

      return matchesSearch && matchesType && matchesStatus;
    });
  }, [currentMetadataList, searchQuery, typeFilter, statusFilter]);

  // Modals State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedDefinition, setSelectedDefinition] = useState<MetadataDefinition | null>(null);
  const [editingDefinition, setEditingDefinition] = useState<MetadataDefinition | null>(null);

  // Form Fields State
  const [name, setName] = useState('');
  const [label, setLabel] = useState('');
  const [type, setType] = useState<MetadataType>('STRING');
  const [description, setDescription] = useState('');
  const [defaultValue, setDefaultValue] = useState('');
  const [displayOrder, setDisplayOrder] = useState<number>(0);
  const [required, setRequired] = useState(false);
  const [searchable, setSearchable] = useState(true);
  const [filterable, setFilterable] = useState(true);
  const [active, setActive] = useState(true);
  const [validationPattern, setValidationPattern] = useState('');
  const [options, setOptions] = useState<string[]>([]);
  const [optionInput, setOptionInput] = useState('');
  const [scope, setScope] = useState<'GLOBAL' | 'CATEGORY'>('GLOBAL');
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Confirm Delete State
  const [isConfirmDeleteOpen, setIsConfirmDeleteOpen] = useState(false);
  const [deletingDefinition, setDeletingDefinition] = useState<MetadataDefinition | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Action Loading States
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [restoringId, setRestoringId] = useState<string | null>(null);

  const invalidateMetadataQueries = async () => {
    await queryClient.invalidateQueries({ queryKey: ['metadata-definitions-active'] });
    await queryClient.invalidateQueries({ queryKey: ['metadata-definitions-deleted'] });
  };

  // Open Create Form Modal
  const openCreateModal = () => {
    setEditingDefinition(null);
    setName('');
    setLabel('');
    setType('STRING');
    setDescription('');
    setDefaultValue('');
    setDisplayOrder(0);
    setRequired(false);
    setSearchable(true);
    setFilterable(true);
    setActive(true);
    setValidationPattern('');
    setOptions([]);
    setOptionInput('');
    setScope('GLOBAL');
    setSelectedCategoryId('');
    setIsFormModalOpen(true);
  };

  // Open Edit Form Modal
  const openEditModal = (def: MetadataDefinition) => {
    setEditingDefinition(def);
    setName(def.name);
    setLabel(def.label);
    setType(def.type);
    setDescription(def.description || '');
    setDefaultValue(def.defaultValue || '');
    setDisplayOrder(def.displayOrder !== undefined ? def.displayOrder : 0);
    setRequired(def.required);
    setSearchable(def.searchable !== false);
    setFilterable(def.filterable !== false);
    setActive(def.active !== false);
    setValidationPattern(def.validationPattern || '');
    setOptions(def.options || []);
    setOptionInput('');

    if (def.categoryId) {
      setScope('CATEGORY');
      setSelectedCategoryId(def.categoryId);
    } else {
      setScope('GLOBAL');
      setSelectedCategoryId('');
    }

    setIsFormModalOpen(true);
  };

  // Open Detail Modal
  const openDetailModal = (def: MetadataDefinition) => {
    setSelectedDefinition(def);
    setIsDetailModalOpen(true);
  };

  // Option Handlers for SELECT & MULTI_SELECT
  const handleAddOption = (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) e.preventDefault();
    const trimmed = optionInput.trim();
    if (!trimmed) return;
    if (options.includes(trimmed)) {
      toast.error('Cette option existe déjà dans la liste.');
      return;
    }
    setOptions([...options, trimmed]);
    setOptionInput('');
  };

  const handleOptionKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddOption();
    }
  };

  const handleRemoveOption = (indexToRemove: number) => {
    setOptions(options.filter((_, idx) => idx !== indexToRemove));
  };

  const handleUpdateOption = (indexToUpdate: number, newValue: string) => {
    const updated = [...options];
    updated[indexToUpdate] = newValue;
    setOptions(updated);
  };

  // Submit Handler (Create & Edit)
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim() || !label.trim()) {
      toast.error('Le nom technique et le libellé sont obligatoires.');
      return;
    }

    if (scope === 'CATEGORY' && !selectedCategoryId) {
      toast.error('Veuillez sélectionner une catégorie spécifique.');
      return;
    }

    const isSelectType = type === 'SELECT' || type === 'MULTI_SELECT';
    const cleanedOptions = options.map((o) => o.trim()).filter((o) => o.length > 0);

    if (isSelectType && cleanedOptions.length === 0) {
      toast.error('Ajoutez au moins une option pour ce type de métadonnée.');
      return;
    }

    const targetCategoryId = scope === 'CATEGORY' && selectedCategoryId ? selectedCategoryId : null;

    setIsSubmitting(true);
    try {
      if (editingDefinition) {
        // Update Payload (PATCH)
        const updatePayload = {
          name: name.trim(),
          label: label.trim(),
          type,
          description: description.trim() || undefined,
          defaultValue: defaultValue.trim() || undefined,
          displayOrder: Number(displayOrder) || 0,
          required,
          searchable,
          filterable,
          active,
          validationPattern: validationPattern.trim() || undefined,
          options: isSelectType ? cleanedOptions : undefined,
          categoryId: targetCategoryId,
        };

        await metadataApi.update(editingDefinition.id, updatePayload);
        toast.success('Type de métadonnée modifié avec succès');
      } else {
        // Create Payload
        const createPayload = {
          name: name.trim().toLowerCase().replace(/\s+/g, '_'),
          label: label.trim(),
          type,
          description: description.trim() || undefined,
          defaultValue: defaultValue.trim() || undefined,
          displayOrder: Number(displayOrder) || 0,
          required,
          searchable,
          filterable,
          active,
          validationPattern: validationPattern.trim() || undefined,
          options: isSelectType ? cleanedOptions : undefined,
          categoryId: targetCategoryId,
        };

        await metadataApi.create(createPayload);
        toast.success('Nouveau type de métadonnée créé avec succès');
      }

      await invalidateMetadataQueries();
      setIsFormModalOpen(false);
    } catch (err: any) {
      toast.error(
        extractErrorMessage(
          err,
          editingDefinition
            ? 'Échec de la modification du type de métadonnée.'
            : 'Échec de la création du type de métadonnée.'
        )
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Toggle Active Status
  const handleToggleActive = async (def: MetadataDefinition) => {
    setTogglingId(def.id);
    const newActiveState = !def.active;
    try {
      await metadataApi.toggleActive(def.id, newActiveState);
      toast.success(
        newActiveState
          ? `Métadonnée "${def.label}" activée avec succès`
          : `Métadonnée "${def.label}" désactivée avec succès`
      );
      await invalidateMetadataQueries();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec du changement de statut.'));
    } finally {
      setTogglingId(null);
    }
  };

  // Open Delete Confirmation
  const openDeleteConfirm = (def: MetadataDefinition) => {
    setDeletingDefinition(def);
    setIsConfirmDeleteOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!deletingDefinition) return;
    setIsDeleting(true);
    try {
      await metadataApi.remove(deletingDefinition.id);
      toast.success(`Métadonnée "${deletingDefinition.label}" déplacée dans la corbeille`);
      await invalidateMetadataQueries();
      setIsConfirmDeleteOpen(false);
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la suppression du type de métadonnée.'));
    } finally {
      setIsDeleting(false);
      setDeletingDefinition(null);
    }
  };

  // Handle Restore
  const handleRestore = async (def: MetadataDefinition) => {
    setRestoringId(def.id);
    try {
      await metadataApi.restore(def.id);
      toast.success(`Métadonnée "${def.label}" restaurée avec succès`);
      await invalidateMetadataQueries();
    } catch (err: any) {
      toast.error(extractErrorMessage(err, 'Échec de la restauration.'));
    } finally {
      setRestoringId(null);
    }
  };

  const getTypeBadge = (defType: MetadataType) => {
    const found = METADATA_TYPES.find((t) => t.value === defType);
    return (
      <span className={`inline-flex items-center px-2 py-0.5 text-[10px] font-mono font-bold tracking-wider border rounded-md uppercase ${found?.badgeColor || 'bg-slate-100 text-slate-700 border-slate-200'}`}>
        {defType}
      </span>
    );
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
                <Sliders className="w-3.5 h-3.5 text-amber-400" />
                <span>Administration GED — Schéma de Données</span>
              </div>
              <h1 className="text-2xl font-extrabold tracking-tight font-display text-white flex items-center gap-3">
                <ListFilter className="w-6 h-6 text-[#C8102E]" />
                Types de métadonnées
              </h1>
              <p className="text-xs text-neutral-300 max-w-2xl leading-relaxed">
                Gérez les champs personnalisés utilisés pour caractériser vos documents dans la GED Attijariwafa Bank. Configurez les clés techniques, libellés, listes de choix, portées (globales ou par catégorie) et règles d'affichage.
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
                Nouveau type de métadonnée
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
                onClick={() => { setViewMode('ACTIVE'); setPage(0); }}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all ${
                  viewMode === 'ACTIVE'
                    ? 'bg-[#C8102E] text-white shadow-md'
                    : 'bg-brand-alt border border-brand-border text-brand-text hover:bg-brand-border/40'
                }`}
              >
                <Sliders className="w-4 h-4" />
                <span>Définitions actives</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${viewMode === 'ACTIVE' ? 'bg-white/20 text-white' : 'bg-brand-border text-brand-muted'}`}>
                  {activeTotalElements}
                </span>
              </button>

              <button
                onClick={() => { setViewMode('DELETED'); setPage(0); }}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-bold transition-all ${
                  viewMode === 'DELETED'
                    ? 'bg-amber-600 text-white shadow-md'
                    : 'bg-brand-alt border border-brand-border text-brand-text hover:bg-brand-border/40'
                }`}
              >
                <Archive className="w-4 h-4" />
                <span>Corbeille</span>
                <span className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${viewMode === 'DELETED' ? 'bg-white/20 text-white' : 'bg-brand-border text-brand-muted'}`}>
                  {deletedTotalElements}
                </span>
              </button>
            </div>

            {/* Quick Status Count */}
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
                placeholder="Rechercher par nom technique ou libellé..."
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

            {/* Type Filter */}
            <div className="relative">
              <Filter className="w-3.5 h-3.5 absolute left-3 top-3 text-brand-muted pointer-events-none" />
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="w-full pl-8 pr-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text appearance-none"
              >
                <option value="ALL">Tous les types ({METADATA_TYPES.length})</option>
                {METADATA_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.value}
                  </option>
                ))}
              </select>
            </div>

            {/* Status Filter */}
            <div className="relative">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text"
              >
                <option value="ALL">Tous les statuts</option>
                <option value="ACTIVE">Actifs uniquement (●)</option>
                <option value="INACTIVE">Inactifs uniquement (○)</option>
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
                  <th className="py-3 px-4">Nom technique</th>
                  <th className="py-3 px-4">Libellé</th>
                  <th className="py-3 px-4">Type</th>
                  <th className="py-3 px-4">Portée</th>
                  {viewMode === 'DELETED' && <th className="py-3 px-4">Date de suppression</th>}
                  <th className="py-3 px-4 text-center">Obligatoire</th>
                  <th className="py-3 px-4 text-center">Options</th>
                  <th className="py-3 px-4 text-center">Ordre</th>
                  <th className="py-3 px-4 text-center">Statut</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-border font-sans text-xs">
                {isLoading ? (
                  <tr>
                    <td colSpan={viewMode === 'DELETED' ? 10 : 9} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-2">
                        <Loader2 className="w-7 h-7 text-[#C8102E] animate-spin" />
                        <span className="text-xs font-semibold">Chargement des métadonnées...</span>
                      </div>
                    </td>
                  </tr>
                ) : filteredList.length === 0 ? (
                  <tr>
                    <td colSpan={viewMode === 'DELETED' ? 10 : 9} className="text-center py-16 text-brand-muted">
                      <div className="flex flex-col items-center justify-center gap-3">
                        <div className="p-4 bg-brand-alt border border-brand-border rounded-full text-brand-muted">
                          {viewMode === 'ACTIVE' ? <Sliders className="w-8 h-8" /> : <Archive className="w-8 h-8" />}
                        </div>
                        <p className="text-sm font-bold text-brand-text">
                          {viewMode === 'ACTIVE'
                            ? activeMetadataList.length === 0
                              ? 'Aucun type de métadonnée configuré'
                              : 'Aucun résultat pour cette recherche'
                            : 'Aucune métadonnée dans la corbeille'}
                        </p>
                        {viewMode === 'ACTIVE' && activeMetadataList.length === 0 && (
                          <Button
                            variant="primary"
                            size="sm"
                            icon={<Plus className="w-3.5 h-3.5" />}
                            onClick={openCreateModal}
                            className="mt-2 bg-[#C8102E] hover:bg-[#a00d24] text-white border-none"
                          >
                            Créer votre premier type
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredList.map((item) => (
                    <tr key={item.id} className="group hover:bg-brand-alt/40 transition-colors">
                      {/* Nom technique */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2 font-mono text-xs font-bold text-[#C8102E]">
                          <FileCode className="w-3.5 h-3.5 text-brand-muted shrink-0" />
                          <span>{item.name}</span>
                        </div>
                      </td>

                      {/* Libellé */}
                      <td className="py-3 px-4">
                        <div className="flex flex-col">
                          <span className="font-bold text-xs text-brand-text">
                            {item.label}
                          </span>
                          {item.description && (
                            <span className="text-[11px] text-brand-muted line-clamp-1 italic">
                              {item.description}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Type */}
                      <td className="py-3 px-4">
                        {getTypeBadge(item.type)}
                      </td>

                      {/* Portée */}
                      <td className="py-3 px-4">
                        {item.categoryId ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-indigo-500/10 text-indigo-700 border border-indigo-500/20">
                            <FolderKanban className="w-3 h-3 text-indigo-600" />
                            <span>📁 {categoryMap.get(item.categoryId)?.name || 'Catégorie'}</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-slate-500/10 text-slate-700 border border-slate-500/20 font-mono">
                            <Globe className="w-3 h-3 text-slate-500" />
                            <span>🌐 Globale</span>
                          </span>
                        )}
                      </td>

                      {/* Date de suppression (Corbeille uniquement) */}
                      {viewMode === 'DELETED' && (
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-1.5 text-amber-800 font-medium text-[11px]">
                            <Calendar className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                            <span>
                              {item.deletedAt
                                ? new Date(item.deletedAt).toLocaleDateString('fr-FR', {
                                    day: '2-digit',
                                    month: '2-digit',
                                    year: 'numeric',
                                    hour: '2-digit',
                                    minute: '2-digit',
                                  })
                                : '—'}
                            </span>
                          </div>
                        </td>
                      )}

                      {/* Obligatoire */}
                      <td className="py-3 px-4 text-center">
                        {item.required ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500/10 text-amber-700 border border-amber-500/20">
                            <CheckCircle2 className="w-3 h-3 text-amber-600" />
                            <span>OUI</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-medium text-brand-muted bg-brand-alt border border-brand-border">
                            <span>NON</span>
                          </span>
                        )}
                      </td>

                      {/* Options */}
                      <td className="py-3 px-4 text-center">
                        {(item.type === 'SELECT' || item.type === 'MULTI_SELECT') && item.options && item.options.length > 0 ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold bg-blue-500/10 text-blue-700 border border-blue-500/20">
                            <Tag className="w-3 h-3" />
                            <span>{item.options.length} option{item.options.length > 1 ? 's' : ''}</span>
                          </span>
                        ) : (
                          <span className="text-brand-muted text-[11px]">—</span>
                        )}
                      </td>

                      {/* Ordre d'affichage */}
                      <td className="py-3 px-4 text-center font-mono font-semibold text-brand-text">
                        {item.displayOrder !== undefined ? item.displayOrder : 0}
                      </td>

                      {/* Statut */}
                      <td className="py-3 px-4 text-center">
                        {item.active !== false ? (
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
                          {/* Voir (Consultation) */}
                          <Button
                            variant="ghost"
                            size="sm"
                            icon={<Eye className="w-3.5 h-3.5 text-brand-text" />}
                            onClick={() => openDetailModal(item)}
                            title="Voir les détails"
                            className="p-1.5 hover:bg-brand-alt"
                          />

                          {viewMode === 'ACTIVE' ? (
                            <>
                              {/* Modifier */}
                              <Button
                                variant="outline"
                                size="sm"
                                icon={<Pencil className="w-3.5 h-3.5 text-brand-primary" />}
                                onClick={() => openEditModal(item)}
                                title="Modifier"
                              >
                                Modifier
                              </Button>

                              {/* Toggle Activer / Désactiver */}
                              <Button
                                variant="ghost"
                                size="sm"
                                icon={togglingId === item.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Power className={`w-3.5 h-3.5 ${item.active ? 'text-amber-600' : 'text-emerald-600'}`} />}
                                onClick={() => handleToggleActive(item)}
                                disabled={togglingId === item.id}
                                title={item.active ? 'Désactiver' : 'Activer'}
                                className={item.active ? 'hover:bg-amber-500/10 hover:text-amber-700' : 'hover:bg-emerald-500/10 hover:text-emerald-700'}
                              />

                              {/* Supprimer (Soft Delete) */}
                              <Button
                                variant="ghost"
                                size="sm"
                                icon={<Trash2 className="w-3.5 h-3.5 text-red-500" />}
                                onClick={() => openDeleteConfirm(item)}
                                title="Supprimer"
                                className="hover:bg-red-500/10 hover:text-red-600"
                              />
                            </>
                          ) : (
                            /* Restaurer */
                            <Button
                              variant="outline"
                              size="sm"
                              icon={restoringId === item.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RotateCcw className="w-3.5 h-3.5 text-amber-600" />}
                              onClick={() => handleRestore(item)}
                              disabled={restoringId === item.id}
                              className="hover:bg-amber-500/10 hover:text-amber-600"
                            >
                              Restaurer
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          {totalPages > 1 && (
            <div className="p-3 border-t border-brand-border flex items-center justify-between bg-brand-alt/20">
              <span className="text-xs text-brand-muted font-medium">
                Page {page + 1} sur {totalPages} ({totalElements} éléments)
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  icon={<ChevronLeft className="w-3.5 h-3.5" />}
                >
                  Précédent
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  icon={<ChevronRight className="w-3.5 h-3.5" />}
                >
                  Suivant
                </Button>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODAL : Création / Modification                                         */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      <Modal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        title={editingDefinition ? 'Modifier le type de métadonnée' : 'Nouveau type de métadonnée'}
        maxWidth="lg"
      >
        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          {/* Top Red Bar */}
          <div className="h-1 bg-[#C8102E] -mt-4 -mx-6 mb-4" />

          {/* Portée de la Métadonnée (Globale vs Catégorie spécifique) */}
          <div className="p-3.5 bg-brand-alt/40 border border-brand-border rounded-xl space-y-3">
            <label className="block font-bold text-brand-text">
              Portée de la métadonnée <span className="text-red-500">*</span>
            </label>
            <div className="flex items-center gap-6">
              <label className="inline-flex items-center gap-2 cursor-pointer text-xs font-semibold">
                <input
                  type="radio"
                  name="scope"
                  value="GLOBAL"
                  checked={scope === 'GLOBAL'}
                  onChange={() => setScope('GLOBAL')}
                  className="w-4 h-4 text-[#C8102E] border-brand-border focus:ring-[#C8102E]"
                />
                <span className="flex items-center gap-1">
                  <Globe className="w-3.5 h-3.5 text-slate-600" />
                  <span>Globale (S'applique à tous les documents)</span>
                </span>
              </label>
              <label className="inline-flex items-center gap-2 cursor-pointer text-xs font-semibold">
                <input
                  type="radio"
                  name="scope"
                  value="CATEGORY"
                  checked={scope === 'CATEGORY'}
                  onChange={() => setScope('CATEGORY')}
                  className="w-4 h-4 text-[#C8102E] border-brand-border focus:ring-[#C8102E]"
                />
                <span className="flex items-center gap-1">
                  <FolderKanban className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Catégorie spécifique</span>
                </span>
              </label>
            </div>

            {scope === 'CATEGORY' && (
              <div className="pt-2">
                <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
                  Choisir une catégorie <span className="text-red-500">*</span>
                </label>
                <select
                  value={selectedCategoryId}
                  onChange={(e) => setSelectedCategoryId(e.target.value)}
                  required
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text font-semibold"
                >
                  <option value="">-- Sélectionner une catégorie --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Nom technique & Libellé */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-brand-text mb-1">
                Nom technique (Clé API) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="ex: statut, numero_facture"
                disabled={!!editingDefinition}
                required
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text font-mono disabled:opacity-60 disabled:bg-brand-alt"
              />
              <p className="text-[10px] text-brand-muted mt-1">
                {editingDefinition ? 'Le nom technique ne peut pas être modifié.' : 'Identifiant unique (lettres, chiffres, tirets bas).'}
              </p>
            </div>

            <div>
              <label className="block font-bold text-brand-text mb-1">
                Libellé d'affichage <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                placeholder="ex: Statut du document, Numéro de facture"
                required
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text"
              />
              <p className="text-[10px] text-brand-muted mt-1">Texte affiché dans l'interface utilisateur.</p>
            </div>
          </div>

          {/* Type & Description */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-brand-text mb-1">
                Type de métadonnée <span className="text-red-500">*</span>
              </label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as MetadataType)}
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text font-semibold"
              >
                {METADATA_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <p className="text-[10px] text-brand-muted mt-1">
                {METADATA_TYPES.find((t) => t.value === type)?.description}
              </p>
            </div>

            <div>
              <label className="block font-bold text-brand-text mb-1">
                Description / Aide fonctionnelle
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
                placeholder="ex: Indique l'état d'avancement de la validation du document..."
                className="w-full px-3 py-1.5 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] focus:ring-1 focus:ring-[#C8102E] text-brand-text resize-none"
              />
            </div>
          </div>

          {/* Valeur par défaut & Ordre d'affichage */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block font-bold text-brand-text mb-1">
                Valeur par défaut
              </label>
              {type === 'SELECT' && options.length > 0 ? (
                <select
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                >
                  <option value="">-- Aucune valeur par défaut --</option>
                  {options.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              ) : type === 'BOOLEAN' ? (
                <select
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                >
                  <option value="">-- Non définie --</option>
                  <option value="true">Vrai (Coché / Oui)</option>
                  <option value="false">Faux (Décoché / Non)</option>
                </select>
              ) : type === 'DATE' ? (
                <input
                  type="date"
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                />
              ) : type === 'DATETIME' ? (
                <input
                  type="datetime-local"
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                />
              ) : type === 'NUMBER' || type === 'INTEGER' || type === 'DECIMAL' ? (
                <input
                  type="number"
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  placeholder="ex: 0"
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                />
              ) : (
                <input
                  type="text"
                  value={defaultValue}
                  onChange={(e) => setDefaultValue(e.target.value)}
                  placeholder="Valeur initiale pré-remplie"
                  className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                />
              )}
            </div>

            <div>
              <label className="block font-bold text-brand-text mb-1">
                Ordre d'affichage (Tri)
              </label>
              <input
                type="number"
                value={displayOrder}
                onChange={(e) => setDisplayOrder(parseInt(e.target.value, 10) || 0)}
                placeholder="0"
                className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text font-mono"
              />
              <p className="text-[10px] text-brand-muted mt-1">Ordre d'apparition dans les formulaires d'upload (ex: 0, 1, 2...).</p>
            </div>
          </div>

          {/* Options (Visible uniquement si SELECT ou MULTI_SELECT) */}
          {(type === 'SELECT' || type === 'MULTI_SELECT') && (
            <div className="p-4 bg-brand-alt/40 border border-brand-border rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <label className="font-bold text-brand-text flex items-center gap-1.5">
                  <Tag className="w-4 h-4 text-[#C8102E]" />
                  <span>Options de sélection ({options.length})</span>
                  <span className="text-red-500">*</span>
                </label>
                <span className="text-[10px] text-brand-muted">Définissez la liste des choix proposés à l'utilisateur</span>
              </div>

              {/* Add Option Input Bar */}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={optionInput}
                  onChange={(e) => setOptionInput(e.target.value)}
                  onKeyDown={handleOptionKeyDown}
                  placeholder="Saisissez une option (ex: En cours, Validé, Rejeté)..."
                  className="flex-1 px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text"
                />
                <Button
                  type="button"
                  variant="secondary"
                  size="md"
                  icon={<Plus className="w-3.5 h-3.5" />}
                  onClick={handleAddOption}
                  className="bg-neutral-800 text-white hover:bg-neutral-900 border-none"
                >
                  Ajouter
                </Button>
              </div>

              {/* Options Dynamic List */}
              <div className="min-h-[50px] max-h-48 overflow-y-auto p-2.5 bg-brand-surface border border-brand-border rounded-lg space-y-1.5">
                {options.length === 0 ? (
                  <div className="text-center py-3 text-brand-muted italic text-[11px]">
                    Aucune option définie. Saisissez une valeur ci-dessus et cliquez sur Ajouter.
                  </div>
                ) : (
                  options.map((opt, idx) => (
                    <div
                      key={idx}
                      className="flex items-center gap-2 p-1.5 bg-brand-alt border border-brand-border rounded-lg group"
                    >
                      <span className="w-5 text-center font-mono text-[10px] text-brand-muted">{idx + 1}.</span>
                      <input
                        type="text"
                        value={opt}
                        onChange={(e) => handleUpdateOption(idx, e.target.value)}
                        className="flex-1 bg-transparent border-none text-xs text-brand-text font-medium focus:outline-none focus:ring-0"
                      />
                      <button
                        type="button"
                        onClick={() => handleRemoveOption(idx)}
                        className="p-1 hover:bg-red-500/10 text-brand-muted hover:text-red-600 rounded transition-colors"
                        title="Supprimer cette option"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Pattern Regex Optionnel */}
          <div>
            <label className="block font-bold text-brand-text mb-1">
              Expression régulière de validation (Regex optionnelle)
            </label>
            <input
              type="text"
              value={validationPattern}
              onChange={(e) => setValidationPattern(e.target.value)}
              placeholder="ex: ^FAC-\d{4}-\d{3}$"
              className="w-full px-3 py-2 text-xs bg-brand-surface border border-brand-border rounded-lg focus:outline-none focus:border-[#C8102E] text-brand-text font-mono"
            />
          </div>

          {/* Switches / Checkboxes */}
          <div className="p-3 bg-brand-alt/30 border border-brand-border rounded-xl grid grid-cols-2 md:grid-cols-4 gap-3">
            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={required}
                onChange={(e) => setRequired(e.target.checked)}
                className="w-4 h-4 text-[#C8102E] border-brand-border rounded focus:ring-[#C8102E]"
              />
              <span className="font-bold text-brand-text">Obligatoire</span>
            </label>

            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={searchable}
                onChange={(e) => setSearchable(e.target.checked)}
                className="w-4 h-4 text-[#C8102E] border-brand-border rounded focus:ring-[#C8102E]"
              />
              <span className="font-semibold text-brand-text">Recherchable</span>
            </label>

            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={filterable}
                onChange={(e) => setFilterable(e.target.checked)}
                className="w-4 h-4 text-[#C8102E] border-brand-border rounded focus:ring-[#C8102E]"
              />
              <span className="font-semibold text-brand-text">Filtrable</span>
            </label>

            <label className="inline-flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="w-4 h-4 text-emerald-600 border-brand-border rounded focus:ring-emerald-500"
              />
              <span className="font-bold text-emerald-700">Actif</span>
            </label>
          </div>

          {/* Actions */}
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
              {editingDefinition ? 'Enregistrer les modifications' : 'Créer la métadonnée'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* ───────────────────────────────────────────────────────────────────────── */}
      {/* MODAL : Consultation / Détails (Voir)                                   */}
      {/* ───────────────────────────────────────────────────────────────────────── */}
      <Modal
        isOpen={isDetailModalOpen}
        onClose={() => setIsDetailModalOpen(false)}
        title="Fiche technique de métadonnée"
        maxWidth="md"
      >
        {selectedDefinition && (
          <div className="space-y-4 text-xs">
            {/* Top Red Bar */}
            <div className="h-1 bg-[#C8102E] -mt-4 -mx-6 mb-4" />

            {/* Header info */}
            <div className="p-3.5 bg-brand-alt border border-brand-border rounded-xl flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm font-bold text-[#C8102E]">{selectedDefinition.name}</span>
                  {getTypeBadge(selectedDefinition.type)}
                </div>
                <h3 className="text-base font-bold text-brand-text mt-0.5">{selectedDefinition.label}</h3>
              </div>
              <div>
                {selectedDefinition.active !== false ? (
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
            <div className="grid grid-cols-2 gap-3 p-3 bg-brand-surface border border-brand-border rounded-xl">
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Portée</span>
                <p className="font-semibold text-brand-text mt-0.5 flex items-center gap-1">
                  {selectedDefinition.categoryId ? (
                    <>
                      <FolderKanban className="w-3.5 h-3.5 text-indigo-600" />
                      <span>📁 {categoryMap.get(selectedDefinition.categoryId)?.name || selectedDefinition.categoryId}</span>
                    </>
                  ) : (
                    <>
                      <Globe className="w-3.5 h-3.5 text-slate-500" />
                      <span>🌐 Globale (Tous les documents)</span>
                    </>
                  )}
                </p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Obligatoire</span>
                <p className="font-semibold text-brand-text mt-0.5">{selectedDefinition.required ? 'Oui' : 'Non'}</p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Ordre d'affichage</span>
                <p className="font-mono font-semibold text-brand-text mt-0.5">{selectedDefinition.displayOrder !== undefined ? selectedDefinition.displayOrder : 0}</p>
              </div>
              <div>
                <span className="text-[10px] font-bold uppercase text-brand-muted">Valeur par défaut</span>
                <p className="font-semibold text-brand-text mt-0.5">{selectedDefinition.defaultValue || '—'}</p>
              </div>
              {selectedDefinition.deletedAt && (
                <div className="col-span-2 p-2 bg-amber-50 border border-amber-200 rounded-lg">
                  <span className="text-[10px] font-bold uppercase text-amber-800">Date de suppression (Corbeille)</span>
                  <p className="font-semibold text-amber-900 mt-0.5">
                    {new Date(selectedDefinition.deletedAt).toLocaleDateString('fr-FR', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                </div>
              )}
            </div>

            {/* Description */}
            {selectedDefinition.description && (
              <div className="p-3 bg-brand-alt/40 border border-brand-border rounded-xl">
                <span className="text-[10px] font-bold uppercase text-brand-muted">Description fonctionnelle</span>
                <p className="text-brand-text mt-1 leading-relaxed">{selectedDefinition.description}</p>
              </div>
            )}

            {/* Options list for SELECT & MULTI_SELECT */}
            {(selectedDefinition.type === 'SELECT' || selectedDefinition.type === 'MULTI_SELECT') && (
              <div className="p-3.5 bg-brand-surface border border-brand-border rounded-xl space-y-2">
                <span className="text-[10px] font-bold uppercase text-brand-muted flex items-center gap-1.5">
                  <Tag className="w-3.5 h-3.5 text-[#C8102E]" />
                  <span>Options de sélection ({selectedDefinition.options?.length || 0})</span>
                </span>
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {selectedDefinition.options && selectedDefinition.options.length > 0 ? (
                    selectedDefinition.options.map((opt) => (
                      <span key={opt} className="px-2.5 py-1 text-xs bg-blue-500/10 text-blue-700 border border-blue-500/20 rounded-full font-medium">
                        {opt}
                      </span>
                    ))
                  ) : (
                    <span className="text-brand-muted italic text-[11px]">Aucune option configurée.</span>
                  )}
                </div>
              </div>
            )}

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
        title="Supprimer le type de métadonnée"
        message={`Êtes-vous sûr de vouloir supprimer la définition "${deletingDefinition?.label}" (${deletingDefinition?.name}) ? Elle sera déplacée dans la corbeille.`}
        confirmText="Supprimer"
        cancelText="Annuler"
        variant="danger"
        isLoading={isDeleting}
      />
    </div>
  );
};

export default MetadataDefinitionsPage;
