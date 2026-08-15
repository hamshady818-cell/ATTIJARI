package com.awb.ged.application.service.document;

import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DocumentLockGuardTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    private DocumentLockGuard lockGuard;

    private final UUID docId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lockGuard = new DocumentLockGuard(documentRepositoryPort);
    }

    @Test
    @DisplayName("Should pass when no active checkout exists")
    void shouldPassWhenNotLocked() {
        given(documentRepositoryPort.findActiveCheckout(docId)).willReturn(Optional.empty());

        assertDoesNotThrow(() -> lockGuard.assertNotLockedByOther(docId, userId));
    }

    @Test
    @DisplayName("Should pass when checked out by current user")
    void shouldPassWhenLockedByCurrentUser() {
        DocumentRepositoryPort.CheckoutInfo checkout = new DocumentRepositoryPort.CheckoutInfo(
                userId, Instant.now(), Instant.now().plusSeconds(3600)
        );
        given(documentRepositoryPort.findActiveCheckout(docId)).willReturn(Optional.of(checkout));

        assertDoesNotThrow(() -> lockGuard.assertNotLockedByOther(docId, userId));
    }

    @Test
    @DisplayName("Should throw BusinessException when checked out by another user")
    void shouldThrowWhenLockedByOtherUser() {
        DocumentRepositoryPort.CheckoutInfo checkout = new DocumentRepositoryPort.CheckoutInfo(
                otherUserId, Instant.now(), Instant.now().plusSeconds(3600)
        );
        given(documentRepositoryPort.findActiveCheckout(docId)).willReturn(Optional.of(checkout));

        assertThatThrownBy(() -> lockGuard.assertNotLockedByOther(docId, userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_LOCKED)
                .hasMessage("Ce document est verrouillé par un autre utilisateur et ne peut pas être modifié.");
    }
}
