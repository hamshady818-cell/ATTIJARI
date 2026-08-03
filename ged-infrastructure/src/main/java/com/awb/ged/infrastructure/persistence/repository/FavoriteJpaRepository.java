package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.favorite.FavoriteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteJpaRepository extends JpaRepository<FavoriteJpaEntity, UUID> {

    Optional<FavoriteJpaEntity> findByUserIdAndEntityTypeAndEntityId(UUID userId, FavoriteJpaEntity.EntityType entityType, UUID entityId);

    List<FavoriteJpaEntity> findByUserId(UUID userId);
}
