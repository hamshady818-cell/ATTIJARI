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

public class KeycloakJwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String GROUPS = "groups";

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String GROUP_PREFIX = "GROUP_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Extract Realm roles
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);

        if (realmAccess != null && realmAccess.containsKey(ROLES)) {
            Object rolesObj = realmAccess.get(ROLES);

            if (rolesObj instanceof Collection<?>) {
                for (Object role : (Collection<?>) rolesObj) {
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    ROLE_PREFIX + role.toString().toUpperCase()
                            )
                    );
                }
            }
        }

        // 2. Extract Client roles
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
                                authorities.add(
                                        new SimpleGrantedAuthority(
                                                ROLE_PREFIX + role.toString().toUpperCase()
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }

        // 3. Extract Keycloak Groups
        List<String> groups = jwt.getClaim(GROUPS);

        if (groups != null) {

            for (String group : groups) {

                // "/FINANCE" -> "FINANCE"
                String groupName = group
                        .replace("/", "")
                        .toUpperCase();

                authorities.add(
                        new SimpleGrantedAuthority(
                                GROUP_PREFIX + groupName
                        )
                );
            }
        }

        // Remove duplicates
        if (authorities.isEmpty()) {
            return Collections.emptyList();
        }

        return authorities.stream()
                .collect(Collectors.toSet());
    }
}