package com.awb.ged.infrastructure.security.jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h1>KeycloakJwtRoleConverter</h1>
 * <p>
 * Custom JWT converter that extracts realm and client roles from a Keycloak token
 * and converts them into Spring Security {@link GrantedAuthority} objects with the {@code ROLE_} prefix.
 * </p>
 */
public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = new ArrayList<>();

        // 1. Extract Realm roles
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess != null && realmAccess.containsKey(ROLES)) {
            Object rolesObj = realmAccess.get(ROLES);
            if (rolesObj instanceof Collection<?>) {
                for (Object role : (Collection<?>) rolesObj) {
                    roles.add(role.toString());
                }
            }
        }

        // 2. Extract Client roles (from all resource access mappings)
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess != null) {
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                Object clientAccessObj = entry.getValue();
                if (clientAccessObj instanceof Map<?, ?>) {
                    Map<?, ?> clientAccess = (Map<?, ?>) clientAccessObj;
                    if (clientAccess.containsKey(ROLES)) {
                        Object clientRolesObj = clientAccess.get(ROLES);
                        if (clientRolesObj instanceof Collection<?>) {
                            for (Object role : (Collection<?>) clientRolesObj) {
                                roles.add(role.toString());
                            }
                        }
                    }
                }
            }
        }

        // Convert roles to SimpleGrantedAuthority with "ROLE_" prefix
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toSet());
    }
}
