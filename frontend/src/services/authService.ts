import { AuthTokens, UserSession } from '../types/auth';

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8081';
const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'ged-awb';
const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'ged-frontend';

const TOKEN_ENDPOINT = `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`;

const STORAGE_TOKENS_KEY = 'ged_awb_tokens';

class AuthService {
    private currentTokens: AuthTokens | null = null;

    constructor() {
        this.currentTokens = this.loadStoredTokens();
    }

    /**
     * Authentification transparente en arrière-plan via l'endpoint Token Keycloak (Direct Access Grants)
     */
    public async login(username: string, password: string): Promise<AuthTokens> {
        const body = new URLSearchParams();
        body.set('grant_type', 'password');
        body.set('client_id', KEYCLOAK_CLIENT_ID);
        body.set('username', username);
        body.set('password', password);

        const response = await fetch(TOKEN_ENDPOINT, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body,
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            const message = errorData.error_description || 'Identifiant ou mot de passe incorrect.';
            throw new Error(message);
        }

        const data = await response.json();
        const tokens: AuthTokens = {
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            idToken: data.id_token,
            expiresIn: data.expires_in,
            refreshExpiresIn: data.refresh_expires_in,
            tokenType: data.token_type || 'Bearer',
        };

        this.saveTokens(tokens);
        return tokens;
    }

    /**
     * Rafraîchissement silencieux du token via le Refresh Token
     */
    public async refresh(refreshToken: string): Promise<AuthTokens> {
        const body = new URLSearchParams();
        body.set('grant_type', 'refresh_token');
        body.set('client_id', KEYCLOAK_CLIENT_ID);
        body.set('refresh_token', refreshToken);

        const response = await fetch(TOKEN_ENDPOINT, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body,
        });

        if (!response.ok) {
            this.clearTokens();
            throw new Error('Session expirée ou invalide. Veuillez réauthentifier.');
        }

        const data = await response.json();
        const tokens: AuthTokens = {
            accessToken: data.access_token,
            refreshToken: data.refresh_token || refreshToken,
            idToken: data.id_token,
            expiresIn: data.expires_in,
            refreshExpiresIn: data.refresh_expires_in,
            tokenType: data.token_type || 'Bearer',
        };

        this.saveTokens(tokens);
        return tokens;
    }

    /**
     * Décode les claims d'un jeton JWT
     */
    public parseJwt(token: string): UserSession | null {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(
                atob(base64)
                    .split('')
                    .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                    .join('')
            );
            const parsed = JSON.parse(jsonPayload);

            return {
                sub: parsed.sub || '',
                username: parsed.preferred_username || parsed.username || 'Agent GED',
                name: parsed.name || parsed.preferred_username || 'Agent GED',
                email: parsed.email || '',
                roles: parsed.realm_access?.roles || [],
            };
        } catch (e) {
            console.error('Erreur lors du décodage du token JWT Keycloak:', e);
            return null;
        }
    }

    public getAccessToken(): string | undefined {
        return this.currentTokens?.accessToken;
    }

    public getTokens(): AuthTokens | null {
        return this.currentTokens;
    }

    public saveTokens(tokens: AuthTokens): void {
        this.currentTokens = tokens;
        try {
            sessionStorage.setItem(STORAGE_TOKENS_KEY, JSON.stringify(tokens));
        } catch (e) {
            console.error('Impossible d\'enregistrer les jetons dans le sessionStorage', e);
        }
    }

    public loadStoredTokens(): AuthTokens | null {
        try {
            const raw = sessionStorage.getItem(STORAGE_TOKENS_KEY);
            if (!raw) return null;
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    public clearTokens(): void {
        this.currentTokens = null;
        sessionStorage.removeItem(STORAGE_TOKENS_KEY);
        localStorage.removeItem('ged_kc_token');
        localStorage.removeItem('ged_kc_refresh_token');
        localStorage.removeItem('ged_kc_id_token');
    }
}

export const authService = new AuthService();
