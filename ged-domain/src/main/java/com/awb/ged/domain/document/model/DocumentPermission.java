package com.awb.ged.domain.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentPermission</h1>
 * <p>
 * Domain model representing a document permission (ACL entry).
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPermission {

    private UUID id;
    private UUID documentId;
    private UUID userId;
    private UUID groupId;
    private boolean canRead;
    private boolean canWrite;
    private boolean canDelete;
    private boolean canShare;
    private UUID grantedBy;
    private Instant createdAt;
    private Instant updatedAt;
}
