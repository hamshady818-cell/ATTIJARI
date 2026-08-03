import { api } from '../../api/client';
import type {
  FolderContentResponseDto,
  FolderResponseDto,
  DocumentResponseDto,
  FavoriteResponseDto,
  CategoryResponseDto,
  TagResponseDto,
} from '../../types';

export const explorerApi = {
  // Folders API
  getFolderContent: async (folderId?: string): Promise<FolderContentResponseDto> => {
    const url = folderId ? `/folders/${folderId}/content` : '/folders/content';
    const res = await api.get<FolderContentResponseDto>(url);
    return res.data;
  },

  createFolder: async (name: string, parentFolderId?: string): Promise<FolderResponseDto> => {
    const res = await api.post<FolderResponseDto>('/folders', {
      name,
      parentFolderId: parentFolderId || null,
    });
    return res.data;
  },

  deleteFolder: async (folderId: string): Promise<void> => {
    await api.delete(`/folders/${folderId}`);
  },

  // Documents API
  uploadDocument: async (
    file: File,
    name: string,
    folderId?: string,
    categoryId?: string
  ): Promise<DocumentResponseDto> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', name);
    if (folderId) formData.append('folderId', folderId);
    if (categoryId) formData.append('categoryId', categoryId);

    const res = await api.post<DocumentResponseDto>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  deleteDocument: async (documentId: string): Promise<void> => {
    await api.delete(`/documents/${documentId}`);
  },

  // Favorites API
  getFavorites: async (): Promise<FavoriteResponseDto[]> => {
    const res = await api.get<FavoriteResponseDto[]>('/favorites');
    return res.data;
  },

  addFavorite: async (
    entityType: 'DOCUMENT' | 'FOLDER',
    entityId: string
  ): Promise<FavoriteResponseDto> => {
    const res = await api.post<FavoriteResponseDto>('/favorites', {
      entityType,
      entityId,
    });
    return res.data;
  },

  removeFavorite: async (favoriteId: string): Promise<void> => {
    await api.delete(`/favorites/${favoriteId}`);
  },

  // Categories API
  getCategories: async (parentId?: string): Promise<CategoryResponseDto[]> => {
    const res = await api.get<CategoryResponseDto[]>('/categories', {
      params: parentId ? { parentId } : {},
    });
    return res.data;
  },

  // Tags API
  getTags: async (): Promise<TagResponseDto[]> => {
    const res = await api.get<TagResponseDto[]>('/tags');
    return res.data;
  },
};
