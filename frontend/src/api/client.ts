import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
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
