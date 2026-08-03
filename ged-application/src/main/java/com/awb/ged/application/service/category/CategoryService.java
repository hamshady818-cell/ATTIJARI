package com.awb.ged.application.service.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;
import com.awb.ged.application.mapper.CategoryMapper;
import com.awb.ged.application.port.in.category.*;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.category.model.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService implements CreateCategoryUseCase, GetCategoryUseCase, ListCategoriesUseCase, UpdateCategoryUseCase, DeleteCategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;
    private final CategoryMapper categoryMapper;

    @Autowired
    public CategoryService(CategoryRepositoryPort categoryRepositoryPort, CategoryMapper categoryMapper) {
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public CategoryResponseDto createCategory(CreateCategoryCommand command) {
        UUID id = UUID.randomUUID();
        String path = id.toString();

        if (command.getParentId() != null) {
            Category parent = categoryRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Parent category not found."));
            path = parent.getPath() + "." + id;
        }

        Category category = Category.builder()
                .id(id)
                .name(command.getName())
                .parentId(command.getParentId())
                .path(path)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(category);
        return categoryMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));
        return categoryMapper.toResponseDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> listCategories(UUID parentId) {
        List<Category> list;
        if (parentId != null) {
            list = categoryRepositoryPort.findByParentId(parentId);
        } else {
            list = categoryRepositoryPort.findAll();
        }
        return list.stream()
                .map(categoryMapper::toResponseDto)
                .toList();
    }

    @Override
    public CategoryResponseDto updateCategory(UpdateCategoryCommand command) {
        Category category = categoryRepositoryPort.findById(command.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        String path = category.getPath();
        if (command.getParentId() != null && !command.getParentId().equals(category.getParentId())) {
            Category parent = categoryRepositoryPort.findById(command.getParentId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Parent category not found."));
            path = parent.getPath() + "." + category.getId();
        } else if (command.getParentId() == null) {
            path = category.getId().toString();
        }

        Category updated = category.toBuilder()
                .name(command.getName())
                .parentId(command.getParentId())
                .path(path)
                .updatedAt(Instant.now())
                .build();

        Category saved = categoryRepositoryPort.save(updated);
        return categoryMapper.toResponseDto(saved);
    }

    @Override
    public void deleteCategory(UUID id) {
        Category category = categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Category not found."));

        List<Category> children = categoryRepositoryPort.findByParentId(id);
        if (!children.isEmpty()) {
            throw new ConflictException(ErrorCode.INVALID_INPUT, "Cannot delete category: it has active sub-categories.");
        }

        categoryRepositoryPort.delete(id);
    }
}
