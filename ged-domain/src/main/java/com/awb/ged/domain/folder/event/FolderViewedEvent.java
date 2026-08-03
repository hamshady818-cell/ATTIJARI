package com.awb.ged.domain.folder.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>FolderViewedEvent</h1>
 * <p>
 * Domain event published when a folder content is viewed or browsed.
 * Ensures audit traceability in banking compliance context.
 * </p>
 */
@Value
public class FolderViewedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID folderId;
    String folderName;
    UUID viewedBy;
}
