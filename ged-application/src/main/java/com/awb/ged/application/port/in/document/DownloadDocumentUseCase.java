package com.awb.ged.application.port.in.document;

import java.io.InputStream;
import java.util.UUID;

public interface DownloadDocumentUseCase {

    DownloadResult download(UUID documentId, UUID versionId);

    record DownloadResult(InputStream inputStream, String fileName, String mimeType, long sizeBytes) {}
}
