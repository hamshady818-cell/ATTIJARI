import React, { useState, useEffect, useRef, useCallback } from 'react';
import { FolderItem } from '../../types';
import {
  Folder,
  FolderOpen,
  ChevronRight,
  ChevronDown,
  Plus,
  HardDrive,
  Files,
  FileClock,
  Trash2,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-react';

interface FolderTreeSidebarProps {
  folders: FolderItem[];
  selectedFolderId?: string;
  onSelectFolder: (folderId?: string) => void;
  onCreateFolderClick: () => void;
  activeFilterType?: 'all' | 'folder' | 'drafts';
  onSelectFilterType: (type: 'all' | 'folder' | 'drafts') => void;
  onMoveDocument?: (documentIds: string[], targetFolderId?: string, moveToRoot?: boolean) => void;
  onDeleteFolder?: (folder: FolderItem) => void;
}

const MIN_WIDTH = 200;
const MAX_WIDTH = 480;
const DEFAULT_WIDTH = 256;

export const FolderTreeSidebar: React.FC<FolderTreeSidebarProps> = ({
  folders,
  selectedFolderId,
  onSelectFolder,
  onCreateFolderClick,
  activeFilterType = 'folder',
  onSelectFilterType,
  onMoveDocument,
  onDeleteFolder,
}) => {
  const [expandedFolders, setExpandedFolders] = useState<Record<string, boolean>>({});
  const [dragOverTarget, setDragOverTarget] = useState<string | null>(null);

  // Width & Collapse state with localStorage persistence
  const [sidebarWidth, setSidebarWidth] = useState<number>(() => {
    const saved = localStorage.getItem('ged-sidebar-width');
    if (saved) {
      const parsed = parseInt(saved, 10);
      if (!isNaN(parsed) && parsed >= MIN_WIDTH && parsed <= MAX_WIDTH) {
        return parsed;
      }
    }
    return DEFAULT_WIDTH;
  });

  const [isCollapsed, setIsCollapsed] = useState<boolean>(() => {
    return localStorage.getItem('ged-sidebar-collapsed') === 'true';
  });

  const [isResizing, setIsResizing] = useState<boolean>(false);
  const sidebarRef = useRef<HTMLDivElement>(null);

  // Persist sidebar width
  useEffect(() => {
    if (!isResizing) {
      localStorage.setItem('ged-sidebar-width', sidebarWidth.toString());
    }
  }, [sidebarWidth, isResizing]);

  // Persist collapsed state
  useEffect(() => {
    localStorage.setItem('ged-sidebar-collapsed', isCollapsed ? 'true' : 'false');
  }, [isCollapsed]);

  // Resize drag listener
  const startResizing = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isResizing) return;
      if (sidebarRef.current) {
        const rect = sidebarRef.current.getBoundingClientRect();
        const newWidth = e.clientX - rect.left;
        const clampedWidth = Math.min(Math.max(newWidth, MIN_WIDTH), MAX_WIDTH);
        setSidebarWidth(clampedWidth);
      }
    };

    const handleMouseUp = () => {
      if (isResizing) {
        setIsResizing(false);
      }
    };

    if (isResizing) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }

    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isResizing]);

  const toggleExpand = (folderId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedFolders((prev) => ({ ...prev, [folderId]: !prev[folderId] }));
  };

  // Build tree hierarchy
  const rootFolders = folders.filter((f) => !f.parentId);
  const getSubfolders = (parentId: string) => folders.filter((f) => f.parentId === parentId);

  const handleDrop = (e: React.DragEvent, targetFolderId?: string, moveToRoot = false) => {
    e.preventDefault();
    setDragOverTarget(null);
    try {
      const dataStr = e.dataTransfer.getData('text/plain');
      if (!dataStr) return;
      const data = JSON.parse(dataStr);
      if (data && Array.isArray(data.documentIds) && data.documentIds.length > 0) {
        if (onMoveDocument) {
          onMoveDocument(data.documentIds, targetFolderId, moveToRoot);
        }
      }
    } catch { /* ignore invalid data */ }
  };

  const renderFolderNode = (folder: FolderItem, depth = 0) => {
    const isExpanded = expandedFolders[folder.id];
    const isSelected = activeFilterType === 'folder' && selectedFolderId === folder.id;
    const isDragOver = dragOverTarget === folder.id;
    const subfolders = getSubfolders(folder.id);
    const hasChildren = subfolders.length > 0;

    return (
      <div key={folder.id} className="select-none group">
        <div
          onClick={() => {
            onSelectFilterType('folder');
            onSelectFolder(folder.id);
          }}
          onDragOver={(e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            setDragOverTarget(folder.id);
          }}
          onDragLeave={() => setDragOverTarget(null)}
          onDrop={(e) => handleDrop(e, folder.id)}
          style={{ paddingLeft: `${depth * 14 + 10}px` }}
          className={`flex items-center justify-between py-1.5 pr-2.5 mx-1.5 my-0.5 text-xs font-medium cursor-pointer rounded-md transition-all duration-150 ${
            isDragOver
              ? 'bg-brand-primary-light border border-brand-primary text-brand-primary font-bold scale-[1.01]'
              : isSelected
                ? 'bg-brand-primary-light text-brand-primary font-bold border-l-3 border-brand-primary'
                : 'text-brand-text hover:bg-brand-alt'
          }`}
        >
          <div className="flex items-center gap-1.5 truncate min-w-0">
            {hasChildren ? (
              <button
                onClick={(e) => toggleExpand(folder.id, e)}
                className="p-0.5 text-brand-muted hover:text-brand-text rounded-xs shrink-0"
              >
                {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
              </button>
            ) : (
              <span className="w-4 shrink-0" />
            )}

            {isSelected || isDragOver ? (
              <FolderOpen className="w-4 h-4 text-brand-primary shrink-0" />
            ) : (
              <Folder className="w-4 h-4 text-brand-muted shrink-0" />
            )}

            <span className="truncate">{folder.name}</span>
          </div>

          {onDeleteFolder && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onDeleteFolder(folder);
              }}
              className="opacity-0 group-hover:opacity-100 p-1 text-brand-muted hover:text-brand-primary hover:bg-brand-primary-light rounded-md transition-all shrink-0"
              title={`Supprimer le dossier "${folder.name}"`}
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>

        {hasChildren && isExpanded && (
          <div>{subfolders.map((child) => renderFolderNode(child, depth + 1))}</div>
        )}
      </div>
    );
  };

  return (
    <>
      {/* Floating expand button when collapsed */}
      {isCollapsed && (
        <button
          onClick={() => setIsCollapsed(false)}
          className="fixed left-3 top-20 z-30 p-2 bg-brand-surface border border-brand-border hover:border-brand-primary text-brand-primary rounded-r-lg shadow-md transition-all group hover:scale-105"
          title="Afficher l'espace de classement"
        >
          <PanelLeftOpen className="w-4 h-4 text-brand-primary group-hover:scale-110 transition-transform" />
        </button>
      )}

      {/* Main Sidebar Container */}
      <aside
        ref={sidebarRef}
        style={{ width: isCollapsed ? 0 : `${sidebarWidth}px` }}
        className={`relative bg-brand-surface border-r border-brand-border flex flex-col h-full shrink-0 select-none ${
          isCollapsed ? 'overflow-hidden border-r-0' : ''
        } ${isResizing ? 'transition-none' : 'transition-[width] duration-200 ease-in-out'}`}
      >
        {/* Resize Handle on Right Border */}
        {!isCollapsed && (
          <div
            onMouseDown={startResizing}
            className={`absolute top-0 right-0 w-1.5 h-full cursor-col-resize z-20 group transition-colors hover:bg-brand-primary/40 active:bg-brand-primary ${
              isResizing ? 'bg-brand-primary' : 'bg-transparent'
            }`}
            title="Glisser pour redimensionner"
          >
            <div className="w-0.5 h-8 bg-brand-muted/30 group-hover:bg-brand-primary absolute top-1/2 -translate-y-1/2 left-0.5 rounded-full" />
          </div>
        )}

        {/* Inner Content (Kept min-w to prevent text warping during transition) */}
        <div style={{ minWidth: `${sidebarWidth}px` }} className="flex flex-col h-full">
          {/* Sidebar Header */}
          <div className="p-3.5 bg-brand-alt/50 border-b border-brand-border flex items-center justify-between min-w-0">
            <span className="text-[11px] font-bold uppercase tracking-wider text-brand-muted truncate pr-2">
              Espace de classement
            </span>
            <div className="flex items-center gap-1 shrink-0">
              <button
                onClick={onCreateFolderClick}
                className="p-1.5 bg-brand-surface border border-brand-border hover:border-brand-primary hover:text-brand-primary text-brand-text rounded-md shadow-xs transition-all"
                title="Créer un nouveau dossier"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => setIsCollapsed(true)}
                className="p-1.5 bg-brand-surface border border-brand-border hover:border-brand-primary hover:text-brand-primary text-brand-muted hover:text-brand-text rounded-md shadow-xs transition-all"
                title="Masquer le panneau latéral"
              >
                <PanelLeftClose className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {/* Main Shortcuts */}
          <div className="p-2 border-b border-brand-border flex flex-col gap-0.5">
            <button
              onClick={() => {
                onSelectFilterType('all');
                onSelectFolder(undefined);
              }}
              className={`flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-left rounded-md transition-all duration-150 ${
                activeFilterType === 'all'
                  ? 'bg-brand-primary-light text-brand-primary border-l-3 border-brand-primary'
                  : 'text-brand-text hover:bg-brand-alt'
              }`}
            >
              <Files className="w-4 h-4 text-brand-muted shrink-0" />
              <span className="truncate">Tous les documents</span>
            </button>

            <button
              onClick={() => {
                onSelectFilterType('folder');
                onSelectFolder(undefined);
              }}
              onDragOver={(e) => {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                setDragOverTarget('root');
              }}
              onDragLeave={() => setDragOverTarget(null)}
              onDrop={(e) => handleDrop(e, undefined, true)}
              className={`flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-left rounded-md transition-all duration-150 ${
                dragOverTarget === 'root'
                  ? 'bg-brand-primary-light border border-brand-primary text-brand-primary font-bold scale-[1.01]'
                  : activeFilterType === 'folder' && !selectedFolderId
                    ? 'bg-brand-primary-light text-brand-primary border-l-3 border-brand-primary'
                    : 'text-brand-text hover:bg-brand-alt'
              }`}
            >
              <HardDrive className="w-4 h-4 text-brand-muted shrink-0" />
              <span className="truncate">Racine (sans dossier)</span>
            </button>

            <button
              onClick={() => {
                onSelectFilterType('drafts');
                onSelectFolder(undefined);
              }}
              className={`flex items-center gap-2.5 px-3 py-2 text-xs font-semibold text-left rounded-md transition-all duration-150 ${
                activeFilterType === 'drafts'
                  ? 'bg-brand-primary-light text-brand-primary border-l-3 border-brand-primary'
                  : 'text-brand-text hover:bg-brand-alt'
              }`}
            >
              <FileClock className="w-4 h-4 text-brand-muted shrink-0" />
              <span className="truncate">Mes Brouillons</span>
            </button>
          </div>

          {/* Folder Tree List */}
          <div className="flex-1 overflow-y-auto py-2">
            <div className="px-3.5 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-brand-muted truncate">
              Dossiers ({folders.length})
            </div>
            {rootFolders.length === 0 ? (
              <div className="px-3.5 py-4 text-center text-xs text-brand-muted italic">
                Aucun dossier créé
              </div>
            ) : (
              rootFolders.map((folder) => renderFolderNode(folder, 0))
            )}
          </div>

          {/* Storage Information footer */}
          <div className="p-3.5 bg-brand-alt/50 border-t border-brand-border text-[11px] text-brand-muted">
            <div className="flex justify-between font-mono mb-1.5">
              <span className="truncate pr-1">MinIO GED Bucket</span>
              <span className="font-bold text-brand-text shrink-0">ged-documents</span>
            </div>
            <div className="w-full bg-brand-border h-1.5 rounded-full overflow-hidden">
              <div className="bg-brand-primary h-full w-[28%] rounded-full" />
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};

