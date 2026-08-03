import axios from 'axios';
import type { ApiErrorResponse } from '../types';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to inject the Keycloak access token into every request
api.interceptors.request.use(
  (config) => {
    // Find the Keycloak OIDC storage key in sessionStorage
    const oidcKey = Object.keys(sessionStorage).find((key) =>
      key.startsWith('oidc.user:')
    );
    if (oidcKey) {
      const oidcData = sessionStorage.getItem(oidcKey);
      if (oidcData) {
        try {
          const parsed = JSON.parse(oidcData);
          if (parsed.access_token) {
            config.headers.Authorization = `Bearer ${parsed.access_token}`;
          }
        } catch (err) {
          console.error('Failed to parse OIDC session storage token', err);
        }
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Map backend error codes to friendly French descriptions
export const mapErrorCodeToMessage = (error: ApiErrorResponse): string => {
  switch (error.code) {
    case 'FORBIDDEN':
      return "Accès refusé. Vous n'avez pas les permissions nécessaires pour effectuer cette action.";
    case 'UNAUTHORIZED':
      return "Session expirée. Veuillez vous reconnecter.";
    case 'NOT_FOUND':
      return "Élément introuvable. Il a peut-être été déplacé ou supprimé.";
    case 'CONFLICT':
      return "Un conflit est survenu (ex. cet élément existe déjà).";
    case 'INVALID_INPUT':
      return "Les informations saisies sont incorrectes ou incomplètes.";
    case 'FILE_TOO_LARGE':
      return "Le fichier sélectionné dépasse la taille maximale autorisée.";
    case 'INVALID_DOCUMENT_FORMAT':
      return "Le format de ce document n'est pas supporté.";
    case 'INTERNAL_ERROR':
    default:
      return error.message || "Une erreur technique inattendue s'est produite. Veuillez réessayer.";
  }
};
