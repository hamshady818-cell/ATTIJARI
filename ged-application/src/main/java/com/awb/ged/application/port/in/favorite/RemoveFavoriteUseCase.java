package com.awb.ged.application.port.in.favorite;

import java.util.UUID;

public interface RemoveFavoriteUseCase {
    void removeFavorite(UUID favoriteId, UUID userId);
}
