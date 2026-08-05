import { apiClient } from './client';
import { FolderContent, FolderItem } from '../types';

export const folderApi = {
  getRootContent: async (): Promise<FolderContent> => {
    const res = await apiClient.get<FolderContent>('/folders/content');
    return res.data;
  },

  getFolderContent: async (folderId: string): Promise<FolderContent> => {
    const res = await apiClient.get<FolderContent>(`/folders/${folderId}/content`);
    return res.data;
  },

  createFolder: async (name: string, parentFolderId?: string): Promise<FolderItem> => {
    const res = await apiClient.post<FolderItem>('/folders', { name, parentFolderId });
    return res.data;
  },

  deleteFolder: async (folderId: string): Promise<void> => {
    await apiClient.delete(`/folders/${folderId}`);
  },
};
