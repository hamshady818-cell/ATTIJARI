package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * <h1>DocumentExpiredEvent</h1>
 * <p>
 * Domain event published when a document is automatically archived because its
 * {@code expirationDate} has passed. Triggered by the scheduled expiry job,
 * not by a human actor — hence {@code expiredBy} is always {@code null}.
 * </p>
 */
@Value
public class DocumentExpiredEvent implements DomainEvent {

    UUID      eventId        = UUID.randomUUID();
    Instant   occurredAt     = Instant.now();

    /** ID of the document that has been archived due to expiry. */
    UUID      documentId;

    /** Human-readable name of the document, carried for listeners that need it without a DB reload. */
    String    documentName;

    /** Owner of the document at the time of expiry. */
    UUID      ownerId;

    /** The expiration date that triggered this event. */
    LocalDate expirationDate;
}

