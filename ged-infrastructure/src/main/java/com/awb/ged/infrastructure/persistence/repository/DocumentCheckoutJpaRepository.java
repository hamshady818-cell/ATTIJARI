package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentCheckoutJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentCheckoutJpaRepository extends JpaRepository<DocumentCheckoutJpaEntity, UUID> {

    Optional<DocumentCheckoutJpaEntity> findByDocumentIdAndCheckedInAtIsNull(UUID documentId);
}
