package com.awb.ged.application.dto.metadata;

import com.awb.ged.domain.metadata.model.MetadataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMetadataDefinitionCommand {
    private String name;
    private String label;
    private MetadataType type;
    private Boolean required;
    private String validationPattern;
    private List<String> options;
    private String description;
    private String defaultValue;
    private Integer displayOrder;
    private Boolean active;
    private java.util.UUID categoryId;
}
