package com.awb.ged.domain.document.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * <h1>DocumentMetadataValue</h1>
 * <p>
 * Value Object representing a dynamic metadata instance on a specific document.
 * Refers back to a {@code MetadataDefinition} (schema) using {@code definitionId}.
 * </p>
 */
@Value
@Builder
public class DocumentMetadataValue {

    /** Identifier of the associated metadata definition schema */
    UUID definitionId;

    /** Direct name/key of the metadata field for fast resolution (redundant, matches definition.name) */
    String key;

    /** Serialized value string (e.g. "2026-07-14" for date or "150.5" for decimal) */
    String value;
}
