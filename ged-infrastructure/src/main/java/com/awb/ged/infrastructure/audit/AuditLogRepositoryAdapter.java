package com.awb.ged.infrastructure.audit;

import com.awb.ged.application.dto.audit.AuditLogResponseDto;
import com.awb.ged.application.port.out.audit.AuditLogPort;
import com.awb.ged.application.port.out.persistence.AuditLogRepositoryPort;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.infrastructure.persistence.entity.audit.AuditLogJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditLogRepositoryAdapter implements AuditLogPort, AuditLogRepositoryPort {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository auditLogJpaRepository,
                                     UserJpaRepository userJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public void record(String action, String entityType, UUID entityId, String entityName,
                       UUID userId, Map<String, Object> metadata) {

        UserJpaEntity userEntity = null;
        if (userId != null) {
            userEntity = userJpaRepository.findById(userId).orElse(null);
        }

        String ipAddress = extractIpAddress();
        String userAgent = extractUserAgent();

        AuditLogJpaEntity auditLog = AuditLogJpaEntity.builder()
                .isNew(true)
                .user(userEntity)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata)
                .occurredAt(Instant.now())
                .build();

        auditLogJpaRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponseDto> findAuditLogs(String entityType, UUID entityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogJpaEntity> jpaPage;

        if (entityType != null && entityId != null) {
            jpaPage = auditLogJpaRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId, pageable);
        } else {
            jpaPage = auditLogJpaRepository.findAllByOrderByOccurredAtDesc(pageable);
        }

        List<AuditLogResponseDto> content = jpaPage.getContent().stream()
                .map(this::toResponseDto)
                .toList();

        return PageResponse.<AuditLogResponseDto>builder()
                .content(content)
                .pageNumber(jpaPage.getNumber())
                .pageSize(jpaPage.getSize())
                .totalElements(jpaPage.getTotalElements())
                .totalPages(jpaPage.getTotalPages())
                .first(jpaPage.isFirst())
                .last(jpaPage.isLast())
                .empty(jpaPage.isEmpty())
                .sortBy("occurredAt")
                .sortDirection("DESC")
                .build();
    }

    @SuppressWarnings("unchecked")
    private AuditLogResponseDto toResponseDto(AuditLogJpaEntity entity) {
        Map<String, Object> metaMap = null;
        if (entity.getMetadata() instanceof Map) {
            metaMap = (Map<String, Object>) entity.getMetadata();
        }

        return AuditLogResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .action(entity.getAction())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .entityName(entity.getEntityName())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .metadata(metaMap)
                .occurredAt(entity.getOccurredAt())
                .build();
    }

    private String extractIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }

    private String extractUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        }
        return null;
    }
}
