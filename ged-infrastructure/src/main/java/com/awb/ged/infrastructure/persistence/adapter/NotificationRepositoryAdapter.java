package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.NotificationRepositoryPort;
import com.awb.ged.domain.notification.model.Notification;
import com.awb.ged.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import com.awb.ged.infrastructure.persistence.entity.user.UserJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.NotificationJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository notificationJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Autowired
    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository,
                                         UserJpaRepository userJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = mapToEntity(notification);
        NotificationJpaEntity saved = notificationJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findById(UUID id) {
        return notificationJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findByUserId(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).stream()
                .map(this::mapToDomain)
                .toList();
    }

    private Notification mapToDomain(NotificationJpaEntity entity) {
        if (entity == null) return null;
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .channel(Notification.Channel.valueOf(entity.getChannel().name()))
                .status(entity.getStatus().name())
                .readAt(entity.getReadAt())
                .sentAt(entity.getSentAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private NotificationJpaEntity mapToEntity(Notification domain) {
        if (domain == null) return null;

        UserJpaEntity user = null;
        if (domain.getUserId() != null) {
            user = userJpaRepository.findById(domain.getUserId()).orElse(null);
        }

        NotificationJpaEntity.Channel channelEnum = domain.getChannel() != null ? NotificationJpaEntity.Channel.valueOf(domain.getChannel().name()) : NotificationJpaEntity.Channel.IN_APP;
        NotificationJpaEntity.NotificationStatus statusEnum = NotificationJpaEntity.NotificationStatus.valueOf(domain.getStatus().toUpperCase());

        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .type(domain.getType())
                .title(domain.getTitle())
                .body(domain.getBody())
                .entityType(domain.getEntityType())
                .entityId(domain.getEntityId())
                .channel(channelEnum)
                .status(statusEnum)
                .readAt(domain.getReadAt())
                .sentAt(domain.getSentAt())
                .expiresAt(domain.getExpiresAt())
                .user(user)
                .build();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }
}
