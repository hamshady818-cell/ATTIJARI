package com.awb.ged.application.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated dashboard statistics — all values come from live database queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    /** Total number of non-deleted documents */
    private long totalDocuments;

    /** Total number of non-deleted folders */
    private long totalFolders;

    /** Total storage used in bytes across all document versions */
    private long storageUsedBytes;

    /** Up to 10 most recently uploaded documents */
    private List<RecentDocumentDto> recentUploads;

    /** Up to 10 most recently modified documents */
    private List<RecentDocumentDto> recentlyModified;

    /** Top categories by document count */
    private List<CategoryStatDto> topCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentDocumentDto {
        private UUID id;
        private String name;
        private String status;
        private String mimeType;
        private UUID ownerId;
        private String ownerUsername;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatDto {
        private UUID categoryId;
        private String categoryName;
        private long documentCount;
    }
}
