package com.awb.ged.api.category;

import com.awb.ged.api.category.dto.CategoryRequest;
import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;
import com.awb.ged.application.port.in.category.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    @Autowired
    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                              GetCategoryUseCase getCategoryUseCase,
                              ListCategoriesUseCase listCategoriesUseCase,
                              UpdateCategoryUseCase updateCategoryUseCase,
                              DeleteCategoryUseCase deleteCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name(request.getName())
                .parentId(request.getParentId())
                .build();
        CategoryResponseDto created = createCategoryUseCase.createCategory(command);
        URI location = URI.create("/api/v1/categories/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable("id") UUID id) {
        CategoryResponseDto category = getCategoryUseCase.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<List<CategoryResponseDto>> listCategories(
            @RequestParam(value = "parentId", required = false) UUID parentId) {
        List<CategoryResponseDto> list = listCategoriesUseCase.listCategories(parentId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CategoryRequest request) {
        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(id)
                .name(request.getName())
                .parentId(request.getParentId())
                .build();
        CategoryResponseDto updated = updateCategoryUseCase.updateCategory(command);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") UUID id) {
        deleteCategoryUseCase.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
