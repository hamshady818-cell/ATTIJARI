package com.awb.ged.application.dto.folder;

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
public class FolderResponseDto {

    private UUID id;
    private String name;
    private UUID parentId;
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
}
