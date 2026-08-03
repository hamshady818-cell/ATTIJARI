package com.awb.ged.application.service.favorite;

import com.awb.ged.application.dto.favorite.AddFavoriteCommand;
import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.mapper.FavoriteMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.favorite.model.Favorite;
import com.awb.ged.domain.folder.model.Folder;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddFavoriteServiceTest {

    @Mock
    private FavoriteRepositoryPort favoriteRepositoryPort;

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    private final FavoriteMapper favoriteMapper = Mappers.getMapper(FavoriteMapper.class);

    private AddFavoriteService addFavoriteService;

    @BeforeEach
    void setUp() {
        addFavoriteService = new AddFavoriteService(favoriteRepositoryPort, documentRepositoryPort, folderRepositoryPort, favoriteMapper);
    }

    @Test
    @DisplayName("Should successfully add favorite for document when document exists and not already favorited")
    void addFavorite_DocumentSuccess() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .entityType("DOCUMENT")
                .entityId(docId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.of(Document.builder().id(docId).build()));
        given(favoriteRepositoryPort.findByUserIdAndEntityTypeAndEntityId(userId, "DOCUMENT", docId)).willReturn(Optional.empty());
        given(favoriteRepositoryPort.save(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        FavoriteResponseDto result = addFavoriteService.addFavorite(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntityType()).isEqualTo("DOCUMENT");
        assertThat(result.getEntityId()).isEqualTo(docId);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(favoriteRepositoryPort).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should successfully add favorite for folder when folder exists and not already favorited")
    void addFavorite_FolderSuccess() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .entityType("FOLDER")
                .entityId(folderId)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(Folder.builder().id(folderId).build()));
        given(favoriteRepositoryPort.findByUserIdAndEntityTypeAndEntityId(userId, "FOLDER", folderId)).willReturn(Optional.empty());
        given(favoriteRepositoryPort.save(any(Favorite.class))).willAnswer(invocation -> invocation.getArgument(0));

        // When
        FavoriteResponseDto result = addFavoriteService.addFavorite(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntityType()).isEqualTo("FOLDER");
        assertThat(result.getEntityId()).isEqualTo(folderId);
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(favoriteRepositoryPort).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when target document is not found")
    void addFavorite_DocumentNotFound_ThrowsNotFoundException() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .entityType("DOCUMENT")
                .entityId(docId)
                .build();

        given(documentRepositoryPort.findById(docId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> addFavoriteService.addFavorite(command))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document");
    }

    @Test
    @DisplayName("Should throw ConflictException when item is already a favorite")
    void addFavorite_AlreadyFavorited_ThrowsConflictException() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        AddFavoriteCommand command = AddFavoriteCommand.builder()
                .userId(userId)
                .entityType("FOLDER")
                .entityId(folderId)
                .build();

        given(folderRepositoryPort.findById(folderId)).willReturn(Optional.of(Folder.builder().id(folderId).build()));
        given(favoriteRepositoryPort.findByUserIdAndEntityTypeAndEntityId(userId, "FOLDER", folderId))
                .willReturn(Optional.of(Favorite.builder().build()));

        // When / Then
        assertThatThrownBy(() -> addFavoriteService.addFavorite(command))
                .isInstanceOf(ConflictException.class);
    }
}
