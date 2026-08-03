package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.folder.CreateFolderCommand;
import com.awb.ged.application.dto.folder.FolderResponseDto;
import com.awb.ged.domain.folder.model.Folder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FolderMapper {

    @Mapping(target = "parentId", source = "parentFolderId")
    Folder toDomain(CreateFolderCommand command);

    FolderResponseDto toResponseDto(Folder domain);
}
