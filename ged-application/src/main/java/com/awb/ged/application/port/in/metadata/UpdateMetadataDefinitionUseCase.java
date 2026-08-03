package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;

public interface UpdateMetadataDefinitionUseCase {
    MetadataDefinitionResponseDto updateMetadataDefinition(UpdateMetadataDefinitionCommand command);
}
