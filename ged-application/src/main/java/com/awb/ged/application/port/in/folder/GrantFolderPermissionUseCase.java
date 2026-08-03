package com.awb.ged.application.port.in.folder;

import com.awb.ged.application.dto.permission.GrantPermissionCommand;
import com.awb.ged.application.dto.permission.PermissionResponseDto;

public interface GrantFolderPermissionUseCase {
    PermissionResponseDto grantPermission(GrantPermissionCommand command, boolean isAdminOrManager);
}
