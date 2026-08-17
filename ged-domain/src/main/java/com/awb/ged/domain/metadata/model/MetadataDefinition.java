package com.awb.ged.domain.metadata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <h1>MetadataDefinition</h1>
 * <p>
 * Core domain aggregate representing the schema/definition of a dynamic metadata field.
 * Defines the validation rules, names, types, and constraints.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MetadataDefinition {

    /** Unique identifier for this definition */
    private UUID id;

    /** System name / key of the metadata field (e.g. "invoice_amount") */
    private String name;

    /** Human-readable label (e.g. "Montant Facture") */
    private String label;

    /** Expected primitive type of values for this metadata */
    private MetadataType type;

    /** True if this metadata is mandatory for all documents using it */
    private boolean required;

    /** Regular expression pattern for text/string formatting validations */
    private String validationPattern;

    /** List of allowed options for SELECT and MULTI_SELECT metadata types */
    private List<String> options;

    /** Optional description explaining the purpose of this metadata field */
    private String description;

    /** Default value pre-filled when displaying the metadata field */
    private String defaultValue;

    /** Display order in UI forms (lower values appear first) */
    @Builder.Default
    private Integer displayOrder = 0;

    /** Whether this definition is active. Defaults to true for new definitions */
    @Builder.Default
    private boolean active = true;

    /** Optional category scope ID. Null means global scope (all documents) */
    private UUID categoryId;

    /** Timestamp of creation in UTC */
    private Instant createdAt;

    /** Timestamp of last schema modification in UTC */
    private Instant updatedAt;

    /** Timestamp of soft delete in UTC */
    private Instant deletedAt;

    /** ID of user who soft-deleted this definition */
    private UUID deletedBy;
}
