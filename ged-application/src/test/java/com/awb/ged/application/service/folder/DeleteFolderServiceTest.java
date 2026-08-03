package com.awb.ged.application.service.folder;

import com.awb.ged.application.port.out.event.EventPublisherPort;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.event.DomainEvent;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.NotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteFolderServiceTest {

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    private DeleteFolderService deleteFolderService;

    @BeforeEach
    void setUp() {
        deleteFolderService = new DeleteFolderService(folderRepositoryPort, documentRepositoryPort, eventPublisherPort);
    }

    @Test
    @DisplayName("Should successfully soft-delete a folder when it exists and is empty")
    void deleteFolder_Success() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Folder folder = Folder.builder()
                .id(folderId)
                .name("Temp")
                .parentId(null)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(folder));
        given(folderRepositoryPort.findByParentId(folderId)).willReturn(List.of());
        given(documentRepositoryPort.findByFolderId(folderId)).willReturn(List.of());
        given(folderRepositoryPort.save(any(Folder.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        deleteFolderService.deleteFolder(folderId, userId);

        // Then
        assertThat(folder.getDeletedAt()).isNotNull();
        assertThat(folder.getDeletedBy()).isEqualTo(userId);
        verify(folderRepositoryPort).save(folder);
        verify(eventPublisherPort).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when folder does not exist")
    void deleteFolder_NotFound_ThrowsNotFoundException() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> deleteFolderService.deleteFolder(folderId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(folderId.toString());
        verify(folderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when folder has active subfolders")
    void deleteFolder_NotEmptySubfolders_ThrowsConflictException() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Folder folder = Folder.builder().id(folderId).name("Parent").build();
        Folder subfolder = Folder.builder().id(UUID.randomUUID()).name("Sub").build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(folder));
        given(folderRepositoryPort.findByParentId(folderId)).willReturn(List.of(subfolder));

        // When / Then
        assertThatThrownBy(() -> deleteFolderService.deleteFolder(folderId, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active subfolders");
        verify(folderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when folder has active documents")
    void deleteFolder_NotEmptyDocuments_ThrowsConflictException() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Folder folder = Folder.builder().id(folderId).name("Parent").build();
        Document document = Document.builder().id(UUID.randomUUID()).name("doc.txt").build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(folder));
        given(folderRepositoryPort.findByParentId(folderId)).willReturn(List.of());
        given(documentRepositoryPort.findByFolderId(folderId)).willReturn(List.of(document));

        // When / Then
        assertThatThrownBy(() -> deleteFolderService.deleteFolder(folderId, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("active documents");
        verify(folderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publish(any());
    }
}
