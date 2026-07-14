package com.awb.ged.domain.folder.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>FolderCreatedEvent</h1>
 * <p>
 * Domain event published when a new folder is created in the hierarchical tree.
 * </p>
 */
@Value
public class FolderCreatedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID folderId;
    String name;
    UUID parentId;
    UUID ownerId;
}
