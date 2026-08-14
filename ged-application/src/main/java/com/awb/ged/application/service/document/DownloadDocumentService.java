package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.DownloadDocumentUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DownloadDocumentService implements DownloadDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final StoragePort storagePort;
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public DownloadDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                   StoragePort storagePort,
                                   DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.storagePort = storagePort;
        this.documentAccessValidator = documentAccessValidator;
    }

    public DownloadDocumentService(DocumentRepositoryPort documentRepositoryPort, StoragePort storagePort) {
        this(documentRepositoryPort, storagePort, null);
    }

    @Override
    public DownloadResult download(UUID documentId, UUID versionId) {
        return download(documentId, versionId, null);
    }

    @Override
    public DownloadResult download(UUID documentId, UUID versionId, UUID userId) {
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (documentAccessValidator != null) {
            documentAccessValidator.validateAccess(document, userId, "READ");
        }

        UUID targetVersionId = versionId != null ? versionId : document.getActiveVersionId();
        if (targetVersionId == null) {
            throw new NotFoundException(
                    ErrorCode.DOCUMENT_NOT_FOUND,
                    "No active version found for document ID " + documentId
            );
        }

        DocumentVersion version = documentRepositoryPort.findVersionById(targetVersionId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document version with ID " + targetVersionId + " was not found."
                ));

        InputStream stream = storagePort.loadStream(version.getFileReferenceId());
        String fileName = document.getName();
        String mimeType = document.getMimeType() != null ? document.getMimeType() : "application/octet-stream";

        return new DownloadResult(stream, fileName, mimeType, version.getSizeBytes());
    }
}
