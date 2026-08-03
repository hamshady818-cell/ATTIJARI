package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.FavoriteRepositoryPort;
import com.awb.ged.domain.favorite.model.Favorite;
import com.awb.ged.infrastructure.persistence.entity.favorite.FavoriteJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.FavoriteJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class FavoriteRepositoryAdapter implements FavoriteRepositoryPort {

    private final FavoriteJpaRepository favoriteJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Autowired
    public FavoriteRepositoryAdapter(FavoriteJpaRepository favoriteJpaRepository,
                                     UserJpaRepository userJpaRepository) {
        this.favoriteJpaRepository = favoriteJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteJpaEntity entity = mapToEntity(favorite);
        FavoriteJpaEntity saved = favoriteJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Favorite> findById(UUID id) {
        return favoriteJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(UUID userId, String entityType, UUID entityId) {
        FavoriteJpaEntity.EntityType typeEnum = FavoriteJpaEntity.EntityType.valueOf(entityType.toUpperCase());
        return favoriteJpaRepository.findByUserIdAndEntityTypeAndEntityId(userId, typeEnum, entityId)
                .map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Favorite> findByUserId(UUID userId) {
        return favoriteJpaRepository.findByUserId(userId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        favoriteJpaRepository.deleteById(id);
    }

    private Favorite mapToDomain(FavoriteJpaEntity entity) {
        if (entity == null) return null;
        return Favorite.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .entityType(entity.getEntityType().name())
                .entityId(entity.getEntityId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private FavoriteJpaEntity mapToEntity(Favorite domain) {
        if (domain == null) return null;

        UserJpaEntity user = null;
        if (domain.getUserId() != null) {
            user = userJpaRepository.findById(domain.getUserId()).orElse(null);
        }

        FavoriteJpaEntity.EntityType typeEnum = FavoriteJpaEntity.EntityType.valueOf(domain.getEntityType().toUpperCase());

        FavoriteJpaEntity entity = FavoriteJpaEntity.builder()
                .user(user)
                .entityType(typeEnum)
                .entityId(domain.getEntityId())
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
