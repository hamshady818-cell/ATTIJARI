package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.permission.PermissionResponseDto;
import com.awb.ged.domain.folder.model.FolderPermission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FolderPermissionMapper {

    @Mapping(source = "folderId", target = "targetId")
    @Mapping(source = "canManage", target = "canShareOrManage")
    PermissionResponseDto toResponseDto(FolderPermission permission);
}
