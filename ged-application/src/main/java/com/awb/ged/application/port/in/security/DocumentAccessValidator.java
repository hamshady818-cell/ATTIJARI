package com.awb.ged.application.port.in.security;

import com.awb.ged.domain.document.model.Document;

import java.util.UUID;

/**
 * <h1>DocumentAccessValidator</h1>
 * <p>
 * Input Port interface defining dynamic document access validation based on Keycloak groups,
 * department/entity boundaries, document ownership, and explicit ACL grants.
 * </p>
 */
public interface DocumentAccessValidator {

    /**
     * Validates whether the currently authenticated user is authorized to perform an action on a target document.
     *
     * @param document       the target document
     * @param userId         the internal UUID of the current user (nullable)
     * @param requiredAction the action being performed ("READ", "WRITE", "DELETE", etc.)
     * @throws com.awb.ged.common.exception.ForbiddenException if access is denied
     */
    void validateAccess(Document document, UUID userId, String requiredAction);
}
