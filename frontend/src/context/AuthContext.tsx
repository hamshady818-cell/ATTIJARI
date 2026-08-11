import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { AuthTokens, UserSession, AuthContextType } from '../types/auth';
import { authService } from '../services/authService';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [tokens, setTokens] = useState<AuthTokens | null>(() => authService.getTokens());
    const [user, setUser] = useState<UserSession | null>(() => {
        const stored = authService.getTokens();
        return stored ? authService.parseJwt(stored.accessToken) : null;
    });
    const [isLoading, setIsLoading] = useState<boolean>(true);

    const initAuth = useCallback(async () => {
        const storedTokens = authService.getTokens();
        if (storedTokens?.refreshToken) {
            try {
                const refreshed = await authService.refresh(storedTokens.refreshToken);
                setTokens(refreshed);
                setUser(authService.parseJwt(refreshed.accessToken));
            } catch (err) {
                console.warn('Impossible de rafraîchir la session au démarrage:', err);
                authService.clearTokens();
                setTokens(null);
                setUser(null);
            }
        }
        setIsLoading(false);
    }, []);

    useEffect(() => {
        initAuth();
    }, [initAuth]);

    // Intervalle de rafraîchissement silencieux du token toutes les 2 minutes
    useEffect(() => {
        if (!tokens?.refreshToken) return;

        const interval = setInterval(async () => {
            try {
                const refreshed = await authService.refresh(tokens.refreshToken);
                setTokens(refreshed);
                setUser(authService.parseJwt(refreshed.accessToken));
            } catch (err) {
                console.error('Échec du rafraîchissement silencieux du token:', err);
                logout();
            }
        }, 120000); // 2 minutes

        return () => clearInterval(interval);
    }, [tokens?.refreshToken]);

    const login = async (username: string, password: string): Promise<void> => {
        setIsLoading(true);
        try {
            const newTokens = await authService.login(username, password);
            setTokens(newTokens);
            const userSession = authService.parseJwt(newTokens.accessToken);
            setUser(userSession);
        } finally {
            setIsLoading(false);
        }
    };

    const logout = (): void => {
        authService.clearTokens();
        setTokens(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                tokens,
                isAuthenticated: !!tokens?.accessToken && !!user,
                isLoading,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth doit être utilisé à l\'intérieur d\'un AuthProvider');
    }
    return context;
};
