package com.awb.ged.domain.folder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Folder</h1>
 * <p>
 * Core domain aggregate representing a directory folder in the hierarchical filing system.
 * Folders hold references to other folders (via parentId) or documents.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Folder {

    /** Unique identifier for the folder */
    private UUID id;

    /** Name of the folder */
    private String name;

    /** Parent folder identifier (null if located at root level) */
    private UUID parentId;

    /** Owner/Creator of the folder */
    private UUID ownerId;

    /** Timestamp of folder creation in UTC */
    private Instant createdAt;

    /** Timestamp of soft-deletion, null if not deleted */
    private Instant deletedAt;

    /** ID of the user who deleted this folder, null if not deleted */
    private UUID deletedBy;

    /** Timestamp of last folder modification in UTC */
    private Instant updatedAt;

    /**
     * Checks if the folder has been soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
