import { apiClient } from './client';
import {
  DocumentItem,
  DocumentSearchResult,
  DocumentVersion,
  DocumentLock,
  PageResponse,
  SearchFilterParams,
  DocumentStatus,
  UpdateDocumentPayload,
} from '../types';

export const documentApi = {
  // Search
  search: async (params: SearchFilterParams): Promise<PageResponse<DocumentSearchResult>> => {
    const res = await apiClient.get<PageResponse<DocumentSearchResult>>('/documents/search', { params });
    return res.data;
  },

  // Get by ID
  getById: async (id: string): Promise<DocumentItem> => {
    const res = await apiClient.get<DocumentItem>(`/documents/${id}`);
    return res.data;
  },

  // Upload single
  upload: async (formData: FormData): Promise<DocumentItem> => {
    const res = await apiClient.post<DocumentItem>('/documents/upload', formData);
    return res.data;
  },

  // Bulk Upload
  bulkUpload: async (formData: FormData) => {
    const res = await apiClient.post('/documents/upload/bulk', formData);
    return res.data;
  },

  // Download URL
  downloadUrl: (id: string, versionId?: string) => {
    return versionId
      ? `http://localhost:8080/api/v1/documents/${id}/versions/${versionId}/download`
      : `http://localhost:8080/api/v1/documents/${id}/download`;
  },

  // Preview URL
  previewUrl: (id: string) => {
    return `http://localhost:8080/api/v1/documents/${id}/preview`;
  },

  // Authenticated Blob fetch for preview (includes Keycloak Bearer token via apiClient)
  fetchPreviewBlob: async (id: string): Promise<Blob> => {
    const res = await apiClient.get(`/documents/${id}/preview`, {
      responseType: 'blob',
    });
    return res.data;
  },

  // Authenticated Blob fetch for download (includes Keycloak Bearer token via apiClient)
  fetchDownloadBlob: async (id: string, versionId?: string): Promise<Blob> => {
    const url = versionId
      ? `/documents/${id}/versions/${versionId}/download`
      : `/documents/${id}/download`;
    const res = await apiClient.get(url, {
      responseType: 'blob',
    });
    return res.data;
  },

  // Secure download using apiClient + Blob URL revocation
  downloadFile: async (id: string, fileName: string, versionId?: string): Promise<void> => {
    const blob = await documentApi.fetchDownloadBlob(id, versionId);
    const objectUrl = URL.createObjectURL(blob);
    const a = window.document.createElement('a');
    a.href = objectUrl;
    a.download = fileName;
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(objectUrl);
    }, 1000);
  },

  // Upload new version
  uploadVersion: async (id: string, formData: FormData): Promise<DocumentVersion> => {
    const res = await apiClient.post<DocumentVersion>(`/documents/${id}/versions`, formData);
    return res.data;
  },

  // List versions
  listVersions: async (id: string): Promise<DocumentVersion[]> => {
    const res = await apiClient.get<DocumentVersion[]>(`/documents/${id}/versions`);
    return res.data;
  },

  // Checkout (Lock)
  checkout: async (id: string): Promise<DocumentLock> => {
    const res = await apiClient.post<DocumentLock>(`/documents/${id}/checkout`);
    return res.data;
  },

  // Checkin (Unlock)
  checkin: async (id: string): Promise<void> => {
    await apiClient.post(`/documents/${id}/checkin`);
  },

  // Get Lock status
  getLockStatus: async (id: string): Promise<DocumentLock> => {
    const res = await apiClient.get<DocumentLock>(`/documents/${id}/lock`);
    return res.data;
  },

  // Update Document Properties / Location
  update: async (
    id: string,
    data: UpdateDocumentPayload
  ): Promise<DocumentItem> => {
    const res = await apiClient.patch<DocumentItem>(`/documents/${id}`, data);
    return res.data;
  },

  // Change Status
  updateStatus: async (id: string, status: DocumentStatus): Promise<DocumentItem> => {
    const res = await apiClient.patch<DocumentItem>(`/documents/${id}/status`, null, {
      params: { status },
    });
    return res.data;
  },

  // Soft Delete
  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/documents/${id}`);
  },

  // Bulk Delete
  bulkDelete: async (documentIds: string[]): Promise<void> => {
    await apiClient.delete('/documents/bulk', { data: documentIds });
  },

  // Bulk Move
  bulkMove: async (documentIds: string[], targetFolderId?: string, moveToRoot = false): Promise<void> => {
    await apiClient.patch('/documents/bulk/move', documentIds, {
      params: { targetFolderId, moveToRoot },
    });
  },

  // Bulk Tag
  bulkTag: async (documentIds: string[], tagNames: string[]): Promise<void> => {
    await apiClient.post('/documents/bulk/tag', documentIds, {
      params: { tagNames: tagNames.join(',') },
    });
  },
};
