package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;

public interface CreateMetadataDefinitionUseCase {
    MetadataDefinitionResponseDto createMetadataDefinition(CreateMetadataDefinitionCommand command);
}
