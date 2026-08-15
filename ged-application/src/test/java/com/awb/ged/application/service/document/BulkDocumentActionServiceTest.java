package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.BulkActionResultDto;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.model.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BulkDocumentActionServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    @Mock
    private DocumentAccessValidator documentAccessValidator;

    @Mock
    private DocumentLockGuard documentLockGuard;

    private BulkDocumentActionService bulkDocumentActionService;

    @BeforeEach
    void setUp() {
        bulkDocumentActionService = new BulkDocumentActionService(
                documentRepositoryPort,
                folderRepositoryPort,
                eventPublisherPort,
                documentAccessValidator,
                documentLockGuard
        );
    }

    @Test
    @DisplayName("Should successfully bulk delete 2 unlocked documents")
    void bulkDelete_Success() {
        // Given
        UUID docId1 = UUID.randomUUID();
        UUID docId2 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc1 = Document.builder().id(docId1).name("Doc1.pdf").build();
        Document doc2 = Document.builder().id(docId2).name("Doc2.pdf").build();

        given(documentRepositoryPort.findById(docId1)).willReturn(Optional.of(doc1));
        given(documentRepositoryPort.findById(docId2)).willReturn(Optional.of(doc2));
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(any(), any());

        // When
        BulkActionResultDto result = bulkDocumentActionService.bulkDelete(List.of(docId1, docId2), userId);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSkippedIds()).isEmpty();
        assertThat(result.getSkippedNames()).isEmpty();
        verify(documentRepositoryPort, times(2)).save(any(Document.class));
        verify(eventPublisherPort, times(2)).publish(any());
    }

    @Test
    @DisplayName("Should skip locked document but process the others")
    void bulkDelete_LockedDocument_SkippedAndOthersProcessed() {
        // Given
        UUID docId1 = UUID.randomUUID();
        UUID docId2Locked = UUID.randomUUID();
        UUID docId3 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc1 = Document.builder().id(docId1).name("Doc1.pdf").build();
        Document doc2Locked = Document.builder().id(docId2Locked).name("Doc2Locked.pdf").build();
        Document doc3 = Document.builder().id(docId3).name("Doc3.pdf").build();

        given(documentRepositoryPort.findById(docId1)).willReturn(Optional.of(doc1));
        given(documentRepositoryPort.findById(docId2Locked)).willReturn(Optional.of(doc2Locked));
        given(documentRepositoryPort.findById(docId3)).willReturn(Optional.of(doc3));

        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId1, userId);
        willThrow(new BusinessException(
                ErrorCode.DOCUMENT_LOCKED,
                "Document is locked"
        )).given(documentLockGuard).assertNotLockedByOther(docId2Locked, userId);
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId3, userId);

        // When
        BulkActionResultDto result = bulkDocumentActionService.bulkDelete(List.of(docId1, docId2Locked, docId3), userId);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSkippedIds()).containsExactly(docId2Locked);
        assertThat(result.getSkippedNames()).containsExactly("Doc2Locked.pdf");
        verify(documentRepositoryPort, times(2)).save(any(Document.class));
        verify(eventPublisherPort, times(2)).publish(any());
    }

    @Test
    @DisplayName("Should successfully bulk move documents")
    void bulkMove_Success() {
        // Given
        UUID docId1 = UUID.randomUUID();
        UUID targetFolderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc1 = Document.builder().id(docId1).name("Doc1.pdf").build();
        Folder targetFolder = Folder.builder().id(targetFolderId).name("FolderA").build();

        given(folderRepositoryPort.findById(targetFolderId)).willReturn(Optional.of(targetFolder));
        given(documentRepositoryPort.findById(docId1)).willReturn(Optional.of(doc1));
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(any(), any());

        // When
        BulkActionResultDto result = bulkDocumentActionService.bulkMove(List.of(docId1), targetFolderId, false, userId);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(1);
        assertThat(result.getSkippedIds()).isEmpty();
        assertThat(result.getSkippedNames()).isEmpty();
    }

    @Test
    @DisplayName("Should successfully bulk tag documents")
    void bulkTag_Success() {
        // Given
        UUID docId1 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Document doc1 = Document.builder().id(docId1).name("Doc1.pdf").build();

        given(documentRepositoryPort.findById(docId1)).willReturn(Optional.of(doc1));
        willDoNothing().given(documentLockGuard).assertNotLockedByOther(any(), any());

        // When
        BulkActionResultDto result = bulkDocumentActionService.bulkTag(List.of(docId1), List.of("urgent", "finance"), userId);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(1);
        assertThat(result.getSkippedIds()).isEmpty();
        assertThat(result.getSkippedNames()).isEmpty();
        verify(documentRepositoryPort, times(1)).addTagToDocument(docId1, "urgent");
        verify(documentRepositoryPort, times(1)).addTagToDocument(docId1, "finance");
    }
}
