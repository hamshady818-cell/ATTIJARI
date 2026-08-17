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
    /** Optional description of the category's purpose */
    private String description;

    /** Hex color code for UI display (e.g., "#C8102E") */
    private String color;

    /** Icon identifier string for UI rendering */
    private String icon;

    /** Security classification key used to enforce document type access control */
    private String securityClass;

    /** Whether this category is active. Defaults to true */
    @Builder.Default
    private boolean active = true;

    /** UTC timestamp of soft delete, null if active */
    private Instant deletedAt;

    /** ID of user who soft-deleted this category */
    private UUID deletedBy;

    /** Number of metadata definitions associated with this category */
    private Integer metadataCount;

    /** UTC timestamp when the category was created */
    private Instant createdAt;

    /** UTC timestamp when the category was last updated */
    private Instant updatedAt;
}
