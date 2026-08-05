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
public class DocumentResponseDto {

    private UUID id;
    private String name;
    private String description;
    private String status;
    private String mimeType;
    private UUID folderId;
    private UUID categoryId;
    private UUID ownerId;
    private UUID activeVersionId;
    private boolean isLocked;
    private java.util.List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
