package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.category.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    List<Category> findByParentId(UUID parentId);

    void delete(UUID id);
}
