package com.awb.ged.domain.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>AuditLog</h1>
 * <p>
 * Domain aggregate representing an immutable audit log entry in the GED-AWB system.
 * Records significant business and system events for traceability and compliance.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /** Unique identifier of the audit log entry */
    private UUID id;

    /** Identifier of the user who performed the action (null for system actions) */
    private UUID userId;

    /** Action code describing what happened (e.g. "DOCUMENT_VIEWED", "VERSION_CREATED") */
    private String action;

    /** Type of the entity affected (e.g. "DOCUMENT", "FOLDER", "USER") */
    private String entityType;

    /** Identifier of the affected entity (optional) */
    private UUID entityId;

    /** Name or label of the entity at the time of event */
    private String entityName;

    /** Client IP address */
    private String ipAddress;

    /** UTC timestamp when the action occurred */
    private Instant occurredAt;
}
