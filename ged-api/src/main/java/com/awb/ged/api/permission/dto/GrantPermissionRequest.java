package com.awb.ged.api.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantPermissionRequest {

    private UUID userId;
    private UUID groupId;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canShareOrManage;
}
