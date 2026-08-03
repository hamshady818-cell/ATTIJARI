package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentPermissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentPermissionJpaRepository extends JpaRepository<DocumentPermissionJpaEntity, UUID> {

    List<DocumentPermissionJpaEntity> findByDocumentId(UUID documentId);
}
