package com.awb.ged.application.port.in.security;

import com.awb.ged.common.security.CurrentUser;

import java.util.Optional;

/**
 * <h1>CurrentUserProvider</h1>
 * <p>
 * Input Port interface defining how the current user context is retrieved.
 * Keeps the business logic fully independent of Spring Security or request-scoped threads.
 * </p>
 */
public interface CurrentUserProvider {

    /**
     * Resolves the currently authenticated user context.
     *
     * @return an {@link Optional} containing the {@link CurrentUser}, or empty if unauthenticated
     */
    Optional<CurrentUser> getCurrentUser();

    /**
     * Resolves the current user, throwing an exception if the context is unauthenticated.
     *
     * @return the {@link CurrentUser}
     * @throws com.awb.ged.common.exception.UnauthorizedException if no user is authenticated
     */
    CurrentUser getRequiredCurrentUser();
}
