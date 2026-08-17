package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.category.model.Category;
import com.awb.ged.infrastructure.persistence.entity.category.CategoryJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.CategoryJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository categoryJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public CategoryRepositoryAdapter(CategoryJpaRepository categoryJpaRepository,
                                     UserJpaRepository userJpaRepository,
                                     CurrentUserProvider currentUserProvider) {
        this.categoryJpaRepository = categoryJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = mapToEntity(category);
        CategoryJpaEntity saved = categoryJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID id) {
        return categoryJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllActive() {
        return categoryJpaRepository.findAllByDeletedAtIsNull().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findByParentId(UUID parentId) {
        return categoryJpaRepository.findByParentCategoryId(parentId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findActiveByParentId(UUID parentId) {
        return categoryJpaRepository.findByParentCategoryIdAndDeletedAtIsNull(parentId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllDeleted() {
        return categoryJpaRepository.findAllByDeletedAtIsNotNull().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findByName(String name) {
        return categoryJpaRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name).map(this::mapToDomain);
    }

    @Override
    public void delete(UUID id) {
        categoryJpaRepository.deleteById(id);
    }

    private Category mapToDomain(CategoryJpaEntity entity) {
        if (entity == null) return null;
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .parentId(entity.getParentCategory() != null ? entity.getParentCategory().getId() : null)
                .path(entity.getPath())
                .color(entity.getColor())
                .icon(entity.getIcon())
                .securityClass(entity.getSecurityClass())
                .active(entity.isActive())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CategoryJpaEntity mapToEntity(Category domain) {
        if (domain == null) return null;

        CategoryJpaEntity parent = null;
        if (domain.getParentId() != null) {
            parent = categoryJpaRepository.findById(domain.getParentId()).orElse(null);
        }

        UserJpaEntity deletedByUser = null;
        if (domain.getDeletedBy() != null) {
            deletedByUser = userJpaRepository.findById(domain.getDeletedBy()).orElse(null);
        }

        CategoryJpaEntity entity = CategoryJpaEntity.builder()
                .name(domain.getName())
                .description(domain.getDescription())
                .path(domain.getPath() != null ? domain.getPath() : "")
                .color(domain.getColor())
                .icon(domain.getIcon())
                .parentCategory(parent)
                .securityClass(domain.getSecurityClass())
                .active(domain.isActive())
                .deletedAt(domain.getDeletedAt())
                .deletedBy(deletedByUser)
                .build();

        if (domain.getId() != null) {
            CategoryJpaEntity existing = categoryJpaRepository.findById(domain.getId()).orElse(null);
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
                        // Fallback to first user if not found in db during context init
                        return userJpaRepository.findAll().stream().findFirst().orElse(null);
                    });
        } catch (Exception e) {
            // Fallback for tests or asynchronous tasks
            return userJpaRepository.findAll().stream().findFirst().orElse(null);
        }
    }
}
