package com.awb.ged.infrastructure.persistence.entity.document;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.GroupJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * <h1>DocumentPermissionJpaEntity</h1>
 * <p>
 * JPA entity representing a document-level Access Control List (ACL) override entry.
 * </p>
 *
 * <p>
 * Document permissions take precedence over folder-level permissions and allow
 * fine-grained control over specific documents — for example, making a confidential
 * document accessible to only one user even within an open folder.
 * </p>
 *
 * <p>
 * As with folder permissions, exactly one of {@code user} or {@code group} must be set
 * (enforced via a database CHECK constraint in the Flyway migration script).
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link DocumentJpaEntity} — the controlled document.</li>
 *   <li>M:1 (nullable) → {@link UserJpaEntity} — user-level grant.</li>
 *   <li>M:1 (nullable) → {@link GroupJpaEntity} — group-level grant.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code grantedBy} — admin who created this grant.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "document_permissions",
        indexes = {
                @Index(name = "idx_doc_perm_document_id", columnList = "document_id"),
                @Index(name = "idx_doc_perm_user_id",     columnList = "user_id"),
                @Index(name = "idx_doc_perm_group_id",    columnList = "group_id")
        }
)
public class DocumentPermissionJpaEntity extends BaseEntity {

    /**
     * The document on which this ACL entry grants permissions.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doc_perm_document")
    )
    private DocumentJpaEntity document;

    /**
     * Target user for this permission grant (null if group-level).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_doc_perm_user")
    )
    private UserJpaEntity user;

    /**
     * Target group for this permission grant (null if user-level).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "group_id",
            foreignKey = @ForeignKey(name = "fk_doc_perm_group")
    )
    private GroupJpaEntity group;

    /** Grants permission to open and read this document. */
    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private boolean canRead = false;

    /** Grants permission to edit and check in new versions of this document. */
    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private boolean canWrite = false;

    /** Grants permission to move this document to trash. */
    @Column(name = "can_delete", nullable = false)
    @Builder.Default
    private boolean canDelete = false;

    /** Grants permission to share this document with other users. */
    @Column(name = "can_share", nullable = false)
    @Builder.Default
    private boolean canShare = false;

    /**
     * Administrator who created this permission grant.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "granted_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_doc_perm_granted_by")
    )
    private UserJpaEntity grantedBy;
}
