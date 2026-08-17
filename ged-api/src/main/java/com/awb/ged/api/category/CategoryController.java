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
    private final ListDeletedCategoriesUseCase listDeletedCategoriesUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final RestoreCategoryUseCase restoreCategoryUseCase;

    @Autowired
    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                               GetCategoryUseCase getCategoryUseCase,
                               ListCategoriesUseCase listCategoriesUseCase,
                               ListDeletedCategoriesUseCase listDeletedCategoriesUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase,
                               DeleteCategoryUseCase deleteCategoryUseCase,
                               RestoreCategoryUseCase restoreCategoryUseCase) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.listDeletedCategoriesUseCase = listDeletedCategoriesUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.restoreCategoryUseCase = restoreCategoryUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parentId(request.getParentId())
                .color(request.getColor())
                .icon(request.getIcon())
                .securityClass(request.getSecurityClass())
                .active(request.getActive())
                .build();
        CategoryResponseDto created = createCategoryUseCase.createCategory(command);
        URI location = URI.create("/api/v1/categories/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<CategoryResponseDto>> listDeletedCategories() {
        List<CategoryResponseDto> list = listDeletedCategoriesUseCase.listDeletedCategories();
        return ResponseEntity.ok(list);
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
            @RequestBody CategoryRequest request) {
        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .parentId(request.getParentId())
                .color(request.getColor())
                .icon(request.getIcon())
                .securityClass(request.getSecurityClass())
                .active(request.getActive())
                .build();
        CategoryResponseDto updated = updateCategoryUseCase.updateCategory(command);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDto> toggleActive(
            @PathVariable("id") UUID id,
            @RequestBody CategoryRequest request) {
        boolean active = request.getActive() != null ? request.getActive() : true;
        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(id)
                .active(active)
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

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<CategoryResponseDto> restoreCategory(@PathVariable("id") UUID id) {
        CategoryResponseDto restored = restoreCategoryUseCase.restoreCategory(id);
        return ResponseEntity.ok(restored);
    }
}
