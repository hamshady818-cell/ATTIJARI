package com.awb.ged.domain.folder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>FolderPermission</h1>
 * <p>
 * Domain model representing a folder permission (ACL entry).
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FolderPermission {

    private UUID id;
    private UUID folderId;
    private UUID userId;
    private UUID groupId;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canManage;
    private boolean inherited;
    private UUID grantedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
