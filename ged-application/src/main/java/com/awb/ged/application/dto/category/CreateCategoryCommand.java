package com.awb.ged.application.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryCommand {
    private String name;
    private UUID parentId;

    /**
     * Security classification key (e.g., "FINANCE") — must match the suffix of a Keycloak
     * client role {@code DOC_TYPE_*} on {@code ged-boot}. Nullable = no type restriction.
     */
    private String securityClass;
}
