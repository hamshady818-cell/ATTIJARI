package com.awb.ged.api.dashboard;

import com.awb.ged.application.dto.dashboard.DashboardStatsDto;
import com.awb.ged.application.port.in.dashboard.GetDashboardStatsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GetDashboardStatsUseCase getDashboardStatsUseCase;

    public DashboardController(GetDashboardStatsUseCase getDashboardStatsUseCase) {
        this.getDashboardStatsUseCase = getDashboardStatsUseCase;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('DOCUMENT_READ') or hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        DashboardStatsDto stats = getDashboardStatsUseCase.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
