package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentCheckedOutEvent</h1>
 * <p>
 * Domain event published when a document is checked out (locked) by a user.
 * Used to notify the document owner when another user acquires the lock.
 * </p>
 */
@Value
public class DocumentCheckedOutEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    /** ID of the document that was locked. */
    UUID documentId;
    /** Name of the document (denormalized for notification body, avoids a DB lookup in the listener). */
    String documentName;
    /** User who performed the checkout. */
    UUID checkedOutBy;
    /** Owner of the document — potential recipient of the notification. */
    UUID ownerId;
}
