package com.awb.ged.domain.document.event;

import com.awb.ged.common.event.DomainEvent;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentUploadedEvent</h1>
 * <p>
 * Domain event published when a physical file content has been uploaded and a new version is created.
 * This triggers the OCR, LLM metadata generation and Vectorization pipeline.
 * </p>
 */
@Value
public class DocumentUploadedEvent implements DomainEvent {

    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();
    UUID documentId;
    UUID versionId;
    String hash;
    long sizeBytes;
    UUID uploadedBy;
}
