package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.PreviewDocumentUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.util.FileUtils;
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
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // docx
            "application/msword",                                                       // doc
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",      // xlsx
            "application/vnd.ms-excel",                                                 // xls
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
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public PreviewDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                  StoragePort storagePort,
                                  DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.storagePort = storagePort;
        this.documentAccessValidator = documentAccessValidator;
    }

    public PreviewDocumentService(DocumentRepositoryPort documentRepositoryPort, StoragePort storagePort) {
        this(documentRepositoryPort, storagePort, null);
    }

    @Override
    public PreviewResult preview(UUID documentId) {
        return preview(documentId, null);
    }

    @Override
    public PreviewResult preview(UUID documentId, UUID userId) {
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (documentAccessValidator != null) {
            documentAccessValidator.validateAccess(document, userId, "READ");
        }

        if (document.getActiveVersionId() == null) {
            throw new NotFoundException(
                    ErrorCode.DOCUMENT_NOT_FOUND,
                    "ERR-DOC-001: No active version found for document ID " + documentId
            );
        }

        DocumentVersion version = documentRepositoryPort.findVersionById(document.getActiveVersionId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Active document version was not found."
                ));

        // BUG 2 FIX: Résolution du mimeType par ordre de priorité :
        // 1. mimeType de la version (source de vérité du fichier physique)
        // 2. mimeType du document agrégat (dénormalisé)
        // 3. Détection par extension du nom de fichier (fallback offline)
        // 4. application/octet-stream (dernier recours)
        String mimeType = resolveEffectiveMimeType(version, document);

        if (!PREVIEWABLE_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(
                    ErrorCode.INVALID_DOCUMENT_FORMAT,
                    "Document format '" + mimeType + "' is not inline previewable. Please use download instead."
            );
        }

        InputStream stream = storagePort.loadStream(version.getFileReferenceId());
        return new PreviewResult(stream, document.getName(), mimeType, version.getSizeBytes());
    }

    /**
     * Résout le MIME type effectif en testant plusieurs sources par ordre de priorité.
     * Cela garantit que les anciens documents avec des données corrompues (octet-stream)
     * ont quand même une chance d'être prévisualisés grâce à la détection par extension.
     */
    private String resolveEffectiveMimeType(DocumentVersion version, Document document) {
        // 1. mimeType de la version
        if (version.getMimeType() != null
                && !version.getMimeType().isBlank()
                && !version.getMimeType().equals("application/octet-stream")) {
            return version.getMimeType().toLowerCase();
        }

        // 2. mimeType du document
        if (document.getMimeType() != null
                && !document.getMimeType().isBlank()
                && !document.getMimeType().equals("application/octet-stream")) {
            return document.getMimeType().toLowerCase();
        }

        // 3. Détection par extension du nom de fichier
        if (document.getName() != null && !document.getName().isBlank()) {
            String detected = FileUtils.detectMimeType(document.getName());
            if (!detected.equals("application/octet-stream")) {
                return detected;
            }
        }

        // 4. Dernier recours : retourner octet-stream (déclenchera l'erreur "not previewable")
        return "application/octet-stream";
    }
}
