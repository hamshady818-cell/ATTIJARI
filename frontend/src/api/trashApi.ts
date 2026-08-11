import { apiClient } from './client';
import { PageResponse, TrashItemResponse } from '../types';

export const trashApi = {
  getTrash: async (page = 0, size = 10): Promise<PageResponse<TrashItemResponse>> => {
    try {
      const res = await apiClient.get<PageResponse<TrashItemResponse>>('/trash', {
        params: { page, size },
      });
      return res.data;
    } catch {
      const res = await apiClient.get<PageResponse<TrashItemResponse>>('/documents/trash', {
        params: { page, size },
      });
      return res.data;
    }
  },

  restore: async (id: string): Promise<void> => {
    try {
      await apiClient.post(`/trash/${id}/restore`);
    } catch {
      await apiClient.post(`/documents/${id}/restore`);
    }
  },
};
