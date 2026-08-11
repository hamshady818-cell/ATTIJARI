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

import com.awb.ged.application.dto.document.DocumentMetadataValueDto;
import com.awb.ged.domain.document.model.DocumentMetadataValue;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "isLocked", expression = "java(domain.getLock() != null)")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "mapTagsToStrings")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "mapMetadataToDtos")
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "ownerUsername", ignore = true)
    @Mapping(target = "ownerName", ignore = true)
    DocumentResponseDto toResponseDto(Document domain);

    @Mapping(target = "fileReferenceId", source = "fileReferenceId", qualifiedByName = "fileReferenceToString")
    @Mapping(target = "uploadedAt", source = "uploadedAt")
    @Mapping(target = "uploadedBy", source = "uploadedBy")
    @Mapping(target = "versionLabel", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "changeSummary", ignore = true)
    @Mapping(target = "majorVersion", ignore = true)
    @Mapping(target = "uploadedByUsername", ignore = true)
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

    @Named("statusToString")
    default String statusToString(Document.DocumentStatus status) {
        return status != null ? status.name() : Document.DocumentStatus.DRAFT.name();
    }

    @Named("mapMetadataToDtos")
    default List<DocumentMetadataValueDto> mapMetadataToDtos(List<DocumentMetadataValue> metadata) {
        if (metadata == null) return List.of();
        return metadata.stream()
                .map(m -> DocumentMetadataValueDto.builder()
                        .definitionId(m.getDefinitionId())
                        .key(m.getKey())
                        .value(m.getValue())
                        .build())
                .toList();
    }
}
