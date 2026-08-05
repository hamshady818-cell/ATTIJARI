import { apiClient } from './client';
import { CategoryItem, TagItem, DashboardStats } from '../types';

export const refApi = {
  getCategories: async (): Promise<CategoryItem[]> => {
    try {
      const res = await apiClient.get<CategoryItem[]>('/categories');
      return res.data;
    } catch {
      return [];
    }
  },

  getTags: async (): Promise<TagItem[]> => {
    try {
      const res = await apiClient.get<TagItem[]>('/tags');
      return res.data;
    } catch {
      return [];
    }
  },

  getDashboardStats: async (): Promise<DashboardStats> => {
    const res = await apiClient.get<DashboardStats>('/dashboard/stats');
    return res.data;
  },
};
