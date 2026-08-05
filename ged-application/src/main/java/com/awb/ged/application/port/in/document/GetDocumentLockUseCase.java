package com.awb.ged.application.port.in.document;

import com.awb.ged.application.dto.document.DocumentLockResponseDto;

import java.util.UUID;

public interface GetDocumentLockUseCase {

    DocumentLockResponseDto getLockStatus(UUID documentId);
}
