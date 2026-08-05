import React, { useState } from 'react';
import { FolderItem } from '../../types';
import { Folder, FolderOpen, ChevronRight, ChevronDown, Plus, HardDrive, Files, FileClock } from 'lucide-react';

interface FolderTreeSidebarProps {
  folders: FolderItem[];
  selectedFolderId?: string;
  onSelectFolder: (folderId?: string) => void;
  onCreateFolderClick: () => void;
  activeFilterType?: 'all' | 'folder' | 'drafts';
  onSelectFilterType: (type: 'all' | 'folder' | 'drafts') => void;
}

export const FolderTreeSidebar: React.FC<FolderTreeSidebarProps> = ({
  folders,
  selectedFolderId,
  onSelectFolder,
  onCreateFolderClick,
  activeFilterType = 'folder',
  onSelectFilterType,
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({});

  const toggleExpand = (folderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedFolders((prev) => ({ ...prev, [folderId]: !prev[folderId] }));
  };

  // Build tree hierarchy
  const rootFolders = folders.filter((f) => !f.parentId);
  const getSubfolders = (parentId: string) => folders.filter((f) => f.parentId === parentId);

  const renderFolderNode = (folder: FolderItem, depth = 0) => {
    const isExpanded = expandedFolders[folder.id];
    const isSelected = activeFilterType === 'folder' && selectedFolderId === folder.id;
    const subfolders = getSubfolders(folder.id);
    const hasChildren = subfolders.length > 0;

    return (
      <div key={folder.id} className="select-none">
        <div
          onClick={() => {
            onSelectFilterType('folder');
            onSelectFolder(folder.id);
          }}
          style={{ paddingLeft: `${depth * 14 + 12}px` }}
          className={`flex items-center justify-between py-1.5 pr-2 text-xs font-medium cursor-pointer border-l-2 transition-colors ${
            isSelected
              ? 'bg-brand-alt border-brand-primary text-brand-primary font-bold'
              : 'border-transparent text-brand-text hover:bg-brand-alt/60'
          }`}
        >
          <div className="flex items-center gap-1.5 truncate">
            {hasChildren ? (
              <button
                onClick={(e) => toggleExpand(folder.id, e)}
                className="p-0.5 text-brand-muted hover:text-brand-text"
              >
                {isExpanded ? <ChevronDown className="w-3 h-3" /> : <ChevronRight className="w-3 h-3" />}
              </button>
            ) : (
              <span className="w-4" />
            )}

            {isSelected ? (
              <FolderOpen className="w-3.5 h-3.5 text-brand-primary shrink-0" />
            ) : (
              <Folder className="w-3.5 h-3.5 text-brand-muted shrink-0" />
            )}

            <span className="truncate">{folder.name}</span>
          </div>
        </div>

        {hasChildren && isExpanded && (
          <div>{subfolders.map((child) => renderFolderNode(child, depth + 1))}</div>
        )}
      </div>
    );
  };

  return (
    <aside className="w-64 bg-brand-surface border-r border-brand-border flex flex-col h-full shrink-0 select-none">
      {/* Sidebar Header */}
      <div className="p-3 bg-brand-alt border-b border-brand-border flex items-center justify-between">
        <span className="text-[11px] font-bold uppercase tracking-wider text-brand-muted">
          Espace de classement
        </span>
        <button
          onClick={onCreateFolderClick}
          className="p-1 bg-white border border-brand-border hover:border-brand-primary hover:text-brand-primary text-brand-text rounded-sm transition-colors"
          title="Créer un nouveau dossier"
        >
          <Plus className="w-3.5 h-3.5" />
        </button>
      </div>

      {/* Main Shortcuts */}
      <div className="p-2 border-b border-brand-border flex flex-col gap-0.5">
        <button
          onClick={() => {
            onSelectFilterType('all');
            onSelectFolder(undefined);
          }}
          className={`flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-left border-l-2 transition-colors ${
            activeFilterType === 'all'
              ? 'bg-brand-alt border-brand-primary text-brand-primary font-bold'
              : 'border-transparent text-brand-text hover:bg-brand-alt/60'
          }`}
        >
          <Files className="w-3.5 h-3.5 text-brand-muted" />
          <span>Tous les documents</span>
        </button>

        <button
          onClick={() => {
            onSelectFilterType('folder');
            onSelectFolder(undefined);
          }}
          className={`flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-left border-l-2 transition-colors ${
            activeFilterType === 'folder' && !selectedFolderId
              ? 'bg-brand-alt border-brand-primary text-brand-primary font-bold'
              : 'border-transparent text-brand-text hover:bg-brand-alt/60'
          }`}
        >
          <HardDrive className="w-3.5 h-3.5 text-brand-muted" />
          <span>Racine (sans dossier)</span>
        </button>

        <button
          onClick={() => {
            onSelectFilterType('drafts');
            onSelectFolder(undefined);
          }}
          className={`flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-left border-l-2 transition-colors ${
            activeFilterType === 'drafts'
              ? 'bg-brand-alt border-brand-primary text-brand-primary font-bold'
              : 'border-transparent text-brand-text hover:bg-brand-alt/60'
          }`}
        >
          <FileClock className="w-3.5 h-3.5 text-brand-muted" />
          <span>Mes Brouillons</span>
        </button>
      </div>

      {/* Folder Tree List */}
      <div className="flex-1 overflow-y-auto py-2">
        <div className="px-3 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-brand-muted">
          Dossiers ({folders.length})
        </div>
        {rootFolders.length === 0 ? (
          <div className="px-3 py-4 text-center text-xs text-brand-muted italic">
            Aucun dossier créé
          </div>
        ) : (
          rootFolders.map((folder) => renderFolderNode(folder, 0))
        )}
      </div>

      {/* Storage Information footer */}
      <div className="p-3 bg-brand-alt border-t border-brand-border text-[11px] text-brand-muted">
        <div className="flex justify-between font-mono mb-1">
          <span>MinIO GED Bucket</span>
          <span className="font-bold text-brand-text">ged-documents</span>
        </div>
        <div className="w-full bg-brand-border h-1.5 rounded-none overflow-hidden">
          <div className="bg-brand-primary h-full w-[28%]" />
        </div>
      </div>
    </aside>
  );
};
