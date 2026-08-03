package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentDeletedEvent</h1>
 * <p>
 * Domain event published when a document is soft-deleted.
 * </p>
 */
@Value
public class DocumentDeletedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID documentId;
    String name;
    UUID folderId;
    UUID deletedBy;
}
