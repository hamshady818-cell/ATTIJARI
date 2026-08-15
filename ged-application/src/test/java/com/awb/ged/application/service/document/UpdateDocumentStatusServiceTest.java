package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.in.security.DocumentAccessValidator;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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

@ExtendWith(MockitoExtension.class)
class UpdateDocumentStatusServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private DocumentAccessValidator documentAccessValidator;

    @Mock
    private DocumentLockGuard documentLockGuard;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UpdateDocumentStatusService updateDocumentStatusService;

    @BeforeEach
    void setUp() {
        updateDocumentStatusService = new UpdateDocumentStatusService(
                documentRepositoryPort, documentMapper, documentAccessValidator, documentLockGuard);
    }

    @Test
    @DisplayName("Should successfully transition DRAFT -> PUBLISHED")
    void updateStatus_DraftToPublished_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document draftDoc = Document.builder()
                .id(docId)
                .name("DraftReport.pdf")
                .status(Document.DocumentStatus.DRAFT)
                .build();

        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId, userId);
        given(documentRepositoryPort.findByIdIncludingDeleted(docId)).willReturn(Optional.of(draftDoc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentStatusService.updateStatus(docId, "PUBLISHED", userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("Should successfully restore document TRASHED -> DRAFT")
    void updateStatus_TrashedToDraft_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document trashedDoc = Document.builder()
                .id(docId)
                .name("TrashedReport.pdf")
                .status(Document.DocumentStatus.TRASHED)
                .deletedAt(java.time.Instant.now())
                .deletedBy(userId)
                .build();

        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId, userId);
        given(documentRepositoryPort.findByIdIncludingDeleted(docId)).willReturn(Optional.of(trashedDoc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentStatusService.updateStatus(docId, "DRAFT", userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("Should throw BusinessException on invalid status transition (ARCHIVED -> PUBLISHED)")
    void updateStatus_InvalidTransition_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document archivedDoc = Document.builder()
                .id(docId)
                .name("ArchivedDoc.pdf")
                .status(Document.DocumentStatus.ARCHIVED)
                .build();

        willDoNothing().given(documentLockGuard).assertNotLockedByOther(docId, userId);
        given(documentRepositoryPort.findByIdIncludingDeleted(docId)).willReturn(Optional.of(archivedDoc));

        // When / Then
        assertThatThrownBy(() -> updateDocumentStatusService.updateStatus(docId, "PUBLISHED", userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("Should throw DOCUMENT_LOCKED when document is locked by another user")
    void updateStatus_DocumentLockedByOther_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        willThrow(new BusinessException(
                ErrorCode.DOCUMENT_LOCKED,
                "Ce document est verrouillé par un autre utilisateur et ne peut pas être modifié."
        )).given(documentLockGuard).assertNotLockedByOther(docId, userId);

        // When / Then
        assertThatThrownBy(() -> updateDocumentStatusService.updateStatus(docId, "PUBLISHED", userId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DOCUMENT_LOCKED)
                .hasMessageContaining("verrouillé par un autre utilisateur");
    }
}
