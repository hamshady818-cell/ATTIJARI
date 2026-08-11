package com.awb.ged.domain.trash.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>TrashItem</h1>
 * <p>
 * Domain aggregate representing an item (document or folder) in the recycle bin.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TrashItem {

    private UUID id;
    private String entityType; // "DOCUMENT" or "FOLDER"
    private UUID entityId;
    private String name;
    private UUID originalFolderId;
    private UUID deletedBy;
    private String ownerUsername;
    private Instant deletedAt;
    private Instant autoPurgeAt;
    private Instant purgedAt;
}
