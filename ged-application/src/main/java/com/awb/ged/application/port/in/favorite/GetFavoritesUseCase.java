package com.awb.ged.application.port.in.favorite;

import com.awb.ged.application.dto.favorite.FavoriteResponseDto;

import java.util.List;
import java.util.UUID;

public interface GetFavoritesUseCase {
    List<FavoriteResponseDto> getFavorites(UUID userId);
}
