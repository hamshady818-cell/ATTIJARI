package com.awb.ged.application.dto.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDto {

    private UUID id;
    private UUID targetId;
    private UUID userId;
    private UUID groupId;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canShareOrManage;
    private boolean inherited;
    private UUID grantedBy;
    private Instant createdAt;
}
