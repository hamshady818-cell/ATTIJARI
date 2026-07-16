package com.awb.ged.infrastructure.persistence.entity.document;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * <h1>DocumentVersionJpaEntity</h1>
 * <p>
 * JPA entity representing a single <strong>immutable version snapshot</strong> of a document's
 * file content within GED-AWB.
 * </p>
 *
 * <p>
 * Every time a checked-out document is checked back in, a new {@code DocumentVersionJpaEntity}
 * is created. Previous versions are preserved indefinitely to support full version history,
 * rollback, and legal audit requirements.
 * </p>
 *
 * <p>
 * The physical file resides in object storage (MinIO / S3).
 * This entity stores the storage coordinates ({@code storageBucket}, {@code storagePath})
 * and integrity metadata ({@code checksumSha256}, {@code fileSizeBytes}) — not the file bytes themselves.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link DocumentJpaEntity} — the logical document this version belongs to.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code createdBy} — who uploaded this version.</li>
 *   <li>1:1 ← {@code DocumentJpaEntity.currentVersion} — bidirectional pointer (deferrable at DB level).</li>
 *   <li>1:1 ← {@code AiOcrResultJpaEntity} — OCR result for this version.</li>
 *   <li>1:1 ← {@code AiDocumentAnalysisJpaEntity} — AI analysis for this version.</li>
 *   <li>1:N ← {@code AiEmbeddingJpaEntity} — vector embedding chunks for this version.</li>
 *   <li>1:1 ← {@code SearchIndexLogJpaEntity} — indexing state for this version.</li>
 * </ul>
 *
 * <p><strong>Design Decision — Circular FK with DocumentJpaEntity:</strong>
 * {@code documents.current_version_id} references {@code document_versions.id} and vice versa.
 * This is handled by making the FK on {@code documents.current_version_id}
 * {@code DEFERRABLE INITIALLY DEFERRED} in the Flyway migration script,
 * allowing both rows to be inserted in the same transaction without ordering issues.
 * On the JPA side, {@code DocumentJpaEntity.currentVersion} is nullable on insert
 * and updated after the version is persisted.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "document_versions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_doc_versions_doc_number",
                columnNames = {"document_id", "version_number"}
        ),
        indexes = {
                @Index(name = "idx_doc_versions_document_id",  columnList = "document_id"),
                @Index(name = "idx_doc_versions_created_at",   columnList = "created_at DESC")
        }
)
public class DocumentVersionJpaEntity extends BaseEntity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Version identity
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The logical document that this version belongs to.
     * Insert-only reference — a version can never be reassigned to a different document.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_doc_versions_document")
    )
    private DocumentJpaEntity document;

    /**
     * Sequential version number within the document (starts at 1).
     * Monotonically increasing — never reused even after version deletion.
     */
    @Min(1)
    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    /**
     * Optional semantic label applied by the author or an approver.
     * Examples: {@code "v1.0-approved"}, {@code "v2-draft"}, {@code "final"}.
     */
    @Size(max = 50)
    @Column(name = "version_label", length = 50)
    private String versionLabel;

    // ─────────────────────────────────────────────────────────────────────────
    //  Storage reference (object store coordinates)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Object storage bucket containing this version's file.
     * Example: {@code "ged-documents"}.
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "storage_bucket", nullable = false, updatable = false, length = 100)
    private String storageBucket;

    /**
     * Object key / path within the storage bucket.
     * Example: {@code "2024/07/a3f1c2d4-…/v3.pdf"}.
     */
    @NotBlank
    @Column(name = "storage_path", nullable = false, updatable = false)
    private String storagePath;

    // ─────────────────────────────────────────────────────────────────────────
    //  File integrity & metadata
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SHA-256 hex digest of the file content.
     * Used for integrity verification and duplicate detection across versions.
     */
    @NotBlank
    @Size(min = 64, max = 64)
    @Column(name = "checksum_sha256", nullable = false, updatable = false, length = 64)
    private String checksumSha256;

    /**
     * Size of the stored file in bytes.
     */
    @Min(0)
    @Column(name = "file_size_bytes", nullable = false, updatable = false)
    private long fileSizeBytes;

    /**
     * MIME type of the file in this version (e.g., {@code "application/pdf"}).
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, updatable = false, length = 100)
    private String mimeType;

    // ─────────────────────────────────────────────────────────────────────────
    //  Version metadata
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Human-readable description of what changed in this version.
     * Provided by the user during check-in.
     */
    @Column(name = "change_summary")
    private String changeSummary;

    /**
     * {@code true} for major versions (e.g., approved/published snapshots);
     * {@code false} for minor incremental saves.
     */
    @Column(name = "is_major_version", nullable = false)
    @Builder.Default
    private boolean majorVersion = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Audit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * User who created (uploaded) this version.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_doc_versions_created_by")
    )
    private UserJpaEntity createdBy;
}
