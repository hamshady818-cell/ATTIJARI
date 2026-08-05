package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight DTO returned by search results to avoid N+1 queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSearchResultDto {

    private UUID id;
    private String name;
    private String description;
    private String status;
    private String mimeType;
    private UUID folderId;
    private String folderName;
    private UUID categoryId;
    private String categoryName;
    private UUID ownerId;
    private String ownerUsername;
    private UUID activeVersionId;
    private boolean isLocked;
    private java.util.List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
