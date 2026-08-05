package com.awb.ged.domain.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <h1>Document</h1>
 * <p>
 * Core domain Aggregate Root representing a document in the GED-AWB system.
 * Manages document metadata, active version references, locking mechanisms, tags, and timestamps.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * Document lifecycle states.
     * DRAFT   = Only owner can see.
     * PUBLISHED = Visible to authorized users.
     * ARCHIVED = Read-only.
     * TRASHED = Hidden except for restore.
     */
    public enum DocumentStatus {
        DRAFT, PUBLISHED, ARCHIVED, TRASHED
    }

    /** Unique identifier for the document */
    private UUID id;

    /** Name of the document */
    private String name;

    /** Optional description */
    private String description;

    /** Lifecycle status */
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    /** MIME type of the current version */
    private String mimeType;

    /** Associated folder identifier (null if located at root directory) */
    private UUID folderId;

    /** Associated document category identifier (optional) */
    private UUID categoryId;

    /** Owner/Creator of the document */
    private UUID ownerId;

    /** Identifier of the currently active version of the document */
    private UUID activeVersionId;

    /** Check-out lock details (null if not currently locked/checked out) */
    private DocumentLock lock;

    /** List of dynamic metadata values assigned to the document */
    @Builder.Default
    private List<DocumentMetadataValue> metadata = new ArrayList<>();

    /** List of tags associated with the document */
    @Builder.Default
    private List<DocumentTag> tags = new ArrayList<>();

    /** Timestamp of document creation in UTC */
    private Instant createdAt;

    /** Timestamp of soft-deletion, null if not deleted */
    private Instant deletedAt;

    /** ID of the user who deleted this document, null if not deleted */
    private UUID deletedBy;

    /** Timestamp of last document modification in UTC */
    private Instant updatedAt;

    /**
     * Checks if the document is currently locked.
     *
     * @param now the current timestamp in UTC (to check lock expiration)
     * @return true if the document is locked and the lock has not expired
     */
    public boolean isCurrentlyLocked(Instant now) {
        return lock != null && !lock.isExpired(now);
    }

    /**
     * Checks if the document has been soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
