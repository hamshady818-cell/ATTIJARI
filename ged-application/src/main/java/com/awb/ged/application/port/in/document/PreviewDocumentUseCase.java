package com.awb.ged.application.port.in.document;

import java.io.InputStream;
import java.util.UUID;

public interface PreviewDocumentUseCase {

    PreviewResult preview(UUID documentId);

    default PreviewResult preview(UUID documentId, UUID userId) {
        return preview(documentId);
    }

    record PreviewResult(InputStream inputStream, String fileName, String mimeType, long sizeBytes) {}
}
