package com.awb.ged.infrastructure.persistence.repository;

import com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MetadataDefinitionJpaRepository extends JpaRepository<MetadataDefinitionJpaEntity, UUID> {

    Optional<MetadataDefinitionJpaEntity> findByFieldName(String fieldName);
}
