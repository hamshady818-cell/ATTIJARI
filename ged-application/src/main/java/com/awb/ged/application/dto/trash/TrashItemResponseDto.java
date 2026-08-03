package com.awb.ged.application.dto.trash;

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
public class TrashItemResponseDto {

    private UUID id;
    private String entityType;
    private UUID entityId;
    private UUID originalFolderId;
    private UUID deletedBy;
    private Instant deletedAt;
    private Instant autoPurgeAt;
}
