package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.MetadataDefinitionJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetadataDefinitionRepositoryAdapterTest {

    @Mock
    private MetadataDefinitionJpaRepository jpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private com.awb.ged.infrastructure.persistence.repository.CategoryJpaRepository categoryJpaRepository;

    private MetadataDefinitionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MetadataDefinitionRepositoryAdapter(jpaRepository, userJpaRepository, categoryJpaRepository);
    }

    @Test
    @DisplayName("Should mark entity as not new when updating existing metadata definition")
    void save_ExistingEntity_UpdatesFields() {
        UUID id = UUID.randomUUID();
        MetadataDefinition domainToUpdate = MetadataDefinition.builder()
                .id(id)
                .name("numero_facture")
                .label("Numéro Facture V2")
                .type(MetadataType.SELECT)
                .required(true)
                .validationPattern("^[0-9]+$")
                .options(List.of("Option A", "Option B"))
                .build();

        MetadataDefinitionJpaEntity existingEntity = MetadataDefinitionJpaEntity.builder().build();
        existingEntity.setId(id);
        existingEntity.markNotNew();

        given(jpaRepository.findById(id)).willReturn(Optional.of(existingEntity));
        given(jpaRepository.save(any(MetadataDefinitionJpaEntity.class))).willAnswer(inv -> inv.getArgument(0));

        MetadataDefinition result = adapter.save(domainToUpdate);

        assertThat(result).isNotNull();
        assertThat(result.getLabel()).isEqualTo("Numéro Facture V2");
        assertThat(result.getType()).isEqualTo(MetadataType.SELECT);
        assertThat(result.isRequired()).isTrue();
        assertThat(result.getValidationPattern()).isEqualTo("^[0-9]+$");
        assertThat(result.getOptions()).containsExactly("Option A", "Option B");

        verify(jpaRepository).findById(id);
        verify(jpaRepository).save(any(MetadataDefinitionJpaEntity.class));
    }

    @Test
    @DisplayName("Should create new entity when ID is null")
    void save_NewEntity_BuildsAndSaves() {
        MetadataDefinition newDomain = MetadataDefinition.builder()
                .name("code_client")
                .label("Code Client")
                .type(MetadataType.STRING)
                .required(false)
                .build();

        given(jpaRepository.save(any(MetadataDefinitionJpaEntity.class))).willAnswer(inv -> {
            MetadataDefinitionJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        MetadataDefinition result = adapter.save(newDomain);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("code_client");
        assertThat(result.getLabel()).isEqualTo("Code Client");
        assertThat(result.getType()).isEqualTo(MetadataType.STRING);
        verify(jpaRepository).save(any(MetadataDefinitionJpaEntity.class));
    }
}
