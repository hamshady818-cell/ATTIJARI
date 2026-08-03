package com.awb.ged.application.dto.metadata;

import com.awb.ged.domain.metadata.model.MetadataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMetadataDefinitionCommand {
    private String name;
    private String label;
    private MetadataType type;
    private boolean required;
    private String validationPattern;
}
