package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.document.DocumentVersionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionJpaEntity, UUID> {

    List<DocumentVersionJpaEntity> findByDocumentIdOrderByVersionNumberAsc(UUID documentId);

    @Query("SELECT COUNT(v) FROM DocumentVersionJpaEntity v WHERE v.document.id = :documentId")
    int countByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DocumentVersionJpaEntity v WHERE v.document.id = :documentId")
    int findMaxVersionNumberByDocumentId(@Param("documentId") UUID documentId);
}
