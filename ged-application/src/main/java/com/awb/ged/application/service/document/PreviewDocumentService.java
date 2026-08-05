package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.PreviewDocumentUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PreviewDocumentService implements PreviewDocumentUseCase {

    private static final Set<String> PREVIEWABLE_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/tiff",
            "text/plain",
            "text/html"
    );

    private final DocumentRepositoryPort documentRepositoryPort;
    private final StoragePort storagePort;

    @Autowired
    public PreviewDocumentService(DocumentRepositoryPort documentRepositoryPort, StoragePort storagePort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.storagePort = storagePort;
    }

    @Override
    public PreviewResult preview(UUID documentId) {
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        String mimeType = document.getMimeType() != null ? document.getMimeType().toLowerCase() : "application/octet-stream";
        if (!PREVIEWABLE_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(
                    ErrorCode.INVALID_DOCUMENT_FORMAT,
                    "Document format '" + mimeType + "' is not inline previewable. Please use download instead."
            );
        }

        if (document.getActiveVersionId() == null) {
            throw new NotFoundException(
                    ErrorCode.DOCUMENT_NOT_FOUND,
                    "No active version found for document ID " + documentId
            );
        }

        DocumentVersion version = documentRepositoryPort.findVersionById(document.getActiveVersionId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Active document version was not found."
                ));

        InputStream stream = storagePort.loadStream(version.getFileReferenceId());

        return new PreviewResult(stream, document.getName(), mimeType, version.getSizeBytes());
    }
}
