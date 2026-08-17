package com.awb.ged.application.port.out.persistence;

import com.awb.ged.domain.category.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    List<Category> findAllActive();

    List<Category> findByParentId(UUID parentId);

    List<Category> findActiveByParentId(UUID parentId);

    List<Category> findAllDeleted();

    Optional<Category> findByName(String name);

    void delete(UUID id);
}
