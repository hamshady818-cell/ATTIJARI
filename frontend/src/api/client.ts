import axios from 'axios';
import { authService } from '../services/authService';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = authService.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type'];
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.warn('401 Non Autorisé - Réinitialisation de la session GED');
      authService.clearTokens();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    const errorMsg = error.response?.data?.message || error.message || 'Une erreur est survenue';
    console.error('API Error:', errorMsg);
    return Promise.reject(error);
  }
);
