package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.CheckinDocumentUseCase;
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
public class CheckinDocumentService implements CheckinDocumentUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;

    @Autowired
    public CheckinDocumentService(DocumentRepositoryPort documentRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
    }

    @Override
    public void checkin(UUID documentId, UUID userId) {
        // 1. Verify document exists
        documentRepositoryPort.findById(documentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.DOCUMENT_NOT_FOUND,
                        "Document with ID " + documentId + " was not found."
                ));

        // 2. Verify active checkout exists
        DocumentRepositoryPort.CheckoutInfo checkout = documentRepositoryPort
                .findActiveCheckout(documentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Document is not currently checked out."
                ));

        // 3. Only the lock holder can check in (or admins — checked at controller level)
        if (!checkout.checkedOutBy().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Only the user who checked out the document can check it back in."
            );
        }

        // 4. Release the lock
        documentRepositoryPort.checkin(documentId, userId);
    }
}
