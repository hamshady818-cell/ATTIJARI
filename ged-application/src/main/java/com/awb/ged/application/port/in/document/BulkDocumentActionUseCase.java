package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.BulkActionResultDto;
import com.awb.ged.application.dto.document.BulkDocumentActionCommand;

import java.util.List;
import java.util.UUID;

public interface BulkDocumentActionUseCase {

    BulkActionResultDto bulkDelete(List<UUID> documentIds, UUID performedBy);

    BulkActionResultDto bulkMove(List<UUID> documentIds, UUID targetFolderId, boolean moveToRoot, UUID performedBy);

    BulkActionResultDto bulkTag(List<UUID> documentIds, List<String> tagNames, UUID performedBy);
}
