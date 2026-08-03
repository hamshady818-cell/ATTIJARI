package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.favorite.model.Favorite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepositoryPort {

    Favorite save(Favorite favorite);

    Optional<Favorite> findById(UUID id);

    Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(UUID userId, String entityType, UUID entityId);

    List<Favorite> findByUserId(UUID userId);

    void delete(UUID id);
}
