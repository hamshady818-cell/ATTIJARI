package com.awb.ged.api.metadata.dto;

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
public class MetadataDefinitionRequest {

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

    private boolean categoryIdExplicitlySet;

    public void setCategoryId(java.util.UUID categoryId) {
        this.categoryId = categoryId;
        this.categoryIdExplicitlySet = true;
    }
}
