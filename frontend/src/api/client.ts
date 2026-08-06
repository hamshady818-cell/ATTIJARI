import axios from 'axios';
import keycloak from '../lib/keycloak';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(async (config) => {
    await keycloak.updateToken(30).catch(() => keycloak.login());
    config.headers.Authorization = `Bearer ${keycloak.token}`;
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    return config;
});

// Interceptor for handling global API error responses
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const errorMsg = error.response?.data?.message || error.message || 'Une erreur est survenue';
    console.error('API Error:', errorMsg);
    return Promise.reject(error);
  }
);
