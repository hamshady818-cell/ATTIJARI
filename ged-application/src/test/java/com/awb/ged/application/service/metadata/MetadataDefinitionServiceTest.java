package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class MetadataDefinitionServiceTest {

    @Mock
    private MetadataDefinitionRepositoryPort repositoryPort;

    @Mock
    private com.awb.ged.application.port.out.persistence.CategoryRepositoryPort categoryRepositoryPort;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private final MetadataDefinitionMapper mapper = Mappers.getMapper(MetadataDefinitionMapper.class);

    private MetadataDefinitionService service;

    @BeforeEach
    void setUp() {
        service = new MetadataDefinitionService(repositoryPort, categoryRepositoryPort, mapper, currentUserProvider);
    }

    // TEST 1 : Création STRING -> succès
    @Test
    @DisplayName("TEST 1 : Should create STRING metadata definition successfully")
    void test1_createStringMetadataDefinition_Success() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_number")
                .label("Numéro de Facture")
                .type(MetadataType.STRING)
                .required(true)
                .build();

        given(repositoryPort.findByName("invoice_number")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("invoice_number");
        assertThat(result.getType()).isEqualTo(MetadataType.STRING);
        assertThat(result.isActive()).isTrue();
    }

    // TEST 2 : Création BOOLEAN -> succès
    @Test
    @DisplayName("TEST 2 : Should create BOOLEAN metadata definition successfully")
    void test2_createBooleanMetadataDefinition_Success() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("is_confidential")
                .label("Confidentiel")
                .type(MetadataType.BOOLEAN)
                .required(false)
                .build();

        given(repositoryPort.findByName("is_confidential")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(MetadataType.BOOLEAN);
    }

    // TEST 3 : Création SELECT avec options -> succès
    @Test
    @DisplayName("TEST 3 : Should create SELECT metadata definition with options successfully")
    void test3_createSelectWithOptions_Success() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("status")
                .label("Statut")
                .type(MetadataType.SELECT)
                .options(List.of("En cours", " Validé ", "Rejeté", ""))
                .build();

        given(repositoryPort.findByName("status")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        assertThat(result).isNotNull();
        assertThat(result.getOptions()).containsExactly("En cours", "Validé", "Rejeté");
    }

    // TEST 4 : Création SELECT sans options -> erreur
    @Test
    @DisplayName("TEST 4 : Should throw InvalidRequestException when creating SELECT without options")
    void test4_createSelectWithoutOptions_ThrowsException() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("status")
                .label("Statut")
                .type(MetadataType.SELECT)
                .options(List.of("  "))
                .build();

        given(repositoryPort.findByName("status")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMetadataDefinition(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Les types SELECT et MULTI_SELECT doivent contenir au moins une option");
    }

    // TEST 5 : Création MULTI_SELECT avec options -> succès
    @Test
    @DisplayName("TEST 5 : Should create MULTI_SELECT metadata definition with options successfully")
    void test5_createMultiSelectWithOptions_Success() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("tags")
                .label("Mots clés")
                .type(MetadataType.MULTI_SELECT)
                .options(List.of("IT", "Finance", "RH"))
                .build();

        given(repositoryPort.findByName("tags")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        assertThat(result).isNotNull();
        assertThat(result.getOptions()).containsExactly("IT", "Finance", "RH");
    }

    // TEST 6 : Création MULTI_SELECT sans options -> erreur
    @Test
    @DisplayName("TEST 6 : Should throw InvalidRequestException when creating MULTI_SELECT without options")
    void test6_createMultiSelectWithoutOptions_ThrowsException() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("tags")
                .label("Mots clés")
                .type(MetadataType.MULTI_SELECT)
                .options(null)
                .build();

        given(repositoryPort.findByName("tags")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMetadataDefinition(command))
                .isInstanceOf(InvalidRequestException.class);
    }

    // TEST 7 : Création avec description/defaultValue/displayOrder/active -> toutes les valeurs sont persistées
    @Test
    @DisplayName("TEST 7 : Should persist all optional fields (description, defaultValue, displayOrder, active)")
    void test7_createMetadataDefinition_FullFields() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("ref_num")
                .label("Référence")
                .type(MetadataType.STRING)
                .description("Numéro de référence interne")
                .defaultValue("REF-000")
                .displayOrder(5)
                .active(true)
                .build();

        given(repositoryPort.findByName("ref_num")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        assertThat(result.getDescription()).isEqualTo("Numéro de référence interne");
        assertThat(result.getDefaultValue()).isEqualTo("REF-000");
        assertThat(result.getDisplayOrder()).isEqualTo(5);
        assertThat(result.isActive()).isTrue();
    }

    // TEST 8 : PATCH description uniquement -> les autres propriétés restent inchangées
    @Test
    @DisplayName("TEST 8 : Should patch description without modifying other properties")
    void test8_patchDescriptionOnly_PreservesOtherFields() {
        UUID id = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(id)
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .required(true)
                .description("Old description")
                .defaultValue("INV-0")
                .displayOrder(2)
                .active(true)
                .build();

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(id)
                .description("New description")
                .build();

        given(repositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.updateMetadataDefinition(command);

        assertThat(result.getDescription()).isEqualTo("New description");
        assertThat(result.getName()).isEqualTo("invoice_number");
        assertThat(result.isRequired()).isTrue();
        assertThat(result.getDefaultValue()).isEqualTo("INV-0");
        assertThat(result.getDisplayOrder()).isEqualTo(2);
        assertThat(result.isActive()).isTrue();
    }

    // TEST 9 : PATCH active=false -> active devient false
    @Test
    @DisplayName("TEST 9 : Should patch active=false successfully")
    void test9_patchActiveFalse() {
        UUID id = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(id)
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .active(true)
                .build();

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(id)
                .active(false)
                .build();

        given(repositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.updateMetadataDefinition(command);

        assertThat(result.isActive()).isFalse();
    }

    // TEST 10 : PATCH options SELECT -> nouvelles options correctement persistées
    @Test
    @DisplayName("TEST 10 : Should patch SELECT options successfully")
    void test10_patchSelectOptions() {
        UUID id = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(id)
                .name("status")
                .label("Statut")
                .type(MetadataType.SELECT)
                .options(List.of("Option A"))
                .build();

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(id)
                .options(List.of("Option A", "Option B", "Option C"))
                .build();

        given(repositoryPort.findById(id)).willReturn(Optional.of(existing));
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinitionResponseDto result = service.updateMetadataDefinition(command);

        assertThat(result.getOptions()).containsExactly("Option A", "Option B", "Option C");
    }

    // TEST 11 : GET -> retourne correctement tous les nouveaux champs
    @Test
    @DisplayName("TEST 11 : Should return metadata definition with all fields via getById")
    void test11_getMetadataDefinitionById_ReturnsAllFields() {
        UUID id = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(id)
                .name("invoice_number")
                .label("Numéro de Facture")
                .type(MetadataType.STRING)
                .required(true)
                .description("Explication du champ")
                .defaultValue("DEF-1")
                .displayOrder(10)
                .active(true)
                .createdAt(Instant.now())
                .build();

        given(repositoryPort.findById(id)).willReturn(Optional.of(existing));

        MetadataDefinitionResponseDto result = service.getMetadataDefinitionById(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDescription()).isEqualTo("Explication du champ");
        assertThat(result.getDefaultValue()).isEqualTo("DEF-1");
        assertThat(result.getDisplayOrder()).isEqualTo(10);
        assertThat(result.isActive()).isTrue();
    }

    // TEST 12 : Restore d'une définition supprimée -> active et deleted_at restent cohérents
    @Test
    @DisplayName("TEST 12 : Should restore soft-deleted metadata definition successfully")
    void test12_restoreMetadataDefinition() {
        UUID id = UUID.randomUUID();
        MetadataDefinition restored = MetadataDefinition.builder()
                .id(id)
                .name("archived_key")
                .active(true)
                .deletedAt(null)
                .build();

        given(repositoryPort.restore(id)).willReturn(restored);

        MetadataDefinitionResponseDto result = service.restoreMetadataDefinition(id);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDeletedAt()).isNull();
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw ConflictException when creating metadata definition with duplicate name")
    void createMetadataDefinition_DuplicateName_ThrowsConflict() {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_number")
                .label("Numéro de facture")
                .type(MetadataType.STRING)
                .build();

        given(repositoryPort.findByName("invoice_number")).willReturn(Optional.of(MetadataDefinition.builder().build()));

        assertThatThrownBy(() -> service.createMetadataDefinition(command))
                .isInstanceOf(ConflictException.class);
    }
}
