package com.awb.ged.domain.category.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Category</h1>
 * <p>
 * Domain aggregate representing a document category within a hierarchical taxonomy.
 * Enables semantic classification and category-scoped metadata definitions.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    /** Unique identifier for the category */
    private UUID id;

    /** Display name of the category */
    private String name;

    /** Parent category identifier (null for root categories) */
    private UUID parentId;

    /** Materialized path in dot-notation for hierarchy traversal */
    private String path;

    /**
     * Security classification key used to enforce document type access control.
     * <p>
     * This value (e.g., {@code "FINANCE"}) must match, in uppercase, the suffix of a Keycloak
     * client role on {@code ged-boot}: {@code DOC_TYPE_FINANCE}.
     * </p>
     * <p>
     * When {@code null}, no document type restriction is enforced for this category;
     * any authenticated user with department access may access documents of this category.
     * </p>
     * <p>
     * Example mapping:
     * <ul>
     *   <li>securityClass {@code "FINANCE"} ↔ Keycloak role {@code DOC_TYPE_FINANCE}</li>
     *   <li>securityClass {@code "RH"} ↔ Keycloak role {@code DOC_TYPE_RH}</li>
     * </ul>
     * </p>
     */
    private String securityClass;

    /** UTC timestamp when the category was created */
    private Instant createdAt;

    /** UTC timestamp when the category was last updated */
    private Instant updatedAt;
}
