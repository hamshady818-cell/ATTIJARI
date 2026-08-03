package com.awb.ged.application.service.favorite;

import com.awb.ged.application.port.in.favorite.RemoveFavoriteUseCase;
import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.ForbiddenException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.favorite.model.Favorite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RemoveFavoriteService implements RemoveFavoriteUseCase {

    private final FavoriteRepositoryPort favoriteRepositoryPort;

    @Autowired
    public RemoveFavoriteService(FavoriteRepositoryPort favoriteRepositoryPort) {
        this.favoriteRepositoryPort = favoriteRepositoryPort;
    }

    @Override
    public void removeFavorite(UUID favoriteId, UUID userId) {
        // 1. Find favorite
        Favorite favorite = favoriteRepositoryPort.findById(favoriteId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Favorite bookmark with ID " + favoriteId + " was not found."
                ));

        // 2. Security validation: Ensure user owns this bookmark
        if (!favorite.getUserId().equals(userId)) {
            throw new ForbiddenException(
                    ErrorCode.FORBIDDEN,
                    "You are not authorized to delete another user's favorite bookmark."
            );
        }

        // 3. Delete favorite
        favoriteRepositoryPort.delete(favoriteId);
    }
}
