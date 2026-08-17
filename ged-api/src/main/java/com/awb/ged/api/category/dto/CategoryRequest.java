package com.awb.ged.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    private String name;

    private String description;

    private UUID parentId;

    private String color;

    private String icon;

    private String securityClass;

    private Boolean active;
}
