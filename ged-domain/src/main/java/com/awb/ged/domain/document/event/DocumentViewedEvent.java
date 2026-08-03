package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentViewedEvent</h1>
 * <p>
 * Domain event published when a document is viewed or metadata is consulted.
 * Ensures audit traceability in banking compliance context.
 * </p>
 */
@Value
public class DocumentViewedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID documentId;
    String documentName;
    UUID viewedBy;
}
