package com.awb.ged.application.mapper;

import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.DocumentVersionResponseDto;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.DocumentTag;
import com.awb.ged.domain.document.model.DocumentVersion;
import com.awb.ged.domain.document.model.FileReferenceId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "isLocked", expression = "java(domain.getLock() != null)")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "mapTagsToStrings")
    DocumentResponseDto toResponseDto(Document domain);

    @Mapping(target = "fileReferenceId", source = "fileReferenceId", qualifiedByName = "fileReferenceToString")
    DocumentVersionResponseDto toVersionResponseDto(DocumentVersion domain);

    @Named("mapTagsToStrings")
    default List<String> mapTagsToStrings(List<DocumentTag> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(DocumentTag::getName).toList();
    }

    @Named("fileReferenceToString")
    default String fileReferenceToString(FileReferenceId fileReferenceId) {
        return fileReferenceId != null ? fileReferenceId.getValue() : null;
    }
}
