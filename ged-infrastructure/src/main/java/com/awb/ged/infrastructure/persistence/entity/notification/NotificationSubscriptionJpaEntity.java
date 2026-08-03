package com.awb.ged.infrastructure.persistence.entity.notification;

import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * <h1>NotificationSubscriptionJpaEntity</h1>
 * <p>
 * JPA entity representing a user's event subscription preference.
 * Defines which events a user wants to be notified about and via which channel.
 * </p>
 *
 * <p>
 * Subscriptions can be scoped at three levels:
 * <ol>
 *   <li><strong>Global</strong> — {@code entityType = null, entityId = null}:
 *       notified for all events of this type regardless of entity.</li>
 *   <li><strong>Type-scoped</strong> — {@code entityType != null, entityId = null}:
 *       notified for all events of this type on a specific entity class.</li>
 *   <li><strong>Entity-scoped</strong> — both set: notified only for a specific entity.</li>
 * </ol>
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link UserJpaEntity} — the subscribing user.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "notification_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notif_sub_user_event_entity_channel",
                columnNames = {"user_id", "event_type", "entity_type", "entity_id", "channel"}
        ),
        indexes = @Index(name = "idx_notif_sub_user_id", columnList = "user_id")
)
public class NotificationSubscriptionJpaEntity extends BaseEntity {

    /**
     * The user who subscribed to this event type.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_notif_sub_user")
    )
    private UserJpaEntity user;

    /**
     * The event type the user subscribes to.
     * Examples: {@code DOCUMENT_SHARED}, {@code VERSION_CREATED}, {@code CHECKOUT_FORCED}.
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Optional entity type scope (e.g., {@code DOCUMENT}, {@code FOLDER}).
     * {@code null} means the subscription applies globally for this event type.
     */
    @Size(max = 50)
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * Optional specific entity UUID scope.
     * {@code null} means all entities of the given {@code entityType}.
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Delivery channel for this subscription.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    @Builder.Default
    private NotificationJpaEntity.Channel channel = NotificationJpaEntity.Channel.IN_APP;

    /**
     * Whether this subscription is currently active.
     * Inactive subscriptions are skipped during event dispatch.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
