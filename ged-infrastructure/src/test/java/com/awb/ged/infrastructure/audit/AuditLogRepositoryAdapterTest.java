package com.awb.ged.infrastructure.audit;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.infrastructure.persistence.entity.audit.AuditLogJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryAdapterTest {

    @Mock
    private AuditLogJpaRepository auditLogJpaRepository;

    @Mock
    private UserJpaRepository userJpaRepository;

    private AuditLogRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new AuditLogRepositoryAdapter(auditLogJpaRepository, userJpaRepository);
    }

    @Test
    @DisplayName("record() - Should save AuditLogJpaEntity with correct action and entity details")
    void record_Success() {
        // Given
        UUID entityId = UUID.randomUUID();
        Map<String, Object> metadata = Map.of("sizeBytes", 1024L);

        // When
        repositoryAdapter.record("DOCUMENT_UPLOAD", "DOCUMENT", entityId, "Report.pdf", null, metadata);

        // Then
        ArgumentCaptor<AuditLogJpaEntity> captor = ArgumentCaptor.forClass(AuditLogJpaEntity.class);
        verify(auditLogJpaRepository).save(captor.capture());

        AuditLogJpaEntity saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("DOCUMENT_UPLOAD");
        assertThat(saved.getEntityType()).isEqualTo("DOCUMENT");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getEntityName()).isEqualTo("Report.pdf");
        assertThat(saved.getMetadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("findAuditLogs() - Should return PageResponse of AuditLogResponseDto")
    void findAuditLogs_Success() {
        // Given
        UUID entityId = UUID.randomUUID();
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .id(UUID.randomUUID())
                .action("DOCUMENT_VIEW")
                .entityType("DOCUMENT")
                .entityId(entityId)
                .entityName("Invoice.pdf")
                .occurredAt(Instant.now())
                .build();

        given(auditLogJpaRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(entity)));

        // When
        PageResponse<AuditLogResponseDto> response = repositoryAdapter.findAuditLogs("DOCUMENT", entityId, 0, 20);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getAction()).isEqualTo("DOCUMENT_VIEW");
        assertThat(response.getContent().get(0).getEntityId()).isEqualTo(entityId);
    }
}
