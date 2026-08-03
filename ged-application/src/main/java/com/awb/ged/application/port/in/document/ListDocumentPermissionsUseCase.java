package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.permission.PermissionResponseDto;

import java.util.List;
import java.util.UUID;

public interface ListDocumentPermissionsUseCase {
    List<PermissionResponseDto> listPermissions(UUID documentId, UUID userId, boolean isAdminOrManager);
}
