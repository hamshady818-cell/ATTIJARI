package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentLockResponseDto;
import com.awb.ged.application.port.in.document.CheckoutDocumentUseCase;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.event.DocumentCheckedOutEvent;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CheckoutDocumentService implements CheckoutDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;
    private final DocumentAccessValidator documentAccessValidator;

    @Autowired
    public CheckoutDocumentService(DocumentRepositoryPort documentRepositoryPort,
                                   ApplicationEventPublisher eventPublisher,
                                   DocumentAccessValidator documentAccessValidator) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.eventPublisher = eventPublisher;
        this.documentAccessValidator = documentAccessValidator;
    }

    public CheckoutDocumentService(DocumentRepositoryPort documentRepositoryPort, ApplicationEventPublisher eventPublisher) {
        this(documentRepositoryPort, eventPublisher, null);
    }

    @Override
    public DocumentLockResponseDto checkout(UUID documentId, UUID userId) {
        // 1. Verify document exists
        Document document = documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        if (documentAccessValidator != null) {
            documentAccessValidator.validateAccess(document, userId, "WRITE");
        }

        // 2. Verify no active checkout already exists
        documentRepositoryPort.findActiveCheckout(documentId)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            ErrorCode.DOCUMENT_LOCKED,
                            "Document is already checked out by user " + existing.checkedOutBy()
                    );
                });

        // 3. Create checkout record
        documentRepositoryPort.saveCheckout(documentId, userId);

        // 4. Return lock info
        DocumentRepositoryPort.CheckoutInfo info = documentRepositoryPort
                .findActiveCheckout(documentId)
                .orElseThrow(() -> new IllegalStateException("Checkout was not persisted"));

        // 5. Publish domain event — notifies the document owner if they are not the one checking out
        eventPublisher.publishEvent(new DocumentCheckedOutEvent(
                documentId,
                document.getName(),
                userId,
                document.getOwnerId()
        ));

        return DocumentLockResponseDto.builder()
                .documentId(documentId)
                .locked(true)
                .lockedBy(info.checkedOutBy())
                .lockedAt(info.checkedOutAt())
                .lockExpiration(info.expectedReturnAt())
                .build();
    }
}
