package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command for renaming or moving a document.
 * Both fields are optional — only provided fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand {

    /** New name for the document (optional) */
    private String newName;

    /** New description for the document (optional) */
    private String description;

    /** Target folder ID to move the document to (optional, null = root) */
    private UUID newFolderId;

    /** Flag to explicitly move to root (when newFolderId should become null) */
    private boolean moveToRoot;
}
