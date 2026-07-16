package com.awb.ged.infrastructure.persistence.entity.document;

import com.awb.ged.infrastructure.persistence.entity.category.CategoryJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.metadata.DocumentMetadataJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.tag.TagJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h1>DocumentJpaEntity</h1>
 * <p>
 * JPA entity representing the <strong>logical document aggregate root</strong> in GED-AWB.
 * It is the central entity of the system — referenced by virtually every other entity.
 * </p>
 *
 * <p>
 * A document is the <em>logical identity</em> of a file across all its versions, metadata,
 * tags, categories, permissions, and lifecycle states. The physical file bytes live in
 * {@link DocumentVersionJpaEntity}, referenced via {@code currentVersion}.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link FolderJpaEntity} — parent folder.</li>
 *   <li>M:1 → {@link UserJpaEntity} — owner, createdBy, updatedBy.</li>
 *   <li>M:1 (nullable) → {@link DocumentVersionJpaEntity} — pointer to current active version.</li>
 *   <li>1:N → {@link DocumentVersionJpaEntity} (all versions).</li>
 *   <li>1:N → {@link DocumentCheckoutJpaEntity} (checkout history).</li>
 *   <li>1:N → {@link DocumentMetadataJpaEntity} (EAV metadata values).</li>
 *   <li>1:N → {@link DocumentPermissionJpaEntity} (document-level ACL).</li>
 *   <li>M:N → {@link CategoryJpaEntity} — join table {@code document_categories}.</li>
 *   <li>M:N → {@link TagJpaEntity} — join table {@code document_tags}.</li>
 * </ul>
 *
 * <p><strong>Design Decision — DocumentStatus enum:</strong>
 * Document lifecycle is modeled as a typed enum stored as a STRING column.
 * Transitions (e.g., DRAFT → PUBLISHED) are enforced by application-layer state machines.
 * </p>
 *
 * <p><strong>Design Decision — currentVersion circular FK:</strong>
 * The FK {@code documents.current_version_id → document_versions.id} is DEFERRABLE
 * INITIALLY DEFERRED in the Flyway migration. On the JPA side, this field is nullable
 * and set after the first version is persisted. No {@code @OneToOne} is used here —
 * it's modeled as a simple {@code @ManyToOne} to a version, avoiding JPA's
 * bidirectional OneToOne complexity.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "documents",
        indexes = {
                @Index(name = "idx_documents_folder_id",   columnList = "folder_id"),
                @Index(name = "idx_documents_owner_id",    columnList = "owner_id"),
                @Index(name = "idx_documents_status",      columnList = "document_status"),
                @Index(name = "idx_documents_created_at",  columnList = "created_at DESC")
        }
)
public class DocumentJpaEntity extends BaseEntity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Enum — Document lifecycle states
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lifecycle status of a document.
     */
    public enum DocumentStatus {
        /** Initial state — not yet reviewed or published */
        DRAFT,
        /** Formally approved and accessible to authorized users */
        PUBLISHED,
        /** Moved to long-term cold storage — immutable */
        ARCHIVED,
        /** Moved to trash — pending permanent deletion */
        TRASHED
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core metadata
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Human-readable title of the document.
     */
    @NotBlank
    @Size(max = 500)
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Optional extended description of the document's content or purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle status of this document.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 30)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    /**
     * MIME type of the document's content (e.g., {@code "application/pdf"}).
     * Denormalized from the current version for fast access without a join.
     */
    @Size(max = 100)
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * File extension of the document (e.g., {@code ".pdf"}, {@code ".docx"}).
     */
    @Size(max = 20)
    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    /**
     * Primary language of the document's content (ISO 639-1, e.g., {@code "fr"}, {@code "ar"}).
     */
    @Size(max = 10)
    @Column(name = "language", length = 10)
    private String language;

    /**
     * Whether this document is confidential and requires elevated permissions to access.
     */
    @Column(name = "is_confidential", nullable = false)
    @Builder.Default
    private boolean confidential = false;

    /**
     * Legal retention deadline — document must not be deleted before this date.
     * {@code null} means no specific retention policy applies.
     */
    @Column(name = "retention_until")
    private LocalDate retentionUntil;

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationships — ownership & location
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Folder in which this document resides.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "folder_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_documents_folder")
    )
    private FolderJpaEntity folder;

    /**
     * User who owns this document and has full management rights.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_documents_owner")
    )
    private UserJpaEntity owner;

    /**
     * User who created this document record.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_documents_created_by")
    )
    private UserJpaEntity createdBy;

    /**
     * User who last modified this document's properties.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_documents_updated_by")
    )
    private UserJpaEntity updatedBy;

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationship — current active version
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pointer to the currently active version of this document.
     * <p>
     * This FK is DEFERRABLE INITIALLY DEFERRED in the database (see Flyway migration).
     * It is {@code null} during initial document creation and set after the first
     * {@link DocumentVersionJpaEntity} is persisted.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "current_version_id",
            foreignKey = @ForeignKey(name = "fk_documents_current_version")
    )
    private DocumentVersionJpaEntity currentVersion;

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationship — all versions (history)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Complete immutable version history of this document.
     * New versions are appended; old versions are never deleted.
     */
    @OneToMany(
            mappedBy      = "document",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.ALL,
            orphanRemoval = false
    )
    @OrderBy("versionNumber ASC")
    @Builder.Default
    private List<DocumentVersionJpaEntity> versions = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationship — checkout history
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full checkout history for this document (active and past checkouts).
     * Active checkout has {@code checkedInAt = null}.
     */
    @OneToMany(
            mappedBy      = "document",
            fetch         = FetchType.LAZY,
            cascade       = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = false
    )
    @Builder.Default
    private List<DocumentCheckoutJpaEntity> checkouts = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationship — EAV metadata values
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dynamic metadata values assigned to this document (EAV pattern).
     * Orphan removal enabled — removing a value from this collection deletes it.
     */
    @OneToMany(
            mappedBy      = "document",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<DocumentMetadataJpaEntity> metadata = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationship — document-level ACL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Document-level ACL overrides. Take precedence over folder-level permissions.
     * Orphan removal enabled — removing an entry from this collection deletes it.
     */
    @OneToMany(
            mappedBy      = "document",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<DocumentPermissionJpaEntity> permissions = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  M:N — Categories
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Categories this document is classified under.
     * A document may belong to multiple categories simultaneously.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "document_categories",
            joinColumns        = @JoinColumn(name = "document_id",
                                             foreignKey = @ForeignKey(name = "fk_doc_cat_document")),
            inverseJoinColumns = @JoinColumn(name = "category_id",
                                             foreignKey = @ForeignKey(name = "fk_doc_cat_category"))
    )
    @Builder.Default
    private Set<CategoryJpaEntity> categories = new HashSet<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  M:N — Tags
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Free-form tags applied to this document for quick filtering and search.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "document_tags",
            joinColumns        = @JoinColumn(name = "document_id",
                                             foreignKey = @ForeignKey(name = "fk_doc_tag_document")),
            inverseJoinColumns = @JoinColumn(name = "tag_id",
                                             foreignKey = @ForeignKey(name = "fk_doc_tag_tag"))
    )
    @Builder.Default
    private Set<TagJpaEntity> tags = new HashSet<>();
}
