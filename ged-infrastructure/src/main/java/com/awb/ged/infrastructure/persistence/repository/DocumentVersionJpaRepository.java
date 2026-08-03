package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionJpaEntity, UUID> {

    List<DocumentVersionJpaEntity> findByDocumentIdOrderByVersionNumberAsc(UUID documentId);
}
