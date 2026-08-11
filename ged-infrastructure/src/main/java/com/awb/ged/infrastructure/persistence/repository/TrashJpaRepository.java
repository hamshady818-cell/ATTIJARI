package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.trash.TrashJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrashJpaRepository extends JpaRepository<TrashJpaEntity, UUID> {

    List<TrashJpaEntity> findByDeletedByIdAndPurgedAtIsNull(UUID userId);

    List<TrashJpaEntity> findByPurgedAtIsNull();

    Page<TrashJpaEntity> findByDeletedByIdAndPurgedAtIsNull(UUID userId, Pageable pageable);

    Page<TrashJpaEntity> findByPurgedAtIsNull(Pageable pageable);
}
