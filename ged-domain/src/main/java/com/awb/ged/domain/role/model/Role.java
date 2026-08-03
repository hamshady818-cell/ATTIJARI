package com.awb.ged.domain.role.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Role</h1>
 * <p>
 * Domain aggregate representing an application role in the GED-AWB system.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /** Unique identifier for the role */
    private UUID id;

    /** Role name (UPPER_SNAKE_CASE, e.g. "SUPER_ADMIN") */
    private String name;

    /** Role description */
    private String description;

    /** UTC creation timestamp */
    private Instant createdAt;

    /** UTC update timestamp */
    private Instant updatedAt;
}
