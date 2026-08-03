package com.awb.ged.api.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteRequest {

    @NotBlank(message = "Entity type is required")
    private String entityType; // "DOCUMENT" or "FOLDER"

    @NotNull(message = "Entity ID is required")
    private UUID entityId;
}
