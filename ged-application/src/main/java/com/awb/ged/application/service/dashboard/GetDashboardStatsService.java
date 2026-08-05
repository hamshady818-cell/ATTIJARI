package com.awb.ged.application.service.dashboard;

import com.awb.ged.application.dto.dashboard.DashboardStatsDto;
import com.awb.ged.application.port.in.dashboard.GetDashboardStatsUseCase;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.domain.document.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetDashboardStatsService implements GetDashboardStatsUseCase {

    private final DocumentRepositoryPort documentRepositoryPort;
    private final FolderRepositoryPort folderRepositoryPort;

    @Autowired
    public GetDashboardStatsService(DocumentRepositoryPort documentRepositoryPort,
                                    FolderRepositoryPort folderRepositoryPort) {
        this.documentRepositoryPort = documentRepositoryPort;
        this.folderRepositoryPort = folderRepositoryPort;
    }

    @Override
    public DashboardStatsDto getDashboardStats() {
        long totalDocuments = documentRepositoryPort.countAllDocuments();
        long totalFolders = folderRepositoryPort.countAllFolders();
        long storageUsed = documentRepositoryPort.sumAllVersionSizeBytes();

        List<Document> recentUploads = documentRepositoryPort.findRecentUploads(10);
        List<Document> recentlyModified = documentRepositoryPort.findRecentlyModified(10);

        List<DashboardStatsDto.RecentDocumentDto> recentUploadDtos = recentUploads.stream()
                .map(this::toRecentDto)
                .toList();

        List<DashboardStatsDto.RecentDocumentDto> recentlyModifiedDtos = recentlyModified.stream()
                .map(this::toRecentDto)
                .toList();

        return DashboardStatsDto.builder()
                .totalDocuments(totalDocuments)
                .totalFolders(totalFolders)
                .storageUsedBytes(storageUsed)
                .recentUploads(recentUploadDtos)
                .recentlyModified(recentlyModifiedDtos)
                .topCategories(List.of())
                .build();
    }

    private DashboardStatsDto.RecentDocumentDto toRecentDto(Document doc) {
        return DashboardStatsDto.RecentDocumentDto.builder()
                .id(doc.getId())
                .name(doc.getName())
                .status(doc.getStatus() != null ? doc.getStatus().name() : null)
                .mimeType(doc.getMimeType())
                .ownerId(doc.getOwnerId())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
