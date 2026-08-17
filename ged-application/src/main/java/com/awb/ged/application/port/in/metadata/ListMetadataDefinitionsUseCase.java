package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.common.model.PageResponse;

import java.util.UUID;

public interface ListMetadataDefinitionsUseCase {
    PageResponse<MetadataDefinitionResponseDto> listMetadataDefinitions(int page, int size);

    PageResponse<MetadataDefinitionResponseDto> listMetadataDefinitions(UUID categoryId, int page, int size);
}
