package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private DocumentAccessValidator documentAccessValidator;

    @Mock
    private DocumentLockGuard documentLockGuard;

    private DeleteDocumentService deleteDocumentService;

    @BeforeEach
    void setUp() {
        deleteDocumentService = new DeleteDocumentService(
                documentRepositoryPort,
                eventPublisherPort,
                documentAccessValidator,
                documentLockGuard
        );
    }

    @Test
    @DisplayName("Should soft-delete document: set status to TRASHED, set deletedAt and deletedBy, preserve folderId")
    void deleteDocument_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc = Document.builder()
                .id(docId)
                .name("Contrat.pdf")
                .folderId(folderId)
                .status(Document.DocumentStatus.DRAFT)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId, userId);

        // When
        deleteDocumentService.deleteDocument(docId, userId);

        // Then
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepositoryPort).save(captor.capture());

        Document saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(Document.DocumentStatus.TRASHED);
        assertThat(saved.getDeletedAt()).isNotNull();
        assertThat(saved.getDeletedBy()).isEqualTo(userId);
        assertThat(saved.getFolderId()).isEqualTo(folderId);

        verify(eventPublisherPort).publish(ArgumentCaptor.forClass(DocumentDeletedEvent.class).capture());
    }

    @Test
    @DisplayName("Should throw DOCUMENT_LOCKED when document is locked by another user")
    void deleteDocument_LockedByOther_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc = Document.builder()
                .id(docId)
                .name("Contrat.pdf")
                .status(Document.DocumentStatus.DRAFT)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(doc));
        willThrow(new BusinessException(
                ErrorCode.DOCUMENT_LOCKED,
                "Ce document est verrouillé par un autre utilisateur et ne peut pas être modifié."
        )).given(documentLockGuard).assertNotLockedByOther(docId, userId);

        // When / Then
        assertThatThrownBy(() -> deleteDocumentService.deleteDocument(docId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("verrouillé par un autre utilisateur");

        verify(documentRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
