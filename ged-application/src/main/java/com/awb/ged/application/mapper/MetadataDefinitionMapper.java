package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import org.mapstruct.Mapper;

@Mapper
public interface MetadataDefinitionMapper {
    MetadataDefinitionResponseDto toResponseDto(MetadataDefinition definition);
}
