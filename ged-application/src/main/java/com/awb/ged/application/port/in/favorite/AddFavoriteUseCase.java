package com.awb.ged.application.port.in.favorite;

import com.awb.ged.application.dto.favorite.AddFavoriteCommand;
import com.awb.ged.application.dto.favorite.FavoriteResponseDto;

public interface AddFavoriteUseCase {
    FavoriteResponseDto addFavorite(AddFavoriteCommand command);
}
