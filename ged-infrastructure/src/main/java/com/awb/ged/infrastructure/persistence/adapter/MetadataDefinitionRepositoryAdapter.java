package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.MetadataDefinitionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class MetadataDefinitionRepositoryAdapter implements MetadataDefinitionRepositoryPort {

    private final MetadataDefinitionJpaRepository metadataDefinitionJpaRepository;

    public MetadataDefinitionRepositoryAdapter(MetadataDefinitionJpaRepository metadataDefinitionJpaRepository) {
        this.metadataDefinitionJpaRepository = metadataDefinitionJpaRepository;
    }

    @Override
    public MetadataDefinition save(MetadataDefinition definition) {
        MetadataDefinitionJpaEntity entity = mapToEntity(definition);
        MetadataDefinitionJpaEntity saved = metadataDefinitionJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataDefinition> findById(UUID id) {
        return metadataDefinitionJpaRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataDefinition> findByName(String name) {
        return metadataDefinitionJpaRepository.findByFieldName(name).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetadataDefinition> findAll() {
        return metadataDefinitionJpaRepository.findAll().stream().map(this::mapToDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        metadataDefinitionJpaRepository.deleteById(id);
    }

    private MetadataDefinition mapToDomain(MetadataDefinitionJpaEntity entity) {
        if (entity == null) return null;
        return MetadataDefinition.builder()
                .id(entity.getId())
                .name(entity.getFieldName())
                .label(entity.getDisplayLabel())
                .type(mapToDomainType(entity.getFieldType()))
                .required(entity.isRequired())
                .validationPattern(entity.getValidationRegex())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MetadataDefinitionJpaEntity mapToEntity(MetadataDefinition domain) {
        if (domain == null) return null;
        MetadataDefinitionJpaEntity entity = MetadataDefinitionJpaEntity.builder()
                .fieldName(domain.getName())
                .displayLabel(domain.getLabel())
                .fieldType(mapToJpaFieldType(domain.getType()))
                .required(domain.isRequired())
                .validationRegex(domain.getValidationPattern())
                .build();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        return entity;
    }

    private MetadataType mapToDomainType(MetadataDefinitionJpaEntity.FieldType fieldType) {
        if (fieldType == null) return MetadataType.STRING;
        return switch (fieldType) {
            case TEXT -> MetadataType.STRING;
            case NUMBER -> MetadataType.DECIMAL;
            case DATE -> MetadataType.DATE;
            case DATETIME -> MetadataType.DATETIME;
            case BOOLEAN -> MetadataType.BOOLEAN;
            case SELECT -> MetadataType.SELECT;
            case MULTI_SELECT -> MetadataType.MULTI_SELECT;
            case URL -> MetadataType.URL;
        };
    }

    private MetadataDefinitionJpaEntity.FieldType mapToJpaFieldType(MetadataType domainType) {
        if (domainType == null) return MetadataDefinitionJpaEntity.FieldType.TEXT;
        return switch (domainType) {
            case STRING -> MetadataDefinitionJpaEntity.FieldType.TEXT;
            case INTEGER, DECIMAL -> MetadataDefinitionJpaEntity.FieldType.NUMBER;
            case DATE -> MetadataDefinitionJpaEntity.FieldType.DATE;
            case DATETIME -> MetadataDefinitionJpaEntity.FieldType.DATETIME;
            case BOOLEAN -> MetadataDefinitionJpaEntity.FieldType.BOOLEAN;
            case SELECT -> MetadataDefinitionJpaEntity.FieldType.SELECT;
            case MULTI_SELECT -> MetadataDefinitionJpaEntity.FieldType.MULTI_SELECT;
            case URL -> MetadataDefinitionJpaEntity.FieldType.URL;
        };
    }
}
