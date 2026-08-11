package com.awb.ged.application.service.document;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentLock;
import com.awb.ged.domain.document.model.DocumentVersion;
import com.awb.ged.domain.document.model.FileReferenceId;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadDocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private StoragePort storagePort;

    private final DocumentMapper documentMapper = Mappers.getMapper(DocumentMapper.class);

    private UploadDocumentService uploadDocumentService;

    @BeforeEach
    void setUp() {
        uploadDocumentService = new UploadDocumentService(
                documentRepositoryPort,
                folderRepositoryPort,
                userRepositoryPort,
                storagePort,
                documentMapper
        );
    }

    @Test
    @DisplayName("Should successfully upload a document when command is valid")
    void uploadDocument_Success() {
        // Given
        UUID ownerId = UUID.randomUUID();
        byte[] fileContent = "PDF content sample".getBytes();
        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Contract2024.pdf")
                .ownerId(ownerId)
                .mimeType("application/pdf")
                .fileContent(fileContent)
                .build();

        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of());
        given(userRepositoryPort.findById(ownerId)).willReturn(Optional.of(User.builder().id(ownerId).build()));
        given(storagePort.store(anyString(), any(byte[].class), anyString()))
                .willReturn(new FileReferenceId("storage/ref/123"));
        given(documentRepositoryPort.save(any(Document.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        DocumentResponseDto result = uploadDocumentService.uploadDocument(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Contract2024.pdf");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        verify(documentRepositoryPort, times(2)).save(any(Document.class));
        verify(documentRepositoryPort).saveVersion(any(DocumentVersion.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when document name already exists in same folder")
    void uploadDocument_DuplicateName_ThrowsConflictException() {
        // Given
        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Report.pdf")
                .build();

        Document existingDoc = Document.builder().id(UUID.randomUUID()).name("Report.pdf").build();
        given(documentRepositoryPort.findByFolderId(null)).willReturn(List.of(existingDoc));

        // When / Then
        assertThatThrownBy(() -> uploadDocumentService.uploadDocument(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Report.pdf");
    }

    @Test
    @DisplayName("Should throw NotFoundException when target folder ID does not exist")
    void uploadDocument_FolderNotFound_ThrowsNotFoundException() {
        // Given
        UUID folderId = UUID.randomUUID();
        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Report.pdf")
                .folderId(folderId)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> uploadDocumentService.uploadDocument(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(folderId.toString());
    }

    @Test
    @DisplayName("Should verify DocumentLock expiration logic on domain model")
    void documentLock_ExpirationLogicTest() {
        // Given
        Instant now = Instant.now();
        DocumentLock activeLock = DocumentLock.builder()
                .lockedBy(UUID.randomUUID())
                .lockedAt(now.minusSeconds(60))
                .expiration(now.plusSeconds(300))
                .build();

        Document lockedDoc = Document.builder()
                .id(UUID.randomUUID())
                .name("LockedDoc.pdf")
                .lock(activeLock)
                .build();

        // When / Then
        assertThat(lockedDoc.isCurrentlyLocked(now)).isTrue();
        assertThat(lockedDoc.isCurrentlyLocked(now.plusSeconds(600))).isFalse(); // Expired after 10 min
    }
}
