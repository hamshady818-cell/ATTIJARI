package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.tag.TagResponseDto;
import com.awb.ged.domain.tag.model.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagResponseDto toResponseDto(Tag tag);
}
