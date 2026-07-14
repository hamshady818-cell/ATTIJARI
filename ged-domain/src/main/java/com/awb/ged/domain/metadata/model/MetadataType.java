package com.awb.ged.domain.metadata.model;

/**
 * <h1>MetadataType</h1>
 * <p>
 * Supported primitive types for dynamic metadata definitions in the GED-AWB system.
 * </p>
 */
public enum MetadataType {
    /** Plain text strings */
    STRING,

    /** Integer numeric values */
    INTEGER,

    /** Floating point or decimal numeric values (e.g. monetary amounts) */
    DECIMAL,

    /** Temporal dates or timestamps */
    DATE,

    /** True/False values */
    BOOLEAN
}
