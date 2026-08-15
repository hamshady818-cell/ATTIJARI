export function extractErrorMessage(err: any, fallback: string): string {
  const backendMessage = err?.response?.data?.message;
  if (backendMessage) return backendMessage;
  if (err?.message === 'Network Error') return 'Impossible de contacter le serveur. Vérifiez votre connexion.';
  return fallback;
}
