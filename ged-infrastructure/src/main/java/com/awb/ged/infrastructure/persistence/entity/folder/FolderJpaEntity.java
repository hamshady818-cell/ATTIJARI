package com.awb.ged.infrastructure.persistence.entity.folder;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>FolderJpaEntity</h1>
 * <p>
 * JPA entity representing a node in the GED-AWB folder hierarchy.
 * Folders organize documents into a virtual filesystem tree with
 * unlimited nesting depth.
 * </p>
 *
 * <p>
 * The {@code path} column stores the materialized path in PostgreSQL {@code ltree}
 * dot-notation (e.g., {@code "root.finance.invoices.2024"}), enabling fast subtree
 * queries without recursive CTEs.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>Self-referential M:1 → {@code parentFolder} (nullable for root folders).</li>
 *   <li>Self-referential 1:N → {@code children}.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code owner}, {@code createdBy}, {@code updatedBy}.</li>
 *   <li>1:N → {@link FolderPermissionJpaEntity} (ACL entries for this folder).</li>
 *   <li>1:N → {@code DocumentJpaEntity} (documents residing in this folder) — mapped on document side.</li>
 * </ul>
 *
 * <p><strong>Design Decision — System folders:</strong>
 * The {@code system} flag marks folders that are auto-created by the platform
 * (e.g., "Trash", "Archive", "Personal"). System folders cannot be renamed or deleted
 * by regular users — enforced by the application layer.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "folders",
        indexes = {
                @Index(name = "idx_folders_parent_id",  columnList = "parent_folder_id"),
                @Index(name = "idx_folders_owner_id",   columnList = "owner_id"),
                @Index(name = "idx_folders_path",       columnList = "path")
        }
)
public class FolderJpaEntity extends BaseEntity {

    /**
     * Display name of the folder (e.g., "Contracts 2024", "HR Policies").
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Optional description of the folder's purpose or content.
     */
    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Materialized path in ltree dot-notation for efficient subtree queries.
     * Example: {@code "1.42.156"} (using UUID-based numeric IDs or slugs).
     * Maintained by the application layer on folder create/move operations.
     */
    @Column(name = "path", nullable = false)
    private String path;

    /**
     * Optional hex color for UI folder icon (e.g., {@code "#10B981"}).
     */
    @Size(max = 7)
    @Column(name = "color", length = 7)
    private String color;

    /**
     * Optional icon identifier for UI rendering.
     */
    @Size(max = 50)
    @Column(name = "icon", length = 50)
    private String icon;

    /**
     * Whether this is a system-reserved folder (Trash, Archive, Root, Personal).
     * System folders cannot be deleted or renamed by regular users.
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean system = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Self-referential hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parent folder. {@code null} for root-level folders.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_folder_id",
            foreignKey = @ForeignKey(name = "fk_folders_parent_folder")
    )
    private FolderJpaEntity parentFolder;

    /**
     * Direct child folders under this folder.
     *
     * <p>LAZY fetch — tree traversal is an explicit use case, not the default.
     * {@code orphanRemoval = false}: deleting a parent must be handled by
     * the application (re-parent or reject if children exist).</p>
     */
    @OneToMany(
            mappedBy      = "parentFolder",
            fetch         = FetchType.LAZY,
            cascade       = CascadeType.PERSIST,
            orphanRemoval = false
    )
    @Builder.Default
    private List<FolderJpaEntity> children = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Audit & Ownership
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * User who owns this folder and has full management rights over it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_folders_owner")
    )
    private UserJpaEntity owner;

    /**
     * User who initially created this folder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_folders_created_by")
    )
    private UserJpaEntity createdBy;

    /**
     * User who last modified this folder's properties.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_folders_updated_by")
    )
    private UserJpaEntity updatedBy;

    /**
     * Timestamp of soft-deletion. Null if the folder is not deleted.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    /**
     * User who soft-deleted this folder. Null if not deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deleted_by",
            foreignKey = @ForeignKey(name = "fk_folders_deleted_by")
    )
    private UserJpaEntity deletedBy;

    // ─────────────────────────────────────────────────────────────────────────
    //  Bidirectional: ACL entries
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Access Control List entries for this folder.
     * Each entry grants specific permissions to a user or group.
     *
     * <p>Orphan removal is {@code true}: removing a permission entry from this
     * collection immediately deletes it from the database.</p>
     */
    @OneToMany(
            mappedBy      = "folder",
            fetch         = FetchType.LAZY,
            cascade       = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true
    )
    @Builder.Default
    private List<FolderPermissionJpaEntity> permissions = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
