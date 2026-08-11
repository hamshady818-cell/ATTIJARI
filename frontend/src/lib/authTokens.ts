import keycloak from './keycloak';

const ACCESS_TOKEN_KEY = 'ged_kc_token';
const REFRESH_TOKEN_KEY = 'ged_kc_refresh_token';
const ID_TOKEN_KEY = 'ged_kc_id_token';

export interface StoredTokens {
    token: string;
    refreshToken?: string;
    idToken?: string;
}

/**
 * Nettoie les anciens jetons enregistrés dans localStorage par l'ancien flux invalide
 */
export function clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(ID_TOKEN_KEY);
}

/**
 * Récupère le jeton JWT actif directement depuis l'instance Keycloak en mémoire
 */
export function getToken(): string | undefined {
    return keycloak.token;
}

/**
 * Compatibilité legacy : ne stocke plus de jetons bruts obsolètes dans localStorage
 */
export function saveTokens(_tokens: StoredTokens): void {
    clearTokens();
}

/**
 * Compatibilité legacy : retourne null pour forcer keycloak.init() à démarrer de manière propre
 */
export function loadTokens(): StoredTokens | null {
    clearTokens();
    return null;
}