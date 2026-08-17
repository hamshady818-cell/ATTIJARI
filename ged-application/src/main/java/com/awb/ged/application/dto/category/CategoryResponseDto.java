package com.awb.ged.application.dto.category;

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
public class CategoryResponseDto {
    private UUID id;
    private String name;
    private String description;
    private UUID parentId;
    private String path;
    private String color;
    private String icon;
    /** Security classification key (e.g., "FINANCE") — corresponds to Keycloak client role DOC_TYPE_* */
    private String securityClass;
    private boolean active;
    private Instant deletedAt;
    private UUID deletedBy;
    private Integer metadataCount;
    private Instant createdAt;
    private Instant updatedAt;
}
