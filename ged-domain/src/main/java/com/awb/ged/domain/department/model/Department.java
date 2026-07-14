package com.awb.ged.domain.department.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Department</h1>
 * <p>
 * Core domain aggregate representing a department or organizational unit in the enterprise (e.g., HR, Finance).
 * Supports hierarchical nesting through a self-referencing parentId.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    /** Unique identifier for the department */
    private UUID id;

    /** Name of the department (e.g. "Direction Financière") */
    private String name;

    /** Parent department identifier (null if it is a root department) */
    private UUID parentId;

    /** Timestamp of creation in UTC */
    private Instant createdAt;

    /** Timestamp of last modification in UTC */
    private Instant updatedAt;
}
