package com.awb.ged.application.service.document;

import com.awb.ged.application.port.in.document.UpdateDocumentStatusUseCase;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.domain.document.event.DocumentExpiredEvent;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpireDocumentsServiceTest {

    @Mock
    private DocumentRepositoryPort      documentRepositoryPort;

    @Mock
    private UpdateDocumentStatusUseCase updateDocumentStatusUseCase;

    @Mock
    private EventPublisherPort          eventPublisherPort;

    private ExpireDocumentsService expireDocumentsService;

    @BeforeEach
    void setUp() {
        expireDocumentsService = new ExpireDocumentsService(
                documentRepositoryPort,
                updateDocumentStatusUseCase,
                eventPublisherPort
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cas nominal : 2 documents expirés
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should archive 2 documents and publish 2 events when 2 expired documents are found")
    void expireOverdueDocuments_TwoExpiredDocuments_ArchivesBothAndPublishesEvents() {
        // Given
        UUID docId1  = UUID.randomUUID();
        UUID docId2  = UUID.randomUUID();
        UUID ownerId1 = UUID.randomUUID();
        UUID ownerId2 = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Document doc1 = Document.builder()
                .id(docId1)
                .name("ContractA.pdf")
                .ownerId(ownerId1)
                .status(Document.DocumentStatus.PUBLISHED)
                .expirationDate(yesterday)
                .build();

        Document doc2 = Document.builder()
                .id(docId2)
                .name("ReportB.docx")
                .ownerId(ownerId2)
                .status(Document.DocumentStatus.DRAFT)
                .expirationDate(yesterday)
                .build();

        given(documentRepositoryPort.findExpiredActiveDocuments(any(LocalDate.class)))
                .willReturn(List.of(doc1, doc2));

        // When
        int result = expireDocumentsService.expireOverdueDocuments();

        // Then — nombre de documents traités
        assertThat(result).isEqualTo(2);

        // Then — updateStatus appelé exactement 2 fois, une fois par document
        verify(updateDocumentStatusUseCase, times(1))
                .updateStatus(docId1, "ARCHIVED", null);
        verify(updateDocumentStatusUseCase, times(1))
                .updateStatus(docId2, "ARCHIVED", null);

        // Then — publish appelé exactement 2 fois avec les bons champs
        ArgumentCaptor<DocumentExpiredEvent> eventCaptor =
                ArgumentCaptor.forClass(DocumentExpiredEvent.class);
        verify(eventPublisherPort, times(2)).publish(eventCaptor.capture());

        List<DocumentExpiredEvent> publishedEvents = eventCaptor.getAllValues();

        assertThat(publishedEvents)
                .extracting(DocumentExpiredEvent::getDocumentId)
                .containsExactlyInAnyOrder(docId1, docId2);

        assertThat(publishedEvents)
                .extracting(DocumentExpiredEvent::getDocumentName)
                .containsExactlyInAnyOrder("ContractA.pdf", "ReportB.docx");

        assertThat(publishedEvents)
                .extracting(DocumentExpiredEvent::getOwnerId)
                .containsExactlyInAnyOrder(ownerId1, ownerId2);

        assertThat(publishedEvents)
                .extracting(DocumentExpiredEvent::getExpirationDate)
                .containsOnly(yesterday);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cas vide : aucun document expiré
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return 0 and never call use case or publisher when no expired documents are found")
    void expireOverdueDocuments_NoExpiredDocuments_DoesNothing() {
        // Given
        given(documentRepositoryPort.findExpiredActiveDocuments(any(LocalDate.class)))
                .willReturn(List.of());

        // When
        int result = expireDocumentsService.expireOverdueDocuments();

        // Then
        assertThat(result).isEqualTo(0);

        verify(updateDocumentStatusUseCase, never()).updateStatus(any(), any(), any());
        verify(eventPublisherPort,          never()).publish(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cas de verrouillage : document verrouillé ignoré
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should skip locked document but archive the others and not propagate the exception")
    void expireOverdueDocuments_LockedDocument_SkippedAndOthersArchived() {
        // Given
        UUID docId1 = UUID.randomUUID();
        UUID docId2Locked = UUID.randomUUID();
        UUID docId3 = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Document doc1 = Document.builder().id(docId1).name("Doc1.pdf").expirationDate(yesterday).build();
        Document doc2Locked = Document.builder().id(docId2Locked).name("Doc2.pdf").expirationDate(yesterday).build();
        Document doc3 = Document.builder().id(docId3).name("Doc3.pdf").expirationDate(yesterday).build();

        given(documentRepositoryPort.findExpiredActiveDocuments(any(LocalDate.class)))
                .willReturn(List.of(doc1, doc2Locked, doc3));

        lenient().doThrow(new BusinessException(
                ErrorCode.DOCUMENT_LOCKED,
                "Document is locked"
        )).when(updateDocumentStatusUseCase).updateStatus(docId2Locked, "ARCHIVED", null);

        // When
        int result = assertDoesNotThrow(() -> expireDocumentsService.expireOverdueDocuments());

        // Then
        assertThat(result).isEqualTo(2);

        verify(eventPublisherPort, times(2)).publish(any());
        verify(updateDocumentStatusUseCase, times(1)).updateStatus(docId1, "ARCHIVED", null);
        verify(updateDocumentStatusUseCase, times(1)).updateStatus(docId2Locked, "ARCHIVED", null);
        verify(updateDocumentStatusUseCase, times(1)).updateStatus(docId3, "ARCHIVED", null);
    }

    @Test
    @DisplayName("Should propagate non-lock BusinessException")
    void expireOverdueDocuments_NonLockException_Propagated() {
        // Given
        UUID docId = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Document doc = Document.builder().id(docId).name("Doc.pdf").expirationDate(yesterday).build();

        given(documentRepositoryPort.findExpiredActiveDocuments(any(LocalDate.class)))
                .willReturn(List.of(doc));

        willThrow(new BusinessException(
                ErrorCode.DOCUMENT_NOT_FOUND,
                "Document not found"
        )).given(updateDocumentStatusUseCase).updateStatus(docId, "ARCHIVED", null);

        // When / Then
        assertThatThrownBy(() -> expireDocumentsService.expireOverdueDocuments())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_NOT_FOUND);

        verify(eventPublisherPort, never()).publish(any());
    }
}
