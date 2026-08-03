package com.awb.ged.application.service.favorite;

import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.favorite.model.Favorite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RemoveFavoriteServiceTest {

    @Mock
    private FavoriteRepositoryPort favoriteRepositoryPort;

    private RemoveFavoriteService removeFavoriteService;

    @BeforeEach
    void setUp() {
        removeFavoriteService = new RemoveFavoriteService(favoriteRepositoryPort);
    }

    @Test
    @DisplayName("Should successfully remove favorite when it exists and belongs to the user")
    void removeFavorite_Success() {
        // Given
        UUID favoriteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Favorite favorite = Favorite.builder()
                .id(favoriteId)
                .userId(userId)
                .build();

        given(favoriteRepositoryPort.findById(favoriteId)).willReturn(Optional.of(favorite));

        // When
        removeFavoriteService.removeFavorite(favoriteId, userId);

        // Then
        verify(favoriteRepositoryPort).delete(favoriteId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when favorite does not exist")
    void removeFavorite_NotFound_ThrowsNotFoundException() {
        // Given
        UUID favoriteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        given(favoriteRepositoryPort.findById(favoriteId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> removeFavoriteService.removeFavorite(favoriteId, userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ForbiddenException when user does not own the favorite")
    void removeFavorite_NotOwned_ThrowsForbiddenException() {
        // Given
        UUID favoriteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        Favorite favorite = Favorite.builder()
                .id(favoriteId)
                .userId(anotherUserId)
                .build();

        given(favoriteRepositoryPort.findById(favoriteId)).willReturn(Optional.of(favorite));

        // When / Then
        assertThatThrownBy(() -> removeFavoriteService.removeFavorite(favoriteId, userId))
                .isInstanceOf(ForbiddenException.class);
    }
}
