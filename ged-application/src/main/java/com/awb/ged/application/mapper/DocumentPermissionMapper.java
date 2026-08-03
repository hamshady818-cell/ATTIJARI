package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.domain.document.model.DocumentPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentPermissionMapper {

    @Mapping(source = "documentId", target = "targetId")
    @Mapping(source = "canShare", target = "canShareOrManage")
    PermissionResponseDto toResponseDto(DocumentPermission permission);
}
