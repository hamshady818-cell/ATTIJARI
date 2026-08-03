package com.awb.ged.infrastructure.persistence.entity.trash;

import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>TrashJpaEntity</h1>
 * <p>
 * JPA entity representing a soft-deleted item in the GED-AWB recycle bin.
 * When a user deletes a document or folder, it is moved here instead of being
 * permanently removed. Items are automatically purged after a configurable
 * retention period (default: 30 days).
 * </p>
 *
 * <p>
 * A scheduled background job processes rows where {@code autoPurgeAt <= NOW()}
 * and {@code purgedAt IS NULL}, permanently deleting the referenced entities.
 * </p>
 *
 * <p><strong>Design Decision — Polymorphic entity reference:</strong>
 * Same pattern as {@code FavoriteJpaEntity}: {@code entityType} + {@code entityId}
 * discriminate between a trashed document and a trashed folder without
 * requiring separate trash tables.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link UserJpaEntity} via {@code deletedBy}.</li>
 *   <li>M:1 (nullable) → {@link FolderJpaEntity} via {@code originalFolder}
 *       — used to restore the item to its original location.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "trash",
        indexes = {
                @Index(name = "idx_trash_entity",     columnList = "entity_type, entity_id"),
                @Index(name = "idx_trash_purge_date", columnList = "auto_purge_at")
        }
)
public class TrashJpaEntity extends BaseEntity {

    /**
     * Discriminator values for the type of entity in the trash.
     */
    public enum EntityType {
        DOCUMENT,
        FOLDER
    }

    /**
     * Type of the deleted entity.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, updatable = false, length = 20)
    private EntityType entityType;

    /**
     * UUID of the deleted entity (document ID or folder ID).
     * Not a DB-level FK — the referenced row still exists until purge.
     */
    @NotNull
    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    /**
     * The original parent folder the entity was deleted from.
     * Stored to enable "Restore to original location" functionality.
     * May be {@code null} if the original folder no longer exists.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "original_folder_id",
            foreignKey = @ForeignKey(name = "fk_trash_original_folder")
    )
    private FolderJpaEntity originalFolder;

    /**
     * User who performed the deletion operation.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deleted_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_trash_deleted_by")
    )
    private UserJpaEntity deletedBy;

    /**
     * Timestamp when the entity was moved to trash.
     */
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;

    /**
     * Scheduled timestamp for automatic permanent deletion.
     * Set by the application (e.g., {@code deletedAt + 30 days}).
     */
    @Column(name = "auto_purge_at", nullable = false)
    private Instant autoPurgeAt;

    /**
     * Timestamp when the entity was permanently purged from the system.
     * {@code null} means the item is still in the trash (recoverable).
     */
    @Column(name = "purged_at")
    private Instant purgedAt;

    /**
     * Returns {@code true} if the item is still in the trash and can be restored.
     */
    public boolean isRestorable() {
        return this.purgedAt == null;
    }
}
