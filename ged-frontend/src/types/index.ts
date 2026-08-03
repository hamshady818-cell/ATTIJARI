export interface ApiErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  details?: Record<string, any> | null;
}

export interface FolderResponseDto {
  id: string;
  name: string;
  parentId: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentResponseDto {
  id: string;
  name: string;
  folderId: string | null;
  categoryId: string | null;
  ownerId: string;
  activeVersionId: string;
  isLocked: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  mimeType?: string; // Optional but useful MIME type representation
}

export interface FolderContentResponseDto {
  currentFolder: FolderResponseDto | null;
  subFolders: FolderResponseDto[];
  documents: DocumentResponseDto[];
}

export interface PermissionResponseDto {
  id: string;
  targetId: string;
  userId: string | null;
  groupId: string | null;
  canRead: boolean;
  canWrite: boolean;
  canDelete: boolean;
  canShareOrManage: boolean;
  inherited: boolean;
  grantedBy: string;
  createdAt: string;
}

export interface FavoriteResponseDto {
  id: string;
  userId: string;
  entityType: 'DOCUMENT' | 'FOLDER';
  entityId: string;
  createdAt: string;
  // Included fields to show in favorite explorer:
  name?: string;
}

export interface TrashItemResponseDto {
  id: string;
  entityType: 'DOCUMENT' | 'FOLDER';
  entityId: string;
  originalFolderId: string | null;
  deletedBy: string;
  deletedAt: string;
  autoPurgeAt: string;
}

export interface NotificationResponseDto {
  id: string;
  type: string;
  title: string;
  body: string;
  entityType: string;
  entityId: string;
  channel: string;
  status: string; // 'READ', 'UNREAD' etc
  readAt: string | null;
  sentAt: string;
  createdAt: string;
}

export interface CategoryResponseDto {
  id: string;
  name: string;
  parentId: string | null;
  path: string;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentResponseDto {
  id: string;
  name: string;
  parentId: string | null;
  createdAt: string;
  updatedAt: string;
}

export type MetadataType = 'STRING' | 'INTEGER' | 'DATE' | 'BOOLEAN';

export interface MetadataDefinitionResponseDto {
  id: string;
  name: string;
  label: string;
  type: MetadataType;
  required: boolean;
  validationPattern: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TagResponseDto {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoleResponseDto {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuditLogResponseDto {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  performedBy: string;
  timestamp: string;
  details: string;
}
