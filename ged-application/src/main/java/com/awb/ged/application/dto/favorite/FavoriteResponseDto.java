package com.awb.ged.application.dto.favorite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponseDto {

    private UUID id;
    private UUID userId;
    private String entityType;
    private UUID entityId;
    private Instant createdAt;
}
