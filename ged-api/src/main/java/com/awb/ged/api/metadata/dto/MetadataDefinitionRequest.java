package com.awb.ged.api.metadata.dto;

import com.awb.ged.domain.metadata.model.MetadataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataDefinitionRequest {

    @NotBlank(message = "Field name key is required")
    private String name;

    @NotBlank(message = "Field display label is required")
    private String label;

    @NotNull(message = "Field type is required")
    private MetadataType type;

    private boolean required;

    private String validationPattern;
}
