package com.awb.ged.infrastructure.persistence.entity.metadata;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * <h1>DocumentMetadataJpaEntity</h1>
 * <p>
 * JPA entity representing the <strong>value side</strong> of the EAV
 * (Entity-Attribute-Value) pattern for custom document metadata in GED-AWB.
 * </p>
 *
 * <p>
 * Each row stores the value of one custom field (defined by a
 * {@link MetadataDefinitionJpaEntity}) on one specific {@link DocumentJpaEntity}.
 * The field type determines which column holds the value:
 * <ul>
 *   <li>{@code valueText} — for TEXT, NUMBER, DATE, DATETIME, BOOLEAN, URL, SELECT values.</li>
 *   <li>{@code valueJson} — for MULTI_SELECT and complex structured values (JSONB array).</li>
 * </ul>
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link DocumentJpaEntity} — the document holding this metadata value.</li>
 *   <li>M:1 → {@link MetadataDefinitionJpaEntity} — the field definition (schema).</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code updatedBy} — who last edited this value.</li>
 * </ul>
 *
 * <p><strong>Design Decision — Two value columns:</strong>
 * Using two nullable columns ({@code value_text} and {@code value_json}) instead of
 * a single generic {@code TEXT} column allows proper JSONB indexing for multi-value fields
 * while keeping scalar value queries efficient (index on {@code value_text}).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "document_metadata",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_doc_metadata_doc_def",
                columnNames = {"document_id", "metadata_definition_id"}
        ),
        indexes = {
                @Index(name = "idx_doc_metadata_document_id",   columnList = "document_id"),
                @Index(name = "idx_doc_metadata_definition_id", columnList = "metadata_definition_id"),
                @Index(name = "idx_doc_metadata_value_text",    columnList = "value_text")
        }
)
public class DocumentMetadataJpaEntity extends BaseEntity {

    /**
     * The document to which this metadata value belongs.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doc_metadata_document")
    )
    private DocumentJpaEntity document;

    /**
     * The field definition (schema) this value satisfies.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "metadata_definition_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doc_metadata_definition")
    )
    private MetadataDefinitionJpaEntity definition;

    /**
     * Scalar value for TEXT, NUMBER, DATE, DATETIME, BOOLEAN, URL, and SELECT fields.
     * Stored as a string and cast to the correct type at the application layer.
     * Examples: {@code "42.5"}, {@code "2024-07-15"}, {@code "true"}, {@code "Option A"}.
     */
    @Column(name = "value_text", columnDefinition = "TEXT")
    private String valueText;

    /**
     * Structured value for MULTI_SELECT and other complex types.
     * Stored as a JSONB array in PostgreSQL.
     * Example: {@code ["Option A", "Option C"]}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb")
    private Object valueJson;

    /**
     * User who last set or modified this metadata value.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_doc_metadata_updated_by")
    )
    private UserJpaEntity updatedBy;
}
