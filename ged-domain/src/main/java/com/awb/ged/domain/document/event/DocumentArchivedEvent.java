package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentArchivedEvent</h1>
 * <p>
 * Domain event published when a document is soft-deleted, moved to the archive/trash.
 * </p>
 */
@Value
public class DocumentArchivedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID documentId;
    UUID archivedBy;
}
