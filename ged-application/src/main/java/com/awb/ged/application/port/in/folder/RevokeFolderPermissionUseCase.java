package com.awb.ged.application.port.in.folder;

import java.util.UUID;

public interface RevokeFolderPermissionUseCase {
    void revokePermission(UUID folderId, UUID permissionId, UUID userId, boolean isAdminOrManager);
}
