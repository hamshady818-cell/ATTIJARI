package com.awb.ged.domain.document.model;

import lombok.Value;

/**
 * <h1>FileReferenceId</h1>
 * <p>
 * Value Object representing a logical identifier pointing to a file's physical content.
 * Isolates the domain model from infrastructure details (like local file paths, MinIO keys, or S3 URIs).
 * The mapping between this logical reference and the actual location is handled in the infrastructure layer.
 * </p>
 */
@Value
public class FileReferenceId {

    /** The logical reference value (typically a UUID or structured key) */
    String value;
}
