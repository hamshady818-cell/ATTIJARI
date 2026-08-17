package com.awb.ged.application.service.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;
import com.awb.ged.application.mapper.CategoryMapper;
import com.awb.ged.application.port.in.category.*;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.category.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.awb.ged.application.port.in.security.CurrentUserProvider;

@Service
@Transactional
public class CategoryService implements
        CreateCategoryUseCase,
        GetCategoryUseCase,
        ListCategoriesUseCase,
        ListDeletedCategoriesUseCase,
        UpdateCategoryUseCase,
        DeleteCategoryUseCase,
        RestoreCategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort;
    private final CategoryMapper categoryMapper;
    private final CurrentUserProvider currentUserProvider;

    public CategoryService(CategoryRepositoryPort categoryRepositoryPort,
                           MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort,
                           CategoryMapper categoryMapper) {
        this(categoryRepositoryPort, metadataDefinitionRepositoryPort, categoryMapper, null);
    }

    @Autowired
    public CategoryService(CategoryRepositoryPort categoryRepositoryPort,
                           MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort,
                           CategoryMapper categoryMapper,
                           @Autowired(required = false) CurrentUserProvider currentUserProvider) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.metadataDefinitionRepositoryPort = metadataDefinitionRepositoryPort;
        this.categoryMapper = categoryMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public CategoryResponseDto createCategory(CreateCategoryCommand command) {
        if (command.getName() == null || command.getName().trim().isEmpty()) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Le nom de la catégorie est obligatoire.");
        }

        String name = command.getName().trim();
        categoryRepositoryPort.findByName(name).ifPresent(c -> {
            throw new ConflictException(ErrorCode.INVALID_INPUT, "Une catégorie avec le nom '" + name + "' existe déjà.");
        });

        UUID id = UUID.randomUUID();
        String path = id.toString();

        if (command.getParentId() != null) {
            Category parent = categoryRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Catégorie parente introuvable."));
            if (parent.getDeletedAt() != null) {
                throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "La catégorie parente spécifiée a été supprimée.");
            }
            path = parent.getPath() + "." + id;
        }

        Category category = Category.builder()
                .id(id)
                .name(name)
                .description(command.getDescription() != null ? command.getDescription().trim() : null)
                .parentId(command.getParentId())
                .path(path)
                .color(command.getColor())
                .icon(command.getIcon())
                .securityClass(command.getSecurityClass() != null ? command.getSecurityClass().toUpperCase() : null)
                .active(command.getActive() != null ? command.getActive() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(category);
        return mapToDtoWithMetadataCount(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));
        return mapToDtoWithMetadataCount(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> listCategories(UUID parentId) {
        List<Category> list;
        if (parentId != null) {
            list = categoryRepositoryPort.findActiveByParentId(parentId);
        } else {
            list = categoryRepositoryPort.findAllActive();
        }
        return list.stream()
                .map(this::mapToDtoWithMetadataCount)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> listDeletedCategories() {
        return categoryRepositoryPort.findAllDeleted().stream()
                .map(this::mapToDtoWithMetadataCount)
                .toList();
    }

    @Override
    public CategoryResponseDto updateCategory(UpdateCategoryCommand command) {
        Category category = categoryRepositoryPort.findById(command.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        if (command.getParentId() != null && command.getParentId().equals(category.getId())) {
            throw new InvalidRequestException(ErrorCode.INVALID_INPUT, "Une catégorie ne peut pas être sa propre catégorie parente.");
        }

        String name = category.getName();
        if (command.getName() != null && !command.getName().trim().isEmpty()) {
            String trimmedName = command.getName().trim();
            if (!trimmedName.equalsIgnoreCase(category.getName())) {
                categoryRepositoryPort.findByName(trimmedName).ifPresent(c -> {
                    if (!c.getId().equals(category.getId())) {
                        throw new ConflictException(ErrorCode.INVALID_INPUT, "Une catégorie avec le nom '" + trimmedName + "' existe déjà.");
                    }
                });
            }
            name = trimmedName;
        }

        String path = category.getPath();
        UUID parentId = category.getParentId();
        if (command.getParentId() != null && !command.getParentId().equals(category.getParentId())) {
            Category parent = categoryRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Parent category not found."));
            parentId = parent.getId();
            path = parent.getPath() + "." + category.getId();
        }

        Category updated = category.toBuilder()
                .name(name)
                .description(command.getDescription() != null ? command.getDescription().trim() : category.getDescription())
                .parentId(parentId)
                .path(path)
                .color(command.getColor() != null ? command.getColor() : category.getColor())
                .icon(command.getIcon() != null ? command.getIcon() : category.getIcon())
                .securityClass(command.getSecurityClass() != null ? command.getSecurityClass().toUpperCase() : category.getSecurityClass())
                .active(command.getActive() != null ? command.getActive() : category.isActive())
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(updated);
        return mapToDtoWithMetadataCount(saved);
    }

    public CategoryResponseDto toggleActive(UUID id, boolean active) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        Category updated = category.toBuilder()
                .active(active)
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(updated);
        return mapToDtoWithMetadataCount(saved);
    }

    @Override
    public void deleteCategory(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        List<Category> children = categoryRepositoryPort.findActiveByParentId(id);
        if (!children.isEmpty()) {
            throw new ConflictException(ErrorCode.INVALID_INPUT, "Cannot delete category: it has active sub-categories.");
        }

        UUID deletedUserId = null;
        if (currentUserProvider != null) {
            try {
                deletedUserId = currentUserProvider.getRequiredCurrentUser().getId();
            } catch (Exception ignored) {}
        }

        Category softDeleted = category.toBuilder()
                .deletedAt(Instant.now())
                .deletedBy(deletedUserId)
                .updatedAt(Instant.now())
                .build();

        categoryRepositoryPort.save(softDeleted);
    }

    @Override
    public CategoryResponseDto restoreCategory(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        if (category.getDeletedAt() == null) {
            return mapToDtoWithMetadataCount(category);
        }

        Category restored = category.toBuilder()
                .deletedAt(null)
                .deletedBy(null)
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(restored);
        return mapToDtoWithMetadataCount(saved);
    }

    private CategoryResponseDto mapToDtoWithMetadataCount(Category category) {
        CategoryResponseDto dto = categoryMapper.toResponseDto(category);
        if (dto != null && category.getId() != null) {
            long count = metadataDefinitionRepositoryPort.findAllActive(0, 1000).getContent().stream()
                    .filter(def -> category.getId().equals(def.getCategoryId()))
                    .count();
            dto.setMetadataCount((int) count);
        }
        return dto;
    }
}
