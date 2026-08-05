package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command for uploading a new file version to an existing document.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadNewVersionCommand {

    /** ID of the document to add a new version to */
    private UUID documentId;

    /** Binary content of the new file */
    private byte[] fileContent;

    /** MIME type of the new file */
    private String mimeType;

    /** ID of the user uploading this version */
    private UUID uploadedBy;

    /** Optional human-readable change summary */
    private String changeSummary;
}
