package com.awb.ged.application.service.category;

import com.awb.ged.application.dto.category.CategoryResponseDto;
import com.awb.ged.application.dto.category.CreateCategoryCommand;
import com.awb.ged.application.dto.category.UpdateCategoryCommand;
import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.mapper.CategoryMapper;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.category.model.Category;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import com.awb.ged.application.service.metadata.MetadataDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
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
class CategoryBackendStep5ATest {

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    private final MetadataDefinitionMapper metadataMapper = Mappers.getMapper(MetadataDefinitionMapper.class);

    private CategoryService categoryService;
    private MetadataDefinitionService metadataDefinitionService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepositoryPort, metadataDefinitionRepositoryPort, categoryMapper);
        metadataDefinitionService = new MetadataDefinitionService(metadataDefinitionRepositoryPort, categoryRepositoryPort, metadataMapper, currentUserProvider);
        PageResponse<MetadataDefinition> emptyPage = PageResponse.<MetadataDefinition>builder().content(List.of()).build();
        given(metadataDefinitionRepositoryPort.findAllActive(any(Integer.class), any(Integer.class))).willReturn(emptyPage);
    }

    @Test
    @DisplayName("TEST 1 : Création catégorie => SUCCESS & active=true par défaut")
    void test1_createCategory_success_activeTrue() {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("Factures")
                .build();

        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.createCategory(command);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Factures");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("TEST 2 : Création avec description => description persistée")
    void test2_createCategory_withDescription() {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .name("Contrats")
                .description("Documents relatifs aux contrats fournisseurs.")
                .build();

        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.createCategory(command);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Documents relatifs aux contrats fournisseurs.");
    }

    @Test
    @DisplayName("TEST 3 : Modification description => description modifiée")
    void test3_updateCategory_description() {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder()
                .id(id)
                .name("Contrats")
                .description("Ancienne description")
                .active(true)
                .build();

        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(id)
                .description("Nouvelle description mise à jour")
                .build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.updateCategory(command);

        assertThat(result.getName()).isEqualTo("Contrats");
        assertThat(result.getDescription()).isEqualTo("Nouvelle description mise à jour");
    }

    @Test
    @DisplayName("TEST 4 : PATCH active=false => active=false")
    void test4_patch_activeFalse() {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder().id(id).name("Finance").active(true).build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.toggleActive(id, false);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("TEST 5 : PATCH active=true => active=true")
    void test5_patch_activeTrue() {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder().id(id).name("Finance").active(false).build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.toggleActive(id, true);

        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("TEST 6 : PATCH active uniquement => les autres propriétés restent inchangées")
    void test6_patch_activeOnly_preservesOtherFields() {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder()
                .id(id)
                .name("RH")
                .description("Catégorie RH")
                .securityClass("RH_ROLE")
                .active(true)
                .build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.toggleActive(id, false);

        assertThat(result.getName()).isEqualTo("RH");
        assertThat(result.getDescription()).isEqualTo("Catégorie RH");
        assertThat(result.getSecurityClass()).isEqualTo("RH_ROLE");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("TEST 7 : DELETE catégorie => soft delete (deletedAt renseigné, ligne conservée)")
    void test7_deleteCategory_softDelete() {
        UUID id = UUID.randomUUID();
        Category existing = Category.builder().id(id).name("À Supprimer").active(true).build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepositoryPort.findActiveByParentId(id)).willReturn(List.of());
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        categoryService.deleteCategory(id);

        verify(categoryRepositoryPort).save(any(Category.class));
    }

    @Test
    @DisplayName("TEST 8 : GET catégories actives => catégorie supprimée absente")
    void test8_getCategories_activeOnly() {
        Category activeCategory = Category.builder().id(UUID.randomUUID()).name("Active").deletedAt(null).build();
        given(categoryRepositoryPort.findAllActive()).willReturn(List.of(activeCategory));

        List<CategoryResponseDto> list = categoryService.listCategories(null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Active");
    }

    @Test
    @DisplayName("TEST 9 : GET /deleted => catégorie supprimée présente")
    void test9_getDeletedCategories() {
        Category deletedCategory = Category.builder().id(UUID.randomUUID()).name("Deleted").deletedAt(Instant.now()).build();
        given(categoryRepositoryPort.findAllDeleted()).willReturn(List.of(deletedCategory));

        List<CategoryResponseDto> list = categoryService.listDeletedCategories();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("Deleted");
    }

    @Test
    @DisplayName("TEST 10 : RESTORE => deletedAt=null")
    void test10_restoreCategory() {
        UUID id = UUID.randomUUID();
        Category deleted = Category.builder().id(id).name("RestoredCat").deletedAt(Instant.now()).active(true).build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(deleted));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.restoreCategory(id);

        assertThat(result.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("TEST 11 : RESTORE d'une catégorie inactive => active reste false")
    void test11_restoreInactiveCategory_activeStaysFalse() {
        UUID id = UUID.randomUUID();
        Category deletedInactive = Category.builder().id(id).name("RestoredCat").deletedAt(Instant.now()).active(false).build();

        given(categoryRepositoryPort.findById(id)).willReturn(Optional.of(deletedInactive));
        given(categoryRepositoryPort.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponseDto result = categoryService.restoreCategory(id);

        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("TEST 12 : MetadataDefinition avec categoryId => categoryId correctement persisté")
    void test12_createMetadataDefinition_withCategoryId() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder().id(categoryId).name("Factures").build();

        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_num")
                .label("Numéro Facture")
                .type(MetadataType.STRING)
                .categoryId(categoryId)
                .build();

        given(categoryRepositoryPort.findById(categoryId)).willReturn(Optional.of(category));
        given(metadataDefinitionRepositoryPort.findByName("invoice_num")).willReturn(Optional.empty());
        given(metadataDefinitionRepositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = metadataDefinitionService.createMetadataDefinition(command);

        assertThat(result.getCategoryId()).isEqualTo(categoryId);
    }

    @Test
    @DisplayName("TEST 13 : MetadataDefinition globale => categoryId=null")
    void test13_createMetadataDefinition_global_categoryIdNull() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("global_tag")
                .label("Tag Global")
                .type(MetadataType.STRING)
                .categoryId(null)
                .build();

        given(metadataDefinitionRepositoryPort.findByName("global_tag")).willReturn(Optional.empty());
        given(metadataDefinitionRepositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = metadataDefinitionService.createMetadataDefinition(command);

        assertThat(result.getCategoryId()).isNull();
    }

    @Test
    @DisplayName("TEST 14 : MetadataDefinition avec categoryId inexistant => erreur contrôlée (NotFoundException)")
    void test14_createMetadataDefinition_invalidCategoryId_throwsNotFound() {
        UUID invalidCategoryId = UUID.randomUUID();
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_num")
                .label("Numéro Facture")
                .type(MetadataType.STRING)
                .categoryId(invalidCategoryId)
                .build();

        given(categoryRepositoryPort.findById(invalidCategoryId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> metadataDefinitionService.createMetadataDefinition(command))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("TEST 15 : Catégorie comme son propre parent => erreur contrôlée (InvalidRequestException)")
    void test15_updateCategory_selfParent_throwsInvalidRequest() {
        UUID catId = UUID.randomUUID();
        Category existing = Category.builder().id(catId).name("SelfParent").build();

        UpdateCategoryCommand command = UpdateCategoryCommand.builder()
                .id(catId)
                .parentId(catId)
                .build();

        given(categoryRepositoryPort.findById(catId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.updateCategory(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("sa propre catégorie parente");
    }
}
