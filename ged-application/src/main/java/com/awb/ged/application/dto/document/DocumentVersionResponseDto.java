package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Extended version DTO with filename and change summary for version history display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponseDto {

    private UUID id;
    private UUID documentId;
    private int versionNumber;
    private String versionLabel;
    private String hash;
    private long sizeBytes;
    private String mimeType;
    private String fileReferenceId;
    private String changeSummary;
    private boolean majorVersion;
    private UUID uploadedBy;
    private String uploadedByUsername;
    private Instant uploadedAt;
}
