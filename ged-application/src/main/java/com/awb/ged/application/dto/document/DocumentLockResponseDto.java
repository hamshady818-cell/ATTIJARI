package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing lock/checkout state of a document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLockResponseDto {

    private UUID documentId;
    private boolean locked;
    private UUID lockedBy;
    private String lockedByUsername;
    private Instant lockedAt;
    private Instant lockExpiration;
}
