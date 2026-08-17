package com.awb.ged.application.service.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;
import com.awb.ged.application.mapper.CategoryMapper;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.category.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.metadata.model.MetadataDefinition;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceTest {

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort;

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepositoryPort, metadataDefinitionRepositoryPort, categoryMapper);
        PageResponse<MetadataDefinition> emptyPage = PageResponse.<MetadataDefinition>builder().content(List.of()).build();
        given(metadataDefinitionRepositoryPort.findAllActive(any(Integer.class), any(Integer.class))).willReturn(emptyPage);
    }

    @Test
    @DisplayName("Should create root category successfully")
    void createCategory_Root_Success() {
        // Given
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("RootCategory")
                .build();

        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        CategoryResponseDto result = categoryService.createCategory(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("RootCategory");
        assertThat(result.getParentId()).isNull();
        assertThat(result.getPath()).isEqualTo(result.getId().toString());
        verify(categoryRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should create sub category successfully")
    void createCategory_SubCategory_Success() {
        // Given
        UUID parentId = UUID.randomUUID();
        Category parent = Category.builder().id(parentId).name("Parent").path(parentId.toString()).build();
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("SubCategory")
                .parentId(parentId)
                .build();

        given(categoryRepositoryPort.findById(parentId)).willReturn(Optional.of(parent));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        CategoryResponseDto result = categoryService.createCategory(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("SubCategory");
        assertThat(result.getParentId()).isEqualTo(parentId);
        assertThat(result.getPath()).isEqualTo(parentId.toString() + "." + result.getId().toString());
        verify(categoryRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when parent category is not found")
    void createCategory_ParentNotFound_ThrowsNotFound() {
        // Given
        UUID parentId = UUID.randomUUID();
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("SubCategory")
                .parentId(parentId)
                .build();

        given(categoryRepositoryPort.findById(parentId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> categoryService.createCategory(command))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ConflictException when deleting category with children")
    void deleteCategory_HasChildren_ThrowsConflict() {
        // Given
        UUID catId = UUID.randomUUID();
        Category category = Category.builder().id(catId).name("Category").build();
        Category child = Category.builder().id(UUID.randomUUID()).parentId(catId).build();

        given(categoryRepositoryPort.findById(catId)).willReturn(Optional.of(category));
        given(categoryRepositoryPort.findActiveByParentId(catId)).willReturn(List.of(child));

        // When / Then
        assertThatThrownBy(() -> categoryService.deleteCategory(catId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("it has active sub-categories");
    }
}
