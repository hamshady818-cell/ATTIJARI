package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;

import java.util.List;

public interface ListMetadataDefinitionsUseCase {
    List<MetadataDefinitionResponseDto> listMetadataDefinitions();
}
