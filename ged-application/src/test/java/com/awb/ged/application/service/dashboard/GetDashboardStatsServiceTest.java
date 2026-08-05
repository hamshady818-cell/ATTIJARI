package com.awb.ged.application.service.dashboard;

import com.awb.ged.application.dto.dashboard.DashboardStatsDto;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.domain.document.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetDashboardStatsServiceTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private FolderRepositoryPort folderRepositoryPort;

    private GetDashboardStatsService getDashboardStatsService;

    @BeforeEach
    void setUp() {
        getDashboardStatsService = new GetDashboardStatsService(documentRepositoryPort, folderRepositoryPort);
    }

    @Test
    @DisplayName("Should return aggregated dashboard statistics from live repository calls")
    void getDashboardStats_Success() {
        // Given
        given(documentRepositoryPort.countAllDocuments()).willReturn(42L);
        given(folderRepositoryPort.countAllFolders()).willReturn(10L);
        given(documentRepositoryPort.sumAllVersionSizeBytes()).willReturn(1048576L);

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .name("RecentDoc.pdf")
                .status(Document.DocumentStatus.PUBLISHED)
                .createdAt(Instant.now())
                .build();

        given(documentRepositoryPort.findRecentUploads(10)).willReturn(List.of(doc));
        given(documentRepositoryPort.findRecentlyModified(10)).willReturn(List.of(doc));

        // When
        DashboardStatsDto stats = getDashboardStatsService.getDashboardStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalDocuments()).isEqualTo(42L);
        assertThat(stats.getTotalFolders()).isEqualTo(10L);
        assertThat(stats.getStorageUsedBytes()).isEqualTo(1048576L);
        assertThat(stats.getRecentUploads()).hasSize(1);
        assertThat(stats.getRecentUploads().get(0).getName()).isEqualTo("RecentDoc.pdf");
    }
}
