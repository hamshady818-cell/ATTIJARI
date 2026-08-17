package com.awb.ged.application.dto.metadata;

import com.awb.ged.domain.metadata.model.MetadataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMetadataDefinitionCommand {
    private UUID id;
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
    private UUID categoryId;
    private boolean categoryIdExplicitlySet;
}
