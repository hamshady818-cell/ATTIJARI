package com.awb.ged.application.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionCommand {

    private UUID targetId; // Document ID or Folder ID
    private UUID userId;
    private UUID groupId;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canShareOrManage; // Map to canShare for docs and canManage for folders
    private UUID grantedBy;
}
