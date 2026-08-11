package com.awb.ged.application.service.document;

import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    private DeleteDocumentService deleteDocumentService;

    @BeforeEach
    void setUp() {
        deleteDocumentService = new DeleteDocumentService(documentRepositoryPort, eventPublisherPort);
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
}
