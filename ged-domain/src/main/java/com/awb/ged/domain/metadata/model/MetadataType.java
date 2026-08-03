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

    /** Date and time */
    DATETIME,

    /** True/False values */
    BOOLEAN,

    /** Single choice from predefined options */
    SELECT,

    /** Multiple choices from predefined options */
    MULTI_SELECT,

    /** Web URL string */
    URL
}
