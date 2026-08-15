package com.awb.ged.application.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing the outcome of a bulk document operation,
 * including processed counts and details of skipped (e.g. locked) documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionResultDto {

    private int processedCount;
    private List<UUID> skippedIds;
    private List<String> skippedNames;
}
