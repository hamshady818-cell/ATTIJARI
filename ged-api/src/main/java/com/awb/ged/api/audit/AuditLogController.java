package com.awb.ged.api.audit;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.application.port.in.audit.GetAuditLogsUseCase;
import com.awb.ged.common.model.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final GetAuditLogsUseCase getAuditLogsUseCase;

    public AuditLogController(GetAuditLogsUseCase getAuditLogsUseCase) {
        this.getAuditLogsUseCase = getAuditLogsUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasAuthority('AUDIT_READ')")
    public ResponseEntity<PageResponse<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        PageResponse<AuditLogResponseDto> response = getAuditLogsUseCase.getAuditLogs(entityType, entityId, page, size);
        return ResponseEntity.ok(response);
    }
}
