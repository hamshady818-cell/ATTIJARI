package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.CategoryRepositoryPort;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.category.model.Category;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MetadataTrashLifecycleTest {

    private final Map<UUID, MetadataDefinition> memoryDb = new ConcurrentHashMap<>();

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private MetadataDefinitionService service;

    @BeforeEach
    void setUp() {
        memoryDb.clear();

        MetadataDefinitionRepositoryPort repositoryPort = new MetadataDefinitionRepositoryPort() {
            @Override
            public MetadataDefinition save(MetadataDefinition definition) {
                UUID id = definition.getId() != null ? definition.getId() : UUID.randomUUID();
                MetadataDefinition saved = definition.toBuilder()
                        .id(id)
                        .createdAt(definition.getCreatedAt() != null ? definition.getCreatedAt() : Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                memoryDb.put(id, saved);
                return saved;
            }

            @Override
            public Optional<MetadataDefinition> findById(UUID id) {
                MetadataDefinition def = memoryDb.get(id);
                if (def != null && def.getDeletedAt() == null) {
                    return Optional.of(def);
                }
                return Optional.empty();
            }

            @Override
            public Optional<MetadataDefinition> findByName(String name) {
                return memoryDb.values().stream()
                        .filter(d -> d.getDeletedAt() == null && d.getName().equalsIgnoreCase(name))
                        .findFirst();
            }

            @Override
            public PageResponse<MetadataDefinition> findAllActive(int page, int size) {
                List<MetadataDefinition> activeList = memoryDb.values().stream()
                        .filter(d -> d.getDeletedAt() == null)
                        .sorted(Comparator.comparingInt(d -> d.getDisplayOrder() != null ? d.getDisplayOrder() : 0))
                        .toList();
                return PageResponse.<MetadataDefinition>builder()
                        .content(activeList)
                        .totalElements(activeList.size())
                        .totalPages(1)
                        .pageNumber(page)
                        .pageSize(size)
                        .build();
            }

            @Override
            public PageResponse<MetadataDefinition> findAllDeleted(int page, int size) {
                List<MetadataDefinition> deletedList = memoryDb.values().stream()
                        .filter(d -> d.getDeletedAt() != null)
                        .sorted(Comparator.comparing(MetadataDefinition::getUpdatedAt).reversed())
                        .toList();
                return PageResponse.<MetadataDefinition>builder()
                        .content(deletedList)
                        .totalElements(deletedList.size())
                        .totalPages(1)
                        .pageNumber(page)
                        .pageSize(size)
                        .build();
            }

            @Override
            public void softDelete(UUID id, UUID deletedByUserId) {
                MetadataDefinition existing = memoryDb.get(id);
                if (existing != null && existing.getDeletedAt() == null) {
                    MetadataDefinition softDeleted = existing.toBuilder()
                            .deletedAt(Instant.now())
                            .deletedBy(deletedByUserId)
                            .updatedAt(Instant.now())
                            .build();
                    memoryDb.put(id, softDeleted);
                }
            }

            @Override
            public MetadataDefinition restore(UUID id) {
                MetadataDefinition existing = memoryDb.get(id);
                if (existing != null) {
                    MetadataDefinition restored = existing.toBuilder()
                            .deletedAt(null)
                            .deletedBy(null)
                            .updatedAt(Instant.now())
                            .build();
                    memoryDb.put(id, restored);
                    return restored;
                }
                return null;
            }
        };

        MetadataDefinitionMapper mapper = new MetadataDefinitionMapper() {
            @Override
            public MetadataDefinitionResponseDto toResponseDto(MetadataDefinition domain) {
                if (domain == null) return null;
                return MetadataDefinitionResponseDto.builder()
                        .id(domain.getId())
                        .name(domain.getName())
                        .label(domain.getLabel())
                        .type(domain.getType())
                        .required(domain.isRequired())
                        .description(domain.getDescription())
                        .defaultValue(domain.getDefaultValue())
                        .displayOrder(domain.getDisplayOrder())
                        .active(domain.isActive())
                        .options(domain.getOptions())
                        .validationPattern(domain.getValidationPattern())
                        .categoryId(domain.getCategoryId())
                        .createdAt(domain.getCreatedAt())
                        .updatedAt(domain.getUpdatedAt())
                        .deletedAt(domain.getDeletedAt())
                        .deletedBy(domain.getDeletedBy())
                        .build();
            }
        };

        org.mockito.Mockito.lenient().when(currentUserProvider.getCurrentUser())
                .thenReturn(Optional.of(CurrentUser.builder().id(UUID.randomUUID()).username("admin").build()));

        service = new MetadataDefinitionService(
                repositoryPort,
                categoryRepositoryPort,
                mapper,
                currentUserProvider
        );
    }

    @Test
    @DisplayName("Test 1, 2, 3, 4, 5 — Full Metadata Definition Trash & Category Scope Lifecycle")
    void testFullMetadataTrashLifecycle() {
        // Step 0: Create 3 global definitions
        MetadataDefinitionResponseDto defBoolean = service.createMetadataDefinition(
                CreateMetadataDefinitionCommand.builder()
                        .name("TestBoolean")
                        .label("Test Boolean")
                        .type(MetadataType.BOOLEAN)
                        .required(true)
                        .build()
        );

        MetadataDefinitionResponseDto defSelect = service.createMetadataDefinition(
                CreateMetadataDefinitionCommand.builder()
                        .name("TestSelect")
                        .label("Test Select")
                        .type(MetadataType.SELECT)
                        .options(List.of("Option A", "Option B"))
                        .build()
        );

        MetadataDefinitionResponseDto defFacture = service.createMetadataDefinition(
                CreateMetadataDefinitionCommand.builder()
                        .name("TestFacture")
                        .label("Test Facture")
                        .type(MetadataType.STRING)
                        .build()
        );

        assertThat(service.listMetadataDefinitions(0, 20).getContent()).hasSize(3);
        assertThat(service.listDeletedMetadataDefinitions(0, 20).getContent()).isEmpty();

        // TEST 1: Supprimer TestBoolean
        service.deleteMetadataDefinition(defBoolean.getId());

        List<MetadataDefinitionResponseDto> activeAfterTest1 = service.listMetadataDefinitions(0, 20).getContent();
        List<MetadataDefinitionResponseDto> deletedAfterTest1 = service.listDeletedMetadataDefinitions(0, 20).getContent();

        assertThat(activeAfterTest1).extracting(MetadataDefinitionResponseDto::getName)
                .doesNotContain("TestBoolean")
                .containsExactlyInAnyOrder("TestSelect", "TestFacture");
        assertThat(deletedAfterTest1).extracting(MetadataDefinitionResponseDto::getName)
                .containsExactly("TestBoolean");
        assertThat(deletedAfterTest1.get(0).getDeletedAt()).isNotNull();

        // TEST 2: Supprimer TestSelect
        service.deleteMetadataDefinition(defSelect.getId());

        List<MetadataDefinitionResponseDto> activeAfterTest2 = service.listMetadataDefinitions(0, 20).getContent();
        List<MetadataDefinitionResponseDto> deletedAfterTest2 = service.listDeletedMetadataDefinitions(0, 20).getContent();

        assertThat(activeAfterTest2).extracting(MetadataDefinitionResponseDto::getName)
                .containsExactly("TestFacture");
        assertThat(deletedAfterTest2).extracting(MetadataDefinitionResponseDto::getName)
                .containsExactlyInAnyOrder("TestBoolean", "TestSelect");

        // TEST 3: Restaurer TestBoolean
        MetadataDefinitionResponseDto restoredBoolean = service.restoreMetadataDefinition(defBoolean.getId());

        assertThat(restoredBoolean.getDeletedAt()).isNull();

        List<MetadataDefinitionResponseDto> activeAfterTest3 = service.listMetadataDefinitions(0, 20).getContent();
        List<MetadataDefinitionResponseDto> deletedAfterTest3 = service.listDeletedMetadataDefinitions(0, 20).getContent();

        assertThat(activeAfterTest3).extracting(MetadataDefinitionResponseDto::getName)
                .containsExactlyInAnyOrder("TestBoolean", "TestFacture");
        assertThat(deletedAfterTest3).extracting(MetadataDefinitionResponseDto::getName)
                .containsExactly("TestSelect");

        // TEST 4: Désactiver TestFacture (active = false) -> Doit RESTER dans 'Actives' et ne PAS apparaître dans Corbeille
        service.updateMetadataDefinition(
                UpdateMetadataDefinitionCommand.builder()
                        .id(defFacture.getId())
                        .active(false)
                        .build()
        );

        List<MetadataDefinitionResponseDto> activeAfterTest4 = service.listMetadataDefinitions(0, 20).getContent();
        List<MetadataDefinitionResponseDto> deletedAfterTest4 = service.listDeletedMetadataDefinitions(0, 20).getContent();

        assertThat(activeAfterTest4).extracting(MetadataDefinitionResponseDto::getName)
                .contains("TestFacture");
        assertThat(activeAfterTest4.stream().filter(d -> d.getName().equals("TestFacture")).findFirst().get().isActive())
                .isFalse();
        assertThat(deletedAfterTest4).extracting(MetadataDefinitionResponseDto::getName)
                .doesNotContain("TestFacture");

        // TEST 5: Supprimer une métadonnée spécifique à une catégorie
        UUID categoryFacturesId = UUID.randomUUID();
        org.mockito.Mockito.when(categoryRepositoryPort.findById(categoryFacturesId))
                .thenReturn(Optional.of(Category.builder().id(categoryFacturesId).name("Factures").build()));

        MetadataDefinitionResponseDto defCategoryScoped = service.createMetadataDefinition(
                CreateMetadataDefinitionCommand.builder()
                        .name("numero_facture")
                        .label("Numéro de facture")
                        .type(MetadataType.STRING)
                        .categoryId(categoryFacturesId)
                        .build()
        );

        assertThat(defCategoryScoped.getCategoryId()).isEqualTo(categoryFacturesId);

        // Supprimer la métadonnée liée à Factures
        service.deleteMetadataDefinition(defCategoryScoped.getId());

        List<MetadataDefinitionResponseDto> deletedAfterTest5 = service.listDeletedMetadataDefinitions(0, 20).getContent();
        MetadataDefinitionResponseDto deletedCatDef = deletedAfterTest5.stream()
                .filter(d -> d.getName().equals("numero_facture"))
                .findFirst().orElseThrow();

        assertThat(deletedCatDef.getCategoryId()).isEqualTo(categoryFacturesId);
        assertThat(deletedCatDef.getDeletedAt()).isNotNull();

        // Restaurer la métadonnée liée à Factures
        MetadataDefinitionResponseDto restoredCatDef = service.restoreMetadataDefinition(defCategoryScoped.getId());
        assertThat(restoredCatDef.getCategoryId()).isEqualTo(categoryFacturesId);
        assertThat(restoredCatDef.getDeletedAt()).isNull();
    }
}
