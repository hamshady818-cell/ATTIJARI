package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.TagRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.tag.model.Tag;
import com.awb.ged.infrastructure.persistence.entity.tag.TagJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.TagJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class TagRepositoryAdapter implements TagRepositoryPort {

    private final TagJpaRepository tagJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public TagRepositoryAdapter(TagJpaRepository tagJpaRepository,
                                UserJpaRepository userJpaRepository,
                                CurrentUserProvider currentUserProvider) {
        this.tagJpaRepository = tagJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Tag save(Tag tag) {
        TagJpaEntity entity = mapToEntity(tag);
        TagJpaEntity saved = tagJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findById(UUID id) {
        return tagJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tag> findByName(String name) {
        return tagJpaRepository.findByName(name).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        tagJpaRepository.deleteById(id);
    }

    private Tag mapToDomain(TagJpaEntity entity) {
        if (entity == null) return null;
        return Tag.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TagJpaEntity mapToEntity(Tag domain) {
        if (domain == null) return null;

        TagJpaEntity entity = TagJpaEntity.builder()
                .name(domain.getName())
                .build();

        if (domain.getId() != null) {
            TagJpaEntity existing = tagJpaRepository.findById(domain.getId()).orElse(null);
            if (existing != null) {
                entity.setId(existing.getId());
                entity.setCreatedBy(existing.getCreatedBy());
                entity.setCreatedAt(existing.getCreatedAt());
            }
        }

        if (entity.getCreatedBy() == null) {
            UserJpaEntity creator = resolveCurrentUserEntity();
            entity.setCreatedBy(creator);
        }

        return entity;
    }

    private UserJpaEntity resolveCurrentUserEntity() {
        try {
            CurrentUser currentUser = currentUserProvider.getRequiredCurrentUser();
            return userJpaRepository.findByKeycloakId(currentUser.getKeycloakSub())
                    .orElseGet(() -> {
                        return userJpaRepository.findAll().stream().findFirst().orElse(null);
                    });
        } catch (Exception e) {
            return userJpaRepository.findAll().stream().findFirst().orElse(null);
        }
    }
}
