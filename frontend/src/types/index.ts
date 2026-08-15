export type DocumentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'TRASHED';

export interface DocumentMetadataValue {
  definitionId?: string;
  key: string;
  value: string;
}

export interface UserItem {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  departmentId?: string;
}

export interface DocumentItem {
  id: string;
  name: string;
  description?: string;
  status: DocumentStatus;
  mimeType?: string;
  folderId?: string;
  categoryId?: string;
  categoryName?: string;
  departmentId?: string;
  departmentName?: string;
  ownerId: string;
  ownerUsername?: string;
  ownerName?: string;
  expirationDate?: string;
  activeVersionId?: string;
  isLocked: boolean;
  tags: string[];
  metadata?: DocumentMetadataValue[];
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
  departmentId?: string;
  departmentName?: string;
  ownerId: string;
  ownerUsername?: string;
  ownerName?: string;
  expirationDate?: string;
  activeVersionId?: string;
  isLocked: boolean;
  tags: string[];
  metadata?: DocumentMetadataValue[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateDocumentPayload {
  newName?: string;
  name?: string;
  description?: string;
  categoryId?: string;
  departmentId?: string;
  ownerId?: string;
  expirationDate?: string;
  tags?: string[];
  metadata?: DocumentMetadataValue[];
  newFolderId?: string;
  moveToRoot?: boolean;
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

export type NotificationType =
  | 'DOCUMENT_UPLOADED'
  | 'DOCUMENT_UPDATED'
  | 'DOCUMENT_DELETED'
  | 'DOCUMENT_SHARED'
  | 'DOCUMENT_EXPIRED'
  | 'CHECKOUT_REQUESTED'
  | 'CHECKIN_DONE'
  | 'COMMENT_ADDED'
  | 'SYSTEM';

export interface NotificationItem {
  id: string;
  type: NotificationType;
  /** Short title shown as notification header (maps to backend's `title` field). */
  title: string;
  /** Full notification body text (maps to backend's `body` field). */
  body?: string;
  /** Type of the related entity: "DOCUMENT" | "FOLDER" | null */
  entityType?: string;
  /** UUID of the related entity for deep-linking */
  entityId?: string;
  /** Delivery channel: "IN_APP" | "EMAIL" | "PUSH" */
  channel?: string;
  /** Backend lifecycle status: "PENDING" | "SENT" | "READ" | "FAILED" */
  status: 'PENDING' | 'SENT' | 'READ' | 'FAILED';
  /** Timestamp when the user acknowledged this notification (null if unread) */
  readAt?: string;
  /** Timestamp when the notification was dispatched */
  sentAt?: string;
  createdAt: string;
  /** Derived client-side field — true when status === "READ" */
  read: boolean;
}

export interface TrashItemResponse {
  id: string;
  entityType: 'DOCUMENT' | 'FOLDER' | string;
  entityId: string;
  name?: string;
  originalFolderId?: string;
  deletedBy?: string;
  ownerUsername?: string;
  deletedAt: string;
  autoPurgeAt?: string;
}
