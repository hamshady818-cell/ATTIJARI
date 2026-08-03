package com.awb.ged.application.port.in.document;

import java.util.UUID;

public interface RevokeDocumentPermissionUseCase {
    void revokePermission(UUID documentId, UUID permissionId, UUID userId, boolean isAdminOrManager);
}
