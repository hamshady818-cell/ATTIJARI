import { apiClient } from './client';
import { CategoryItem, TagItem, DepartmentItem, UserItem, DashboardStats } from '../types';

export const refApi = {
  getCategories: async (): Promise<CategoryItem[]> => {
    try {
      const res = await apiClient.get<CategoryItem[]>('/categories');
      return res.data;
    } catch {
      return [];
    }
  },

  getDepartments: async (): Promise<DepartmentItem[]> => {
    try {
      const res = await apiClient.get<DepartmentItem[]>('/departments');
      return res.data;
    } catch {
      return [];
    }
  },

  getUsers: async (): Promise<UserItem[]> => {
    try {
      const res = await apiClient.get<UserItem[]>('/users');
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
