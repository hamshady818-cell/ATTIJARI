package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    private String categoryName;
    private UUID departmentId;
    private String departmentName;
    private UUID ownerId;
    private String ownerUsername;
    private String ownerName;
    private LocalDate expirationDate;
    private UUID activeVersionId;
    private boolean isLocked;
    private List<String> tags;
    private List<DocumentMetadataValueDto> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean getIsLocked() {
        return isLocked;
    }
}
