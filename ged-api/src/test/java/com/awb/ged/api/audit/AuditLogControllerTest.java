package com.awb.ged.api.audit;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.application.port.in.audit.GetAuditLogsUseCase;
import com.awb.ged.common.model.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import(GlobalExceptionHandler.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAuditLogsUseCase getAuditLogsUseCase;

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    @DisplayName("GET /api/v1/audit-logs - Should return 200 OK with paginated audit logs (AUDIT_READ authority)")
    void getAuditLogs_Success() throws Exception {
        // Given
        UUID logId = UUID.randomUUID();
        AuditLogResponseDto dto = AuditLogResponseDto.builder()
                .id(logId)
                .action("DOCUMENT_UPLOAD")
                .entityType("DOCUMENT")
                .entityName("Contract.pdf")
                .occurredAt(Instant.now())
                .build();

        PageResponse<AuditLogResponseDto> pageResponse = PageResponse.<AuditLogResponseDto>builder()
                .content(List.of(dto))
                .pageNumber(0)
                .pageSize(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .build();

        given(getAuditLogsUseCase.getAuditLogs(null, null, 0, 20)).willReturn(pageResponse);

        // When / Then
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].action").value("DOCUMENT_UPLOAD"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "AUDIT_READ")
    @DisplayName("GET /api/v1/audit-logs - Should filter by entityType and entityId when provided")
    void getAuditLogs_WithFilters_Success() throws Exception {
        // Given
        UUID entityId = UUID.randomUUID();
        PageResponse<AuditLogResponseDto> emptyPage = PageResponse.<AuditLogResponseDto>builder()
                .content(List.of())
                .pageNumber(0)
                .pageSize(20)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();

        given(getAuditLogsUseCase.getAuditLogs("DOCUMENT", entityId, 0, 20)).willReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", "DOCUMENT")
                        .param("entityId", entityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true));
    }
}
