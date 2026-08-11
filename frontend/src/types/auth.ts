export interface AuthTokens {
    accessToken: string;
    refreshToken: string;
    idToken?: string;
    expiresIn: number;
    refreshExpiresIn?: number;
    tokenType: string;
}

export interface UserSession {
    username: string;
    name?: string;
    email?: string;
    roles: string[];
    sub: string;
}

export interface AuthContextType {
    user: UserSession | null;
    tokens: AuthTokens | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (username: string, password: string) => Promise<void>;
    logout: () => void;
}
