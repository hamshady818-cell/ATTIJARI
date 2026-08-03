package com.awb.ged.infrastructure.trash;

import com.awb.ged.domain.document.event.DocumentDeletedEvent;
import com.awb.ged.domain.folder.event.FolderDeletedEvent;
import com.awb.ged.infrastructure.persistence.entity.folder.FolderJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.trash.TrashJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FolderJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.TrashJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrashEventListenerTest {

    @Mock
    private TrashJpaRepository trashJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private FolderJpaRepository folderJpaRepository;

    private TrashEventListener trashEventListener;

    @BeforeEach
    void setUp() {
        trashEventListener = new TrashEventListener(trashJpaRepository, userJpaRepository, folderJpaRepository);
    }

    @Test
    @DisplayName("Should successfully capture and persist Trash item when DocumentDeletedEvent is received")
    void handleDocumentDeleted_Success() {
        // Given
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        DocumentDeletedEvent event = new DocumentDeletedEvent(docId, "doc.pdf", folderId, userId);

        UserJpaEntity userEntity = new UserJpaEntity();
        userEntity.setId(userId);
        FolderJpaEntity folderEntity = new FolderJpaEntity();
        folderEntity.setId(folderId);

        given(userJpaRepository.findById(userId)).willReturn(Optional.of(userEntity));
        given(folderJpaRepository.findById(folderId)).willReturn(Optional.of(folderEntity));

        // When
        trashEventListener.handleDocumentDeleted(event);

        // Then
        ArgumentCaptor<TrashJpaEntity> captor = ArgumentCaptor.forClass(TrashJpaEntity.class);
        verify(trashJpaRepository).save(captor.capture());
        TrashJpaEntity captured = captor.getValue();

        assertThat(captured.getEntityType()).isEqualTo(TrashJpaEntity.EntityType.DOCUMENT);
        assertThat(captured.getEntityId()).isEqualTo(docId);
        assertThat(captured.getDeletedBy()).isEqualTo(userEntity);
        assertThat(captured.getOriginalFolder()).isEqualTo(folderEntity);
        assertThat(captured.getDeletedAt()).isEqualTo(event.getOccurredAt());
        assertThat(captured.getAutoPurgeAt()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully capture and persist Trash item when FolderDeletedEvent is received")
    void handleFolderDeleted_Success() {
        // Given
        UUID folderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        FolderDeletedEvent event = new FolderDeletedEvent(folderId, "finance", parentId, userId);

        UserJpaEntity userEntity = new UserJpaEntity();
        userEntity.setId(userId);
        FolderJpaEntity parentEntity = new FolderJpaEntity();
        parentEntity.setId(parentId);

        given(userJpaRepository.findById(userId)).willReturn(Optional.of(userEntity));
        given(folderJpaRepository.findById(parentId)).willReturn(Optional.of(parentEntity));

        // When
        trashEventListener.handleFolderDeleted(event);

        // Then
        ArgumentCaptor<TrashJpaEntity> captor = ArgumentCaptor.forClass(TrashJpaEntity.class);
        verify(trashJpaRepository).save(captor.capture());
        TrashJpaEntity captured = captor.getValue();

        assertThat(captured.getEntityType()).isEqualTo(TrashJpaEntity.EntityType.FOLDER);
        assertThat(captured.getEntityId()).isEqualTo(folderId);
        assertThat(captured.getDeletedBy()).isEqualTo(userEntity);
        assertThat(captured.getOriginalFolder()).isEqualTo(parentEntity);
        assertThat(captured.getDeletedAt()).isEqualTo(event.getOccurredAt());
        assertThat(captured.getAutoPurgeAt()).isNotNull();
    }
}
