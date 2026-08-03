package com.awb.ged.domain.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>NotificationSubscription</h1>
 * <p>
 * Domain aggregate representing a user's notification preference for specific domain events.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSubscription {

    /** Unique identifier for the subscription */
    private UUID id;

    /** Identifier of the subscribing user */
    private UUID userId;

    /** Event type subscribed to (e.g. "DOCUMENT_SHARED") */
    private String eventType;

    /** Optional entity type scope */
    private String entityType;

    /** Optional specific entity UUID scope */
    private UUID entityId;

    /** Preferred delivery channel */
    private Notification.Channel channel;

    /** Active flag */
    private boolean active;

    /** UTC creation timestamp */
    private Instant createdAt;

    /** UTC update timestamp */
    private Instant updatedAt;
}
