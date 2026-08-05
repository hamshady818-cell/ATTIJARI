package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;

import java.util.List;
import java.util.UUID;

public interface ListDocumentVersionsUseCase {

    List<DocumentVersionResponseDto> listVersions(UUID documentId);
}
