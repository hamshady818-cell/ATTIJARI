package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.trash.TrashItemResponseDto;
import com.awb.ged.domain.trash.model.TrashItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TrashMapper {

    TrashItemResponseDto toResponseDto(TrashItem item);
}
