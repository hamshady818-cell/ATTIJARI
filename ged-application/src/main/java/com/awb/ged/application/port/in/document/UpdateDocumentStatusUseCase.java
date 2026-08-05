package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;

import java.util.UUID;

public interface UpdateDocumentStatusUseCase {

    DocumentResponseDto updateStatus(UUID documentId, String newStatus, UUID currentUserId);
}
