package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Command for bulk document operations (delete, move, tag).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDocumentActionCommand {

    /** IDs of the documents to act on */
    private List<UUID> documentIds;

    /** For MOVE: target folder ID (null = root) */
    private UUID targetFolderId;

    /** For MOVE to root explicitly */
    private boolean moveToRoot;

    /** For TAG: tag names to apply */
    private List<String> tagNames;

    /** The user performing the action */
    private UUID performedBy;
}
