package com.awb.ged.domain.tag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Tag</h1>
 * <p>
 * Domain aggregate representing a normalized metadata tag in the GED-AWB system.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    /** Unique identifier for the tag */
    private UUID id;

    /** Normalized tag slug name (e.g. "finance-2024") */
    private String name;

    /** Description of the tag purpose */
    private String description;

    /** UTC creation timestamp */
    private Instant createdAt;

    /** UTC update timestamp */
    private Instant updatedAt;
}
