package com.awb.ged.application.service.trash;

import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.TrashRepositoryPort;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.folder.model.Folder;
import com.awb.ged.domain.trash.model.TrashItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RestoreFromTrashServiceTest {

    @Mock
    private TrashRepositoryPort trashRepositoryPort;

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    private RestoreFromTrashService restoreFromTrashService;

    @BeforeEach
    void setUp() {
        restoreFromTrashService = new RestoreFromTrashService(trashRepositoryPort, documentRepositoryPort, folderRepositoryPort);
    }

    @Test
    @DisplayName("Should successfully restore a document from trash")
    void restoreFromTrash_DocumentSuccess() {
        // Given
        UUID trashId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TrashItem item = TrashItem.builder()
                .id(trashId)
                .entityType("DOCUMENT")
                .entityId(docId)
                .deletedBy(userId)
                .build();

        Document document = Document.builder()
                .id(docId)
                .name("doc.txt")
                .deletedAt(java.time.Instant.now())
                .deletedBy(userId)
                .build();

        given(trashRepositoryPort.findById(trashId)).willReturn(Optional.of(item));
        given(documentRepositoryPort.findByIdIncludingDeleted(docId)).willReturn(Optional.of(document));

        // When
        restoreFromTrashService.restoreFromTrash(trashId, userId);

        // Then
        assertThat(document.getDeletedAt()).isNull();
        assertThat(document.getDeletedBy()).isNull();
        verify(documentRepositoryPort).save(document);
        verify(trashRepositoryPort).delete(trashId);
    }

    @Test
    @DisplayName("Should successfully restore a folder from trash")
    void restoreFromTrash_FolderSuccess() {
        // Given
        UUID trashId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TrashItem item = TrashItem.builder()
                .id(trashId)
                .entityType("FOLDER")
                .entityId(folderId)
                .deletedBy(userId)
                .build();

        Folder folder = Folder.builder()
                .id(folderId)
                .name("SubFolder")
                .deletedAt(java.time.Instant.now())
                .deletedBy(userId)
                .build();

        given(trashRepositoryPort.findById(trashId)).willReturn(Optional.of(item));
        given(folderRepositoryPort.findByIdIncludingDeleted(folderId)).willReturn(Optional.of(folder));

        // When
        restoreFromTrashService.restoreFromTrash(trashId, userId);

        // Then
        assertThat(folder.getDeletedAt()).isNull();
        assertThat(folder.getDeletedBy()).isNull();
        verify(folderRepositoryPort).save(folder);
        verify(trashRepositoryPort).delete(trashId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when trash item not found")
    void restoreFromTrash_NotFound_ThrowsNotFoundException() {
        // Given
        UUID trashId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(trashRepositoryPort.findById(trashId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> restoreFromTrashService.restoreFromTrash(trashId, userId))
                .isInstanceOf(NotFoundException.class);
        verify(documentRepositoryPort, never()).save(any());
        verify(folderRepositoryPort, never()).save(any());
    }
}
