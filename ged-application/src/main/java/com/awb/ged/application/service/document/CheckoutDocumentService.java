package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentLockResponseDto;
import com.awb.ged.application.port.in.document.CheckoutDocumentUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CheckoutDocumentService implements CheckoutDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;

    @Autowired
    public CheckoutDocumentService(DocumentRepositoryPort documentRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
    }

    @Override
    public DocumentLockResponseDto checkout(UUID documentId, UUID userId) {
        // 1. Verify document exists
        documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

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

        return DocumentLockResponseDto.builder()
                .documentId(documentId)
                .locked(true)
                .lockedBy(info.checkedOutBy())
                .lockedAt(info.checkedOutAt())
                .lockExpiration(info.expectedReturnAt())
                .build();
    }
}
