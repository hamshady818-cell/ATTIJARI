export type DocumentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'TRASHED';

export interface UserItem {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

export interface DocumentItem {
  id: string;
  name: string;
  description?: string;
  status: DocumentStatus;
  mimeType?: string;
  folderId?: string;
  categoryId?: string;
  ownerId: string;
  activeVersionId?: string;
  isLocked: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface DocumentSearchResult {
  id: string;
  name: string;
  description?: string;
  status: DocumentStatus;
  mimeType?: string;
  folderId?: string;
  folderName?: string;
  categoryId?: string;
  categoryName?: string;
  ownerId: string;
  ownerUsername?: string;
  activeVersionId?: string;
  isLocked: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface FolderItem {
  id: string;
  name: string;
  parentId?: string;
  ownerId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface FolderContent {
  currentFolder?: FolderItem;
  subFolders: FolderItem[];
  documents: DocumentItem[];
}

export interface CategoryItem {
  id: string;
  name: string;
  description?: string;
}

export interface TagItem {
  id: string;
  name: string;
  color?: string;
}

export interface DepartmentItem {
  id: string;
  name: string;
}

export interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  versionLabel?: string;
  hash: string;
  sizeBytes: number;
  mimeType?: string;
  fileReferenceId?: string;
  changeSummary?: string;
  majorVersion?: boolean;
  uploadedBy?: string;
  uploadedByUsername?: string;
  uploadedAt: string;
}

export interface DocumentLock {
  documentId: string;
  locked: boolean;
  lockedBy?: string;
  lockedByUsername?: string;
  lockedAt?: string;
  lockExpiration?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  sortBy?: string;
  sortDirection?: string;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface DashboardStats {
  totalDocuments: number;
  totalFolders: number;
  storageUsedBytes: number;
  recentUploads: {
    id: string;
    name: string;
    status: DocumentStatus;
    mimeType?: string;
    ownerId?: string;
    ownerUsername?: string;
    createdAt: string;
    updatedAt: string;
  }[];
  recentlyModified: {
    id: string;
    name: string;
    status: DocumentStatus;
    mimeType?: string;
    ownerId?: string;
    ownerUsername?: string;
    createdAt: string;
    updatedAt: string;
  }[];
  topCategories: {
    categoryId: string;
    categoryName: string;
    documentCount: number;
  }[];
}

export interface SearchFilterParams {
  keyword?: string;
  categoryId?: string;
  tagName?: string;
  folderId?: string;
  ownerId?: string;
  status?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}
