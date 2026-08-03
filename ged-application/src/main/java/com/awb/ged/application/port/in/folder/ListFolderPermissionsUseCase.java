package com.awb.ged.application.port.in.folder;

import com.awb.ged.application.dto.permission.PermissionResponseDto;

import java.util.List;
import java.util.UUID;

public interface ListFolderPermissionsUseCase {
    List<PermissionResponseDto> listPermissions(UUID folderId, UUID userId, boolean isAdminOrManager);
}
