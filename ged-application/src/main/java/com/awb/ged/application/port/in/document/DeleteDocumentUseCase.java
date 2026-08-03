package com.awb.ged.application.port.in.document;

import java.util.UUID;

public interface DeleteDocumentUseCase {

    void deleteDocument(UUID documentId, UUID deletedByUserId);
}