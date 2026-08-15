package com.awb.ged.application.service.document;

import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * <h1>DocumentLockGuard</h1>
 * <p>
 * Component responsible for enforcing lock/checkout validation rules
 * before document modification operations.
 * </p>
 */
@Component
public class DocumentLockGuard {

    private final DocumentRepositoryPort documentRepositoryPort;

    @Autowired
    public DocumentLockGuard(DocumentRepositoryPort documentRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
    }

    /**
     * Asserts that the document is not checked out by another user.
     * <ul>
     *   <li>If no active checkout exists, execution continues normally.</li>
     *   <li>If an active checkout exists by the {@code currentUserId}, execution continues normally.</li>
     *   <li>If an active checkout exists by another user, throws {@link BusinessException} with {@link ErrorCode#DOCUMENT_LOCKED}.</li>
     * </ul>
     *
     * @param documentId the ID of the document to check
     * @param currentUserId the ID of the current user attempting the action
     */
    public void assertNotLockedByOther(UUID documentId, UUID currentUserId) {
        documentRepositoryPort.findActiveCheckout(documentId)
                .ifPresent(checkout -> {
                    if (currentUserId == null || !Objects.equals(checkout.checkedOutBy(), currentUserId)) {
                        throw new BusinessException(
                                ErrorCode.DOCUMENT_LOCKED,
                                "Ce document est verrouillé par un autre utilisateur et ne peut pas être modifié."
                        );
                    }
                });
    }
}
