package com.awb.ged.application.port.in.metadata;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.common.model.PageResponse;

public interface ListDeletedMetadataDefinitionsUseCase {
    PageResponse<MetadataDefinitionResponseDto> listDeletedMetadataDefinitions(int page, int size);
}
