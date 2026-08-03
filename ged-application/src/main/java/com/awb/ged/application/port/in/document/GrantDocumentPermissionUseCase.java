package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;

public interface GrantDocumentPermissionUseCase {
    PermissionResponseDto grantPermission(GrantPermissionCommand command, boolean isAdminOrManager);
}
