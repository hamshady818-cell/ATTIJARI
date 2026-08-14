package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.notification.NotificationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<NotificationJpaEntity> findByUserIdAndStatusIn(
            UUID userId,
            List<NotificationJpaEntity.NotificationStatus> statuses
    );
}

