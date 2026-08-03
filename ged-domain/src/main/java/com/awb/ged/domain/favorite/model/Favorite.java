package com.awb.ged.domain.favorite.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * <h1>Favorite</h1>
 * <p>
 * Domain model representing a bookmark/favorite.
 * </p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {

    private UUID id;
    private UUID userId;
    private String entityType; // "DOCUMENT" or "FOLDER"
    private UUID entityId;
    private Instant createdAt;
    private Instant updatedAt;
}
