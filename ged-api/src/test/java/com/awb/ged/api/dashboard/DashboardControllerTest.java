package com.awb.ged.api.dashboard;

import com.awb.ged.api.exception.GlobalExceptionHandler;
import com.awb.ged.application.dto.dashboard.DashboardStatsDto;
import com.awb.ged.application.port.in.dashboard.GetDashboardStatsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDashboardStatsUseCase getDashboardStatsUseCase;

    @Test
    @WithMockUser(authorities = "DOCUMENT_READ")
    @DisplayName("GET /api/v1/dashboard/stats - Should return 200 OK with dashboard metrics")
    void getDashboardStats_Success() throws Exception {
        // Given
        DashboardStatsDto statsDto = DashboardStatsDto.builder()
                .totalDocuments(100L)
                .totalFolders(20L)
                .storageUsedBytes(5000000L)
                .recentUploads(List.of())
                .recentlyModified(List.of())
                .topCategories(List.of())
                .build();

        given(getDashboardStatsUseCase.getDashboardStats()).willReturn(statsDto);

        // When / Then
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDocuments").value(100))
                .andExpect(jsonPath("$.totalFolders").value(20))
                .andExpect(jsonPath("$.storageUsedBytes").value(5000000));
    }
}
