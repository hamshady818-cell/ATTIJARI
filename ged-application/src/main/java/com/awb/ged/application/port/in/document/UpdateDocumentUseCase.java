package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UpdateDocumentCommand;

import java.util.UUID;

public interface UpdateDocumentUseCase {

    DocumentResponseDto updateDocument(UUID documentId, UpdateDocumentCommand command, UUID currentUserId);
}
