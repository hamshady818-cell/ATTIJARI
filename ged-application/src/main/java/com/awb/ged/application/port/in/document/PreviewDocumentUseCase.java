package com.awb.ged.application.port.in.document;

import java.io.InputStream;
import java.util.UUID;

public interface PreviewDocumentUseCase {

    PreviewResult preview(UUID documentId);

    record PreviewResult(InputStream inputStream, String fileName, String mimeType, long sizeBytes) {}
}
