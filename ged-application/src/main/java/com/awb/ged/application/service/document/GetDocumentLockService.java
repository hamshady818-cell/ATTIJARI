package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentLockResponseDto;
import com.awb.ged.application.port.in.document.GetDocumentLockUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetDocumentLockService implements GetDocumentLockUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;

    @Autowired
    public GetDocumentLockService(DocumentRepositoryPort documentRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
    }

    @Override
    public DocumentLockResponseDto getLockStatus(UUID documentId) {
        documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        Optional<DocumentRepositoryPort.CheckoutInfo> checkout =
                documentRepositoryPort.findActiveCheckout(documentId);

        if (checkout.isEmpty()) {
            return DocumentLockResponseDto.builder()
                    .documentId(documentId)
                    .locked(false)
                    .build();
        }

        DocumentRepositoryPort.CheckoutInfo info = checkout.get();
        return DocumentLockResponseDto.builder()
                .documentId(documentId)
                .locked(true)
                .lockedBy(info.checkedOutBy())
                .lockedAt(info.checkedOutAt())
                .lockExpiration(info.expectedReturnAt())
                .build();
    }
}
