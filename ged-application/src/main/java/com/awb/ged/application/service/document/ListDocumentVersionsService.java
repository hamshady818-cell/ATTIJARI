package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.ListDocumentVersionsUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListDocumentVersionsService implements ListDocumentVersionsUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentMapper documentMapper;
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public ListDocumentVersionsService(DocumentRepositoryPort documentRepositoryPort,
                                       DocumentMapper documentMapper,
                                       DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentMapper = documentMapper;
        this.documentAccessValidator = documentAccessValidator;
    }

    public ListDocumentVersionsService(DocumentRepositoryPort documentRepositoryPort, DocumentMapper documentMapper) {
        this(documentRepositoryPort, documentMapper, null);
    }

    @Override
    public List<DocumentVersionResponseDto> listVersions(UUID documentId) {
        return listVersions(documentId, null);
    }

    @Override
    public List<DocumentVersionResponseDto> listVersions(UUID documentId, UUID userId) {
        // Verify document exists
        com.awb.ged.domain.document.model.Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (documentAccessValidator != null) {
            documentAccessValidator.validateAccess(document, userId, "READ");
        }

        return documentRepositoryPort.findVersionsByDocumentId(documentId)
                .stream()
                .map(documentMapper::toVersionResponseDto)
                .toList();
    }
}
