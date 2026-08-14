package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.application.dto.document.UploadNewVersionCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.BusinessException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentLock;
import com.awb.ged.domain.document.model.DocumentVersion;
import com.awb.ged.domain.document.model.FileReferenceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadNewVersionServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private StoragePort storagePort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UploadNewVersionService uploadNewVersionService;

    @BeforeEach
    void setUp() {
        uploadNewVersionService = new UploadNewVersionService(
                documentRepositoryPort,
                storagePort,
                documentMapper
        );
    }

    @Test
    @DisplayName("Should successfully upload a new version for an unlocked document")
    void uploadNewVersion_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Document existingDoc = Document.builder()
                .id(docId)
                .name("Invoice.pdf")
                .mimeType("application/pdf")
                .build();

        UploadNewVersionCommand command = UploadNewVersionCommand.builder()
                .documentId(docId)
                .fileContent("New File Bytes".getBytes())
                .mimeType("application/pdf")
                .uploadedBy(userId)
                .changeSummary("Corrected typo")
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(existingDoc));
        given(documentRepositoryPort.countVersionsByDocumentId(docId)).willReturn(1);
        given(storagePort.store(anyString(), any(), anyString())).willReturn(new FileReferenceId("ged-documents/path/v2"));
        given(documentRepositoryPort.saveVersion(any(DocumentVersion.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        DocumentVersionResponseDto result = uploadNewVersionService.uploadNewVersion(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersionNumber()).isEqualTo(2);
        verify(documentRepositoryPort).saveVersion(any(DocumentVersion.class));
        verify(documentRepositoryPort).save(any(Document.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when document is currently locked")
    void uploadNewVersion_Locked_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        Instant now = Instant.now();
        Document lockedDoc = Document.builder()
                .id(docId)
                .lock(DocumentLock.builder().lockedBy(UUID.randomUUID()).lockedAt(now).expiration(now.plusSeconds(300)).build())
                .build();

        UploadNewVersionCommand command = UploadNewVersionCommand.builder()
                .documentId(docId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(lockedDoc));

        // When / Then
        assertThatThrownBy(() -> uploadNewVersionService.uploadNewVersion(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("checked out");
    }

    @Test
    @DisplayName("Should throw NotFoundException when document does not exist")
    void uploadNewVersion_NotFound_ThrowsException() {
        // Given
        UUID docId = UUID.randomUUID();
        UploadNewVersionCommand command = UploadNewVersionCommand.builder().documentId(docId).build();
        given(documentRepositoryPort.findById(docId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> uploadNewVersionService.uploadNewVersion(command))
                .isInstanceOf(NotFoundException.class);
    }
}
