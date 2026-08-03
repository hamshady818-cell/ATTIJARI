import { useAuth as useOidcAuth } from 'react-oidc-context';

export interface UserProfile {
  id: string; // keycloak sub
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
}

export const useAuth = () => {
  const auth = useOidcAuth();

  // Extract roles from Keycloak JWT
  const getRoles = (): string[] => {
    if (!auth.user) return [];
    const profile = auth.user.profile as any;
    const roles: string[] = [];

    // 1. Extract Realm roles
    if (profile.realm_access && Array.isArray(profile.realm_access.roles)) {
      profile.realm_access.roles.forEach((r: string) => roles.push(r.toUpperCase()));
    }

    // 2. Extract Client roles (from the resource_access client claims)
    if (profile.resource_access) {
      Object.keys(profile.resource_access).forEach((clientId) => {
        const clientAccess = profile.resource_access[clientId];
        if (clientAccess && Array.isArray(clientAccess.roles)) {
          clientAccess.roles.forEach((r: string) => roles.push(r.toUpperCase()));
        }
      });
    }

    // Remove duplicates
    return Array.from(new Set(roles));
  };

  const roles = getRoles();

  const hasRole = (role: string): boolean => {
    return roles.includes(role.toUpperCase());
  };

  const hasAnyRole = (allowedRoles: string[]): boolean => {
    return allowedRoles.some((role) => hasRole(role));
  };

  const user: UserProfile | null = auth.user
    ? {
        id: auth.user.profile.sub || '',
        username: (auth.user.profile as any).preferred_username || '',
        email: auth.user.profile.email || '',
        firstName: (auth.user.profile as any).given_name || 'User',
        lastName: (auth.user.profile as any).family_name || 'GED',
      }
    : null;

  return {
    isAuthenticated: auth.isAuthenticated,
    isLoading: auth.isLoading,
    user,
    roles,
    token: auth.user?.access_token || null,
    hasRole,
    hasAnyRole,
    login: () => auth.signinRedirect(),
    logout: () => {
      // Direct post-logout redirect URL setup
      auth.signoutRedirect({
        post_logout_redirect_uri: window.location.origin,
      });
    },
    auth, // raw auth context if needed
  };
};
