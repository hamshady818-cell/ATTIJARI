package com.awb.ged.infrastructure.persistence.entity.archive;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * <h1>ArchiveJpaEntity</h1>
 * <p>
 * JPA entity recording that a {@link DocumentJpaEntity} has been moved to long-term
 * cold storage (archive) in GED-AWB.
 * </p>
 *
 * <p>
 * Archiving is a deliberate, compliance-driven action that makes a document
 * <strong>immutable and read-only</strong>. Archived documents remain searchable
 * but cannot be edited, checked out, or deleted until the retention policy expires.
 * This is distinct from the trash — there is no auto-purge for archived documents.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>1:1 ← {@link DocumentJpaEntity} — one archive record per document (UNIQUE FK).</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code archivedBy} — who archived the document.</li>
 * </ul>
 *
 * <p><strong>Design Decision — Storage tiers:</strong>
 * The {@code StorageTier} enum enables the system to track whether the physical file
 * has been moved to cheaper/slower object storage (COLD or GLACIER tiers in S3/MinIO),
 * supporting cost-optimized long-term retention.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "archives",
        indexes = @Index(name = "idx_archives_document_id", columnList = "document_id")
)
public class ArchiveJpaEntity extends BaseEntity {

    /**
     * Storage tier classification for long-term archived documents.
     */
    public enum StorageTier {
        /** Standard cold storage (infrequent access, low cost) */
        COLD,
        /** Deep archive / glacier tier (very infrequent access, lowest cost) */
        GLACIER
    }

    /**
     * The document that has been archived.
     * One-to-one relationship enforced by the UNIQUE constraint on the FK column.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_archives_document")
    )
    private DocumentJpaEntity document;

    /**
     * Human-readable reason for archiving this document.
     */
    @Column(name = "archive_reason", columnDefinition = "TEXT")
    private String archiveReason;

    /**
     * User who initiated the archiving operation.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "archived_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_archives_archived_by")
    )
    private UserJpaEntity archivedBy;

    /**
     * Timestamp when the document was archived.
     */
    @Column(name = "archived_at", nullable = false, updatable = false)
    private Instant archivedAt;

    /**
     * Name of the retention policy applied to this archived document.
     * Example: {@code "LEGAL_7Y"}, {@code "AUDIT_10Y"}.
     */
    @Column(name = "retention_policy", length = 100)
    private String retentionPolicy;

    /**
     * Current object-storage tier for the archived file.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_tier", nullable = false, length = 30)
    @Builder.Default
    private StorageTier storageTier = StorageTier.COLD;

    /**
     * Timestamp when a restore from cold/glacier storage was requested.
     * {@code null} if no restore has been requested.
     */
    @Column(name = "restore_requested_at")
    private Instant restoreRequestedAt;

    /**
     * Timestamp when the restore from cold/glacier storage completed.
     * {@code null} if still archiving or restore pending.
     */
    @Column(name = "restored_at")
    private Instant restoredAt;
}
