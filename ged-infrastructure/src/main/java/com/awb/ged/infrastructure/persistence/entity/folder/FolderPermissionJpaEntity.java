package com.awb.ged.infrastructure.persistence.entity.folder;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.GroupJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * <h1>FolderPermissionJpaEntity</h1>
 * <p>
 * JPA entity representing an Access Control List (ACL) entry that grants specific
 * permissions on a {@link FolderJpaEntity} to either a {@link UserJpaEntity}
 * or a {@link GroupJpaEntity} — never both simultaneously.
 * </p>
 *
 * <p>
 * Permissions are additive and can be inherited from parent folders.
 * The {@code inherited} flag distinguishes explicitly granted permissions from
 * those propagated automatically from a parent folder.
 * </p>
 *
 * <p><strong>Constraint (enforced at DB level via Flyway migration):</strong>
 * {@code CHECK ((user_id IS NOT NULL AND group_id IS NULL) OR (user_id IS NULL AND group_id IS NOT NULL))}
 * — exactly one of {@code user} or {@code group} must be set. JPA cannot express this
 * as an annotation; it is enforced by the database migration script.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link FolderJpaEntity} — the folder being controlled.</li>
 *   <li>M:1 → {@link UserJpaEntity} (nullable) — user-level grant.</li>
 *   <li>M:1 → {@link GroupJpaEntity} (nullable) — group-level grant.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code grantedBy} — admin who granted.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "folder_permissions",
        indexes = {
                @Index(name = "idx_folder_perm_folder_id", columnList = "folder_id"),
                @Index(name = "idx_folder_perm_user_id",   columnList = "user_id"),
                @Index(name = "idx_folder_perm_group_id",  columnList = "group_id")
        }
)
public class FolderPermissionJpaEntity extends BaseEntity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Target folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The folder on which this ACL entry grants permissions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "folder_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_folder_perm_folder")
    )
    private FolderJpaEntity folder;

    // ─────────────────────────────────────────────────────────────────────────
    //  Principal (user XOR group)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Target user for this permission grant.
     * {@code null} when the grant targets a group instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_folder_perm_user")
    )
    private UserJpaEntity user;

    /**
     * Target group for this permission grant.
     * {@code null} when the grant targets an individual user instead.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "group_id",
            foreignKey = @ForeignKey(name = "fk_folder_perm_group")
    )
    private GroupJpaEntity group;

    // ─────────────────────────────────────────────────────────────────────────
    //  Permission flags
    // ─────────────────────────────────────────────────────────────────────────

    /** Grants permission to list and open documents in this folder. */
    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private boolean canRead = false;

    /** Grants permission to upload, edit, and check in documents in this folder. */
    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private boolean canWrite = false;

    /** Grants permission to move documents to trash from this folder. */
    @Column(name = "can_delete", nullable = false)
    @Builder.Default
    private boolean canDelete = false;

    /** Grants permission to manage the ACL of this folder (admin-level). */
    @Column(name = "can_manage", nullable = false)
    @Builder.Default
    private boolean canManage = false;

    /**
     * {@code true} if this entry was propagated from a parent folder;
     * {@code false} if it was explicitly set on this folder.
     */
    @Column(name = "is_inherited", nullable = false)
    @Builder.Default
    private boolean inherited = false;

    // ─────────────────────────────────────────────────────────────────────────
    //  Audit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Administrator who created this permission grant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "granted_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_folder_perm_granted_by")
    )
    private UserJpaEntity grantedBy;
}
