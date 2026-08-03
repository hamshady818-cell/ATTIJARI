package com.awb.ged.domain.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Notification</h1>
 * <p>
 * Domain aggregate representing a notification.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    public enum Channel {
        IN_APP,
        EMAIL,
        PUSH
    }

    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String body;
    private String entityType;
    private UUID entityId;
    private Channel channel; // IN_APP, EMAIL, PUSH
    private String status; // "PENDING", "SENT", "READ", "FAILED"
    private Instant readAt;
    private Instant sentAt;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
