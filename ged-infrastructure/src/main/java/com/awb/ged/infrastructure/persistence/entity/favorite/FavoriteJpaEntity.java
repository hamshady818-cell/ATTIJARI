package com.awb.ged.infrastructure.persistence.entity.favorite;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * <h1>FavoriteJpaEntity</h1>
 * <p>
 * JPA entity representing a user's bookmarked (favorited) item in GED-AWB.
 * Users can bookmark both documents and folders for quick access from a dedicated
 * "Favorites" section in the UI.
 * </p>
 *
 * <p>
 * The target entity (document or folder) is referenced via a polymorphic pair:
 * {@code entityType} (discriminator) + {@code entityId} (the UUID of the target).
 * This avoids separate favorite tables per entity type and keeps the model simple.
 * </p>
 *
 * <p><strong>Design Decision — Polymorphic reference:</strong>
 * JPA does not natively support polymorphic FKs to different tables in a single column.
 * The {@code entityId} + {@code entityType} pattern is used here instead of two nullable
 * FK columns, because the number of "favoriteable" entity types may grow over time.
 * Referential integrity for this pattern is enforced at the application layer.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link UserJpaEntity} — the user who created this bookmark.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_favorites_user_entity",
                columnNames = {"user_id", "entity_type", "entity_id"}
        ),
        indexes = @Index(name = "idx_favorites_user_id", columnList = "user_id, entity_type")
)
public class FavoriteJpaEntity extends BaseEntity {

    /**
     * Discriminator values for the type of entity being favorited.
     */
    public enum EntityType {
        DOCUMENT,
        FOLDER
    }

    /**
     * The user who created this bookmark.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_favorites_user")
    )
    private UserJpaEntity user;

    /**
     * Discriminator: the type of entity being bookmarked.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, updatable = false, length = 20)
    private EntityType entityType;

    /**
     * UUID of the bookmarked entity (document ID or folder ID).
     * Not a foreign key column — referential integrity is enforced by the application.
     */
    @NotNull
    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;
}
