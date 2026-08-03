package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionResponseDto {

    private UUID id;
    private UUID documentId;
    private int versionNumber;
    private String hash;
    private long sizeBytes;
    private String fileReferenceId;
    private UUID uploadedBy;
    private Instant uploadedAt;
}
