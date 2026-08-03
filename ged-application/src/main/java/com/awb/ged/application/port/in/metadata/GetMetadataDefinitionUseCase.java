package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;

import java.util.UUID;

public interface GetMetadataDefinitionUseCase {
    MetadataDefinitionResponseDto getMetadataDefinitionById(UUID id);
}
