package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.common.exception.BusinessException;
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

@ExtendWith(MockitoExtension.class)
class UpdateDocumentStatusServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UpdateDocumentStatusService updateDocumentStatusService;

    @BeforeEach
    void setUp() {
        updateDocumentStatusService = new UpdateDocumentStatusService(documentRepositoryPort, documentMapper);
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

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(draftDoc));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentResponseDto result = updateDocumentStatusService.updateStatus(docId, "PUBLISHED", userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
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

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(archivedDoc));

        // When / Then
        assertThatThrownBy(() -> updateDocumentStatusService.updateStatus(docId, "PUBLISHED", userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
