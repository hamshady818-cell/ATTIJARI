package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentLockResponseDto;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckoutDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private CheckoutDocumentService checkoutDocumentService;

    @BeforeEach
    void setUp() {
        checkoutDocumentService = new CheckoutDocumentService(documentRepositoryPort, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully checkout document when not already locked")
    void checkout_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document document = Document.builder().id(docId).name("Doc.pdf").build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(document));
        given(documentRepositoryPort.findActiveCheckout(docId)).willReturn(Optional.empty());

        Instant now = Instant.now();
        DocumentRepositoryPort.CheckoutInfo info = new DocumentRepositoryPort.CheckoutInfo(userId, now, now.plusSeconds(3600));
        given(documentRepositoryPort.findActiveCheckout(docId))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(info));

        // When
        DocumentLockResponseDto result = checkoutDocumentService.checkout(docId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isLocked()).isTrue();
        assertThat(result.getLockedBy()).isEqualTo(userId);
        verify(documentRepositoryPort).saveCheckout(docId, userId);
    }

    @Test
    @DisplayName("Should throw BusinessException when already checked out")
    void checkout_AlreadyLocked_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(Document.builder().id(docId).build()));
        given(documentRepositoryPort.findActiveCheckout(docId))
                .willReturn(Optional.of(new DocumentRepositoryPort.CheckoutInfo(otherUser, Instant.now(), null)));

        // When / Then
        assertThatThrownBy(() -> checkoutDocumentService.checkout(docId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("checked out");
    }
}
