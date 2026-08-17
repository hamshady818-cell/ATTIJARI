import { apiClient } from './client';
import { CategoryItem } from '../types';

export interface CreateCategoryPayload {
  name: string;
  description?: string;
  parentId?: string;
  color?: string;
  icon?: string;
  securityClass?: string;
  active?: boolean;
}

export interface UpdateCategoryPayload {
  name?: string;
  description?: string;
  parentId?: string;
  color?: string;
  icon?: string;
  securityClass?: string;
  active?: boolean;
}

export const categoriesApi = {
  list: async (parentId?: string): Promise<CategoryItem[]> => {
    const res = await apiClient.get<CategoryItem[]>('/categories', {
      params: { parentId: parentId || undefined },
    });
    return res.data;
  },

  listDeleted: async (): Promise<CategoryItem[]> => {
    const res = await apiClient.get<CategoryItem[]>('/categories/deleted');
    return res.data;
  },

  getById: async (id: string): Promise<CategoryItem> => {
    const res = await apiClient.get<CategoryItem>(`/categories/${id}`);
    return res.data;
  },

  create: async (payload: CreateCategoryPayload): Promise<CategoryItem> => {
    const res = await apiClient.post<CategoryItem>('/categories', payload);
    return res.data;
  },

  update: async (id: string, payload: UpdateCategoryPayload): Promise<CategoryItem> => {
    const res = await apiClient.patch<CategoryItem>(`/categories/${id}`, payload);
    return res.data;
  },

  toggleActive: async (id: string, active: boolean): Promise<CategoryItem> => {
    const res = await apiClient.patch<CategoryItem>(`/categories/${id}/active`, { active });
    return res.data;
  },

  remove: async (id: string): Promise<void> => {
    await apiClient.delete(`/categories/${id}`);
  },

  restore: async (id: string): Promise<CategoryItem> => {
    const res = await apiClient.post<CategoryItem>(`/categories/${id}/restore`);
    return res.data;
  },
};
