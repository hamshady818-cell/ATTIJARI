package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.ListDocumentVersionsUseCase;
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

    @Autowired
    public ListDocumentVersionsService(DocumentRepositoryPort documentRepositoryPort,
                                       DocumentMapper documentMapper) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentMapper = documentMapper;
    }

    @Override
    public List<DocumentVersionResponseDto> listVersions(UUID documentId) {
        // Verify document exists
        documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        return documentRepositoryPort.findVersionsByDocumentId(documentId)
                .stream()
                .map(documentMapper::toVersionResponseDto)
                .toList();
    }
}
