package com.awb.ged.infrastructure.persistence.entity.audit;

import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * <h1>AuditLogJpaEntity</h1>
 * <p>
 * JPA entity representing a single immutable entry in the GED-AWB audit log.
 * Every significant action (document view, edit, delete, permission change, login)
 * produces one row here.
 * </p>
 *
 * <p><strong>Immutability contract:</strong>
 * Rows in this table are <em>never updated or deleted</em> by the application.
 * The database role used by the application has only INSERT privileges on this table.
 * </p>
 *
 * <p><strong>Why no {@code extends BaseEntity}:</strong>
 * {@code BaseEntity} adds {@code createdAt} / {@code updatedAt} with a {@code @PreUpdate}
 * hook that would be redundant (and misleading) for an immutable log entry.
 * This entity manages its own timestamp via {@code occurredAt} and its own UUID
 * to avoid the {@code @PreUpdate} hook.
 * </p>
 *
 * <p><strong>Table partitioning:</strong>
 * This table should be RANGE-partitioned by {@code occurred_at} (monthly) in the
 * Flyway migration for query performance at scale. JPA is unaware of partitioning —
 * it simply queries the parent table.
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 (nullable) → {@link UserJpaEntity} — the actor ({@code null} for system events).</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_user_occurred",  columnList = "user_id, occurred_at DESC"),
                @Index(name = "idx_audit_entity",         columnList = "entity_type, entity_id, occurred_at DESC"),
                @Index(name = "idx_audit_action",         columnList = "action, occurred_at DESC"),
                @Index(name = "idx_audit_occurred_at",    columnList = "occurred_at DESC")
        }
)
public class AuditLogJpaEntity {

    /**
     * Unique identifier for this log entry.
     * Generated independently (not via {@code BaseEntity}) to avoid {@code @PreUpdate}.
     */
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * User who performed the action.
     * {@code null} for system-generated events (scheduled jobs, webhooks, etc.).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_audit_logs_user")
    )
    private UserJpaEntity user;

    /**
     * Standardized action code describing what happened.
     * Examples: {@code DOCUMENT_VIEWED}, {@code VERSION_CREATED}, {@code PERMISSION_GRANTED}.
     */
    @NotBlank
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Type of the entity affected by this action.
     * Examples: {@code DOCUMENT}, {@code FOLDER}, {@code USER}, {@code PERMISSION}.
     */
    @NotBlank
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * UUID of the specific entity affected by this action.
     * May be {@code null} for system-level events not tied to a specific entity.
     */
    @Column(name = "entity_id")
    private UUID entityId;

    /**
     * Snapshot of the entity's name at the time of the event.
     * Stored to preserve meaningful context even if the entity is later renamed or deleted.
     */
    @Column(name = "entity_name", length = 500)
    private String entityName;

    /**
     * State of the entity <em>before</em> the change (for update/delete events).
     * Stored as JSONB for flexibility across entity types.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Object oldValues;

    /**
     * State of the entity <em>after</em> the change (for create/update events).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Object newValues;

    /**
     * IP address of the client that performed the action.
     * Stored as a string representation of an IP address (IPv4 or IPv6).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Browser/client User-Agent string.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Keycloak session ID associated with this action for session-level tracing.
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * Request-level correlation ID for distributed tracing (matches X-Correlation-ID header).
     */
    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * Additional context-specific metadata for this event.
     * Stored as JSONB for schema-free extensibility.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Object metadata;

    /**
     * Precise UTC timestamp when the event occurred.
     * This is the partition key for range partitioning by month.
     */
    @NotNull
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    /**
     * Sets the {@code occurredAt} timestamp before initial persistence.
     * No {@code @PreUpdate} — this entity is immutable after insert.
     */
    @PrePersist
    protected void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }
}
