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

    /** UTC timestamp when the category was created */
    private Instant createdAt;

    /** UTC timestamp when the category was last updated */
    private Instant updatedAt;
}
