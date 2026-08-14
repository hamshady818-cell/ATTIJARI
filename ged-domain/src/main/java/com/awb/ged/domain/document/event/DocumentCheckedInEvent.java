package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentCheckedInEvent</h1>
 * <p>
 * Domain event published when a document is checked back in (lock released) by a user.
 * Used to notify the document owner that the document is available again.
 * </p>
 */
@Value
public class DocumentCheckedInEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    /** ID of the document that was unlocked. */
    UUID documentId;
    /** Name of the document (denormalized for notification body). */
    String documentName;
    /** User who released the lock. */
    UUID checkedInBy;
    /** Owner of the document — potential recipient of the notification. */
    UUID ownerId;
}
