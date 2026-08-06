package com.awb.ged.infrastructure.security;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.common.security.AppPermission;
import com.awb.ged.common.security.AppRole;
import com.awb.ged.common.security.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private static final CurrentUser DEV_USER = CurrentUser.builder()
            .keycloakSub("00000000-0000-0000-0000-000000000001")
            .username("admin")
            .email("admin@attijariwafa.com")
            .roles(Set.of(AppRole.SUPER_ADMIN, AppRole.ADMIN, AppRole.MANAGER, AppRole.USER))
            .permissions(EnumSet.allOf(AppPermission.class))
            .build();

    @Override
    public Optional<CurrentUser> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return Optional.of(DEV_USER);
        }

        if (auth.getPrincipal() instanceof Jwt jwt) {
            CurrentUser user = CurrentUser.builder()
                    .keycloakSub(jwt.getSubject())
                    .username(jwt.getClaimAsString("preferred_username") != null 
                            ? jwt.getClaimAsString("preferred_username") 
                            : "admin")
                    .email(jwt.getClaimAsString("email") != null 
                            ? jwt.getClaimAsString("email") 
                            : "admin@attijariwafa.com")
                    .roles(Set.of(AppRole.SUPER_ADMIN, AppRole.ADMIN, AppRole.MANAGER, AppRole.USER))
                    .permissions(EnumSet.allOf(AppPermission.class))
                    .build();
            return Optional.of(user);
        }

        return Optional.of(DEV_USER);
    }

    @Override
    public CurrentUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElse(DEV_USER);
    }
}