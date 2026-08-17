import { apiClient } from './client';
import { PageResponse } from '../types';

export type MetadataType =
  | 'STRING'
  | 'TEXT'
  | 'NUMBER'
  | 'INTEGER'
  | 'DECIMAL'
  | 'DATE'
  | 'DATETIME'
  | 'BOOLEAN'
  | 'SELECT'
  | 'MULTI_SELECT'
  | 'URL';

export interface MetadataDefinition {
  id: string;
  name: string;
  label: string;
  type: MetadataType;
  description?: string;
  required: boolean;
  searchable?: boolean;
  filterable?: boolean;
  defaultValue?: string;
  displayOrder?: number;
  active: boolean;
  options?: string[];
  validationPattern?: string;
  categoryId?: string | null;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string;
  deletedBy?: string;
}

export interface CreateMetadataDefinitionPayload {
  name: string;
  label: string;
  type: MetadataType;
  description?: string;
  required?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  defaultValue?: string;
  displayOrder?: number;
  active?: boolean;
  options?: string[];
  validationPattern?: string;
  categoryId?: string | null;
}

export interface UpdateMetadataDefinitionPayload {
  name?: string;
  label?: string;
  type?: MetadataType;
  description?: string;
  required?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  defaultValue?: string;
  displayOrder?: number;
  active?: boolean;
  options?: string[];
  validationPattern?: string;
  categoryId?: string | null;
}

export const metadataApi = {
  list: async (page = 0, size = 20): Promise<PageResponse<MetadataDefinition>> => {
    const res = await apiClient.get<any>('/metadata-definitions', {
      params: { page, size },
    });
    if (Array.isArray(res.data)) {
      return {
        content: res.data,
        totalElements: res.data.length,
        totalPages: 1,
        pageNumber: page,
        pageSize: size,
        first: true,
        last: true,
        empty: res.data.length === 0,
      };
    }
    return res.data;
  },

  getByCategory: async (categoryId?: string, page = 0, size = 100): Promise<PageResponse<MetadataDefinition>> => {
    const res = await apiClient.get<any>('/metadata-definitions', {
      params: { categoryId: categoryId || undefined, page, size },
    });
    if (Array.isArray(res.data)) {
      return {
        content: res.data,
        totalElements: res.data.length,
        totalPages: 1,
        pageNumber: page,
        pageSize: size,
        first: true,
        last: true,
        empty: res.data.length === 0,
      };
    }
    return res.data;
  },

  listDeleted: async (page = 0, size = 20): Promise<PageResponse<MetadataDefinition>> => {
    const res = await apiClient.get<any>('/metadata-definitions/deleted', {
      params: { page, size },
    });
    if (Array.isArray(res.data)) {
      return {
        content: res.data,
        totalElements: res.data.length,
        totalPages: 1,
        pageNumber: page,
        pageSize: size,
        first: true,
        last: true,
        empty: res.data.length === 0,
      };
    }
    return res.data;
  },

  getById: async (id: string): Promise<MetadataDefinition> => {
    const res = await apiClient.get<MetadataDefinition>(`/metadata-definitions/${id}`);
    return res.data;
  },

  create: async (payload: CreateMetadataDefinitionPayload): Promise<MetadataDefinition> => {
    const res = await apiClient.post<MetadataDefinition>('/metadata-definitions', payload);
    return res.data;
  },

  update: async (id: string, payload: UpdateMetadataDefinitionPayload): Promise<MetadataDefinition> => {
    const res = await apiClient.patch<MetadataDefinition>(`/metadata-definitions/${id}`, payload);
    return res.data;
  },

  toggleActive: async (id: string, active: boolean): Promise<MetadataDefinition> => {
    const res = await apiClient.patch<MetadataDefinition>(`/metadata-definitions/${id}`, { active });
    return res.data;
  },

  remove: async (id: string): Promise<void> => {
    await apiClient.delete(`/metadata-definitions/${id}`);
  },

  restore: async (id: string): Promise<MetadataDefinition> => {
    const res = await apiClient.post<MetadataDefinition>(`/metadata-definitions/${id}/restore`);
    return res.data;
  },
};
