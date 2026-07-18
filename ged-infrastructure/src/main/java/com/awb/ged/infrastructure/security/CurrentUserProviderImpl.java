package com.awb.ged.infrastructure.security;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.UnauthorizedException;
import com.awb.ged.common.security.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    @Override
    public Optional<CurrentUser> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        CurrentUser user = CurrentUser.builder()
                .keycloakSub(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .build();

        return Optional.of(user);
    }

    @Override
    public CurrentUser getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED));
    }
}