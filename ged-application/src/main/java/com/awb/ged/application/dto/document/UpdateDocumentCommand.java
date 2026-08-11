package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command for updating document properties or location.
 * All fields are optional — only provided fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand {

    /** New name/title for the document (optional) */
    private String newName;
    private String name;

    /** New description for the document (optional) */
    private String description;

    /** Category ID (optional) */
    private UUID categoryId;

    /** Department ID (optional) */
    private UUID departmentId;

    /** Owner / Responsable User ID (optional) */
    private UUID ownerId;

    /** Expiration date (optional) */
    private LocalDate expirationDate;

    /** List of tags (optional) */
    private List<String> tags;

    /** Dynamic metadata values (optional) */
    private List<DocumentMetadataValueDto> metadata;

    /** Target folder ID to move the document to (optional, null = root) */
    private UUID newFolderId;

    /** Flag to explicitly move to root (when newFolderId should become null) */
    private Boolean moveToRoot;

    public Boolean isMoveToRoot() {
        return Boolean.TRUE.equals(moveToRoot);
    }

    public String getNameToUpdate() {
        if (name != null) return name;
        if (newName != null) return newName;
        return null;
    }
}
