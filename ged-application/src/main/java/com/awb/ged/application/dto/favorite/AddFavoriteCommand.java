package com.awb.ged.application.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteCommand {

    private UUID userId;
    private String entityType; // "DOCUMENT" or "FOLDER"
    private UUID entityId;
}
