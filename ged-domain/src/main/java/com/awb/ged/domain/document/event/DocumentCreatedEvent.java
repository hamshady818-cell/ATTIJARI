package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentCreatedEvent</h1>
 * <p>
 * Domain event published when a new document metadata container is initialized in the system.
 * </p>
 */
@Value
public class DocumentCreatedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID documentId;
    String name;
    UUID ownerId;
    UUID folderId;
}
