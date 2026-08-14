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
    private UUID parentId;
    private String path;
    /** Security classification key (e.g., "FINANCE") — corresponds to Keycloak client role DOC_TYPE_* */
    private String securityClass;
    private Instant createdAt;
    private Instant updatedAt;
}
