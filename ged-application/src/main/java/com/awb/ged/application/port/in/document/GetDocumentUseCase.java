package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;

import java.util.UUID;

public interface GetDocumentUseCase {

    DocumentResponseDto getDocumentById(UUID documentId);
}
