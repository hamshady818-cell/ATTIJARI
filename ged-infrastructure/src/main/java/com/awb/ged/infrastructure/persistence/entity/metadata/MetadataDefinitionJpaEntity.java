package com.awb.ged.infrastructure.persistence.entity.metadata;

import com.awb.ged.infrastructure.persistence.entity.category.CategoryJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.shared.BaseEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * <h1>MetadataDefinitionJpaEntity</h1>
 * <p>
 * JPA entity representing the <strong>schema definition</strong> of a custom metadata field
 * in the GED-AWB EAV (Entity-Attribute-Value) system.
 * </p>
 *
 * <p>
 * Each definition describes one typed field that users can fill in on documents:
 * e.g., "Invoice Number" (TEXT), "Contract Value" (NUMBER), "Signature Date" (DATE).
 * Definitions can be scoped to a specific {@link CategoryJpaEntity} or left global
 * (available on all documents regardless of category).
 * </p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>M:1 → {@link CategoryJpaEntity} (nullable — NULL means global scope).</li>
 *   <li>M:1 → {@link UserJpaEntity} via {@code createdBy}.</li>
 *   <li>1:N → {@link DocumentMetadataJpaEntity} (actual values stored on documents).</li>
 * </ul>
 *
 * <p><strong>Design Decision — {@code FieldType} enum:</strong>
 * Stored as {@code STRING} to keep the database column human-readable and resilient
 * to enum reordering. Adding new types is a schema-free operation.
 * </p>
 *
 * <p><strong>Design Decision — {@code allowedValues} as JSONB:</strong>
 * For {@code SELECT} and {@code MULTI_SELECT} field types, the allowed option list
 * is stored as a JSONB array. This avoids a separate "allowed_values" table for
 * what is essentially a configuration structure.
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "metadata_definitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_metadata_def_category_field",
                columnNames = {"category_id", "field_name"}
        ),
        indexes = {
                @Index(name = "idx_metadata_def_category_id", columnList = "category_id"),
                @Index(name = "idx_metadata_def_field_name",  columnList = "field_name")
        }
)
public class MetadataDefinitionJpaEntity extends BaseEntity {

    // ─────────────────────────────────────────────────────────────────────────
    //  Enum — Field Types
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Supported data types for custom metadata fields.
     */
    public enum FieldType {
        /** Free-form text input */
        TEXT,
        /** Numeric value (integer or decimal) */
        NUMBER,
        /** Calendar date (ISO-8601) */
        DATE,
        /** Date and time (ISO-8601) */
        DATETIME,
        /** Boolean true/false toggle */
        BOOLEAN,
        /** Single value from a predefined list */
        SELECT,
        /** Multiple values from a predefined list */
        MULTI_SELECT,
        /** HTTP/HTTPS URL */
        URL
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Internal field name used as a key in queries and API responses.
     * Should be a camelCase or snake_case identifier (e.g., {@code "invoiceNumber"}).
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    /**
     * Human-readable label shown in the UI (e.g., "Invoice Number", "Contract Value").
     */
    @NotBlank
    @Size(max = 200)
    @Column(name = "display_label", nullable = false, length = 200)
    private String displayLabel;

    /**
     * The data type of this field — determines the input widget and validation rules.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 30)
    private FieldType fieldType;

    /**
     * Whether this field must be filled in before a document can be published.
     */
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private boolean required = false;

    /**
     * Whether this field's value is indexed for full-text search.
     */
    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean searchable = true;

    /**
     * Whether this field is exposed as a filter option in the search UI.
     */
    @Column(name = "is_filterable", nullable = false)
    @Builder.Default
    private boolean filterable = true;

    /**
     * Default value (serialized as text) pre-filled when the field is displayed.
     */
    @Column(name = "default_value")
    private String defaultValue;

    /**
     * For {@code SELECT} and {@code MULTI_SELECT} fields: the list of allowed options.
     * Stored as a JSONB array of strings (e.g., {@code ["Option A", "Option B"]}).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_values", columnDefinition = "jsonb")
    private List<String> allowedValues;

    /**
     * Optional regex pattern for server-side validation of {@code TEXT} field values.
     * Example: {@code "^[A-Z]{2}\\d{6}$"} for a structured invoice reference.
     */
    @Column(name = "validation_regex")
    private String validationRegex;

    /**
     * Controls the display order of this field in the metadata form UI.
     * Lower values appear first.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    // ─────────────────────────────────────────────────────────────────────────
    //  Relationships
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Category to which this field definition is scoped.
     * {@code null} means the field is <strong>global</strong> — available on all documents.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            foreignKey = @ForeignKey(name = "fk_metadata_def_category")
    )
    private CategoryJpaEntity category;

    /**
     * User who created this metadata field definition.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_metadata_def_created_by")
    )
    private UserJpaEntity createdBy;
}
