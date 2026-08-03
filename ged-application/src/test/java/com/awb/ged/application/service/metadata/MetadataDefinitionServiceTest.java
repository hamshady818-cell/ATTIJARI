package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private final MetadataDefinitionMapper mapper = Mappers.getMapper(MetadataDefinitionMapper.class);

    private MetadataDefinitionService service;

    @BeforeEach
    void setUp() {
        service = new MetadataDefinitionService(repositoryPort, mapper);
    }

    @Test
    @DisplayName("Should create metadata definition successfully")
    void createMetadataDefinition_Success() {
        // Given
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_number")
                .label("Invoice Number")
                .type(MetadataType.STRING)
                .required(true)
                .build();

        given(repositoryPort.findByName("invoice_number")).willReturn(Optional.empty());
        given(repositoryPort.save(any(MetadataDefinition.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        MetadataDefinitionResponseDto result = service.createMetadataDefinition(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("invoice_number");
        assertThat(result.getLabel()).isEqualTo("Invoice Number");
        assertThat(result.getType()).isEqualTo(MetadataType.STRING);
        assertThat(result.isRequired()).isTrue();
        verify(repositoryPort).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when creating metadata definition with duplicate name")
    void createMetadataDefinition_DuplicateName_ThrowsConflict() {
        // Given
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name("invoice_number")
                .build();

        given(repositoryPort.findByName("invoice_number")).willReturn(Optional.of(MetadataDefinition.builder().build()));

        // When / Then
        assertThatThrownBy(() -> service.createMetadataDefinition(command))
                .isInstanceOf(ConflictException.class);
    }
}
