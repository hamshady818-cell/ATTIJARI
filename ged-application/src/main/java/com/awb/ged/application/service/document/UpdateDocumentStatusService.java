package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.document.UpdateDocumentStatusUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UpdateDocumentStatusService implements UpdateDocumentStatusUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final DocumentMapper documentMapper;

    @Autowired
    public UpdateDocumentStatusService(DocumentRepositoryPort documentRepositoryPort,
                                       DocumentMapper documentMapper) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.documentMapper = documentMapper;
    }

    @Override
    public DocumentResponseDto updateStatus(UUID documentId, String newStatus, UUID currentUserId) {
        Document document = documentRepositoryPort.findByIdIncludingDeleted(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        Document.DocumentStatus targetStatus;
        try {
            targetStatus = Document.DocumentStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Invalid status value '" + newStatus + "'. Valid values: DRAFT, PUBLISHED, ARCHIVED, TRASHED"
            );
        }

        // Validate transition
        validateTransition(document.getStatus(), targetStatus);

        boolean isMovingToTrash = targetStatus == Document.DocumentStatus.TRASHED;

        Document updated = document.toBuilder()
                .status(targetStatus)
                .deletedAt(isMovingToTrash ? (document.getDeletedAt() != null ? document.getDeletedAt() : Instant.now()) : null)
                .deletedBy(isMovingToTrash ? (document.getDeletedBy() != null ? document.getDeletedBy() : currentUserId) : null)
                .updatedAt(Instant.now())
                .build();

        Document saved = documentRepositoryPort.save(updated);
        return documentMapper.toResponseDto(saved);
    }

    private void validateTransition(Document.DocumentStatus current, Document.DocumentStatus target) {
        if (current == target) return;

        boolean valid = switch (current) {
            case DRAFT -> target == Document.DocumentStatus.PUBLISHED || target == Document.DocumentStatus.TRASHED;
            case PUBLISHED -> target == Document.DocumentStatus.ARCHIVED || target == Document.DocumentStatus.TRASHED;
            case ARCHIVED -> target == Document.DocumentStatus.TRASHED;
            case TRASHED -> target == Document.DocumentStatus.DRAFT; // restore
        };

        if (!valid) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Invalid status transition from " + current + " to " + target
            );
        }
    }
}
