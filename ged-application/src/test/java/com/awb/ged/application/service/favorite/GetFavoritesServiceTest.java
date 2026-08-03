package com.awb.ged.application.service.favorite;

import com.awb.ged.application.dto.favorite.FavoriteResponseDto;
import com.awb.ged.application.mapper.FavoriteMapper;
import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.domain.favorite.model.Favorite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetFavoritesServiceTest {

    @Mock
    private FavoriteRepositoryPort favoriteRepositoryPort;

    private final FavoriteMapper favoriteMapper = Mappers.getMapper(FavoriteMapper.class);

    private GetFavoritesService getFavoritesService;

    @BeforeEach
    void setUp() {
        getFavoritesService = new GetFavoritesService(favoriteRepositoryPort, favoriteMapper);
    }

    @Test
    @DisplayName("Should successfully retrieve user favorites list")
    void getFavorites_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        Favorite f1 = Favorite.builder().id(UUID.randomUUID()).userId(userId).entityType("DOCUMENT").entityId(UUID.randomUUID()).build();
        Favorite f2 = Favorite.builder().id(UUID.randomUUID()).userId(userId).entityType("FOLDER").entityId(UUID.randomUUID()).build();

        given(favoriteRepositoryPort.findByUserId(userId)).willReturn(List.of(f1, f2));

        // When
        List<FavoriteResponseDto> result = getFavoritesService.getFavorites(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEntityType()).isEqualTo("DOCUMENT");
        assertThat(result.get(1).getEntityType()).isEqualTo("FOLDER");
    }
}
