package com.awb.ged.domain.document.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>DocumentVersion</h1>
 * <p>
 * Domain entity representing a specific version of a document's file content.
 * It links to a logical file reference {@link FileReferenceId} instead of a physical path.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {

    /** Unique identifier for this version */
    private UUID id;

    /** Associated document identifier */
    private UUID documentId;

    /** Sequential version number (starts at 1) */
    private int versionNumber;

    /** SHA-256 checksum of the file content (for integrity and duplicate checking) */
    private String hash;

    /** Size of the file in bytes */
    private long sizeBytes;

    /** Logical file reference identifier (used by infrastructure to map physical file) */
    private FileReferenceId fileReferenceId;

    /** Identifier of the user who uploaded this version */
    private UUID uploadedBy;

    /** Upload timestamp in UTC */
    private Instant uploadedAt;
}
