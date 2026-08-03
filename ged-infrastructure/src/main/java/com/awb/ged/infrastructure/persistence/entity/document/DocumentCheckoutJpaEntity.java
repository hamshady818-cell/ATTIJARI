package com.awb.ged.infrastructure.persistence.entity.document;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * <h1>DocumentCheckoutJpaEntity</h1>
 * <p>
 * JPA entity representing a check-out lock record for a {@link DocumentJpaEntity}.
 * </p>
 *
 * <p>
 * When a user checks out a document, a new row is created with {@code checkedInAt = null},
 * indicating an active exclusive lock. Upon check-in, {@code checkedInAt} is stamped
 * and a new {@link DocumentVersionJpaEntity} is linked via {@code newVersion}.
 * </p>
 *
 * <p>
 * The active-checkout uniqueness constraint ({@code unique WHERE checked_in_at IS NULL})
 * is a partial index defined in the Flyway migration — JPA cannot express this directly.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link DocumentJpaEntity} — the locked document.</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code checkedOutBy} — the lock holder.</li>
 *   <li>M:1 (nullable) → {@link DocumentVersionJpaEntity} via {@code newVersion}
 *       — the version produced on check-in.</li>
 *   <li>M:1 (nullable) → {@link UserJpaEntity} via {@code forcedBy}
 *       — admin who force-released the lock.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "document_checkouts",
        indexes = {
                @Index(name = "idx_checkouts_document_id",    columnList = "document_id"),
                @Index(name = "idx_checkouts_checked_out_by", columnList = "checked_out_by")
        }
)
public class DocumentCheckoutJpaEntity extends BaseEntity {

    /**
     * The document being locked for exclusive editing.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_checkouts_document")
    )
    private DocumentJpaEntity document;

    /**
     * User who holds the exclusive checkout lock.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "checked_out_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_checkouts_user")
    )
    private UserJpaEntity checkedOutBy;

    /**
     * Timestamp when the document was checked out (lock acquired).
     */
    @Column(name = "checked_out_at", nullable = false, updatable = false)
    private Instant checkedOutAt;

    /**
     * Optional comment from the user explaining why they are checking out this document.
     */
    @Column(name = "checkout_comment", columnDefinition = "TEXT")
    private String checkoutComment;

    /**
     * Expected date/time by which the user intends to check the document back in.
     * Informational only — does not automatically release the lock.
     */
    @Column(name = "expected_return_at")
    private Instant expectedReturnAt;

    /**
     * Timestamp when the document was checked back in (lock released).
     * {@code null} means the checkout is still active — the document is currently locked.
     */
    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    /**
     * The new document version created as a result of this check-in.
     * {@code null} until the document is checked back in.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "new_version_id",
            foreignKey = @ForeignKey(name = "fk_checkouts_new_version")
    )
    private DocumentVersionJpaEntity newVersion;

    /**
     * Administrator who force-released the lock (overriding the checkout holder).
     * {@code null} for normal check-ins by the lock holder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "forced_by",
            foreignKey = @ForeignKey(name = "fk_checkouts_forced_by")
    )
    private UserJpaEntity forcedBy;

    // ─────────────────────────────────────────────────────────────────────────
    //  Convenience methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this checkout is still active (document is locked).
     */
    public boolean isActive() {
        return this.checkedInAt == null;
    }
}
