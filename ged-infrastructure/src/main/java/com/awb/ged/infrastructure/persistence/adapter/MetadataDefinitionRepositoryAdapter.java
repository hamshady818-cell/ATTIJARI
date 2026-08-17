package com.awb.ged.infrastructure.persistence.adapter;

import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import com.awb.ged.infrastructure.persistence.entity.metadata.MetadataDefinitionJpaEntity;
import com.awb.ged.infrastructure.persistence.repository.CategoryJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.MetadataDefinitionJpaRepository;
import com.awb.ged.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional
public class MetadataDefinitionRepositoryAdapter implements MetadataDefinitionRepositoryPort {

    private final MetadataDefinitionJpaRepository metadataDefinitionJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public MetadataDefinitionRepositoryAdapter(MetadataDefinitionJpaRepository metadataDefinitionJpaRepository,
                                                UserJpaRepository userJpaRepository,
                                                CategoryJpaRepository categoryJpaRepository) {
        this.metadataDefinitionJpaRepository = metadataDefinitionJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public MetadataDefinition save(MetadataDefinition definition) {
        MetadataDefinitionJpaEntity entityToSave;
        if (definition.getId() != null) {
            Optional<MetadataDefinitionJpaEntity> existingOpt = metadataDefinitionJpaRepository.findById(definition.getId());
            if (existingOpt.isPresent()) {
                entityToSave = existingOpt.get();
                updateEntityFromDomain(entityToSave, definition);
            } else {
                entityToSave = mapToEntity(definition);
            }
        } else {
            entityToSave = mapToEntity(definition);
        }

        MetadataDefinitionJpaEntity saved = metadataDefinitionJpaRepository.save(entityToSave);
        return mapToDomain(saved);
    }

    private void updateEntityFromDomain(MetadataDefinitionJpaEntity entity, MetadataDefinition domain) {
        entity.setFieldName(domain.getName());
        entity.setDisplayLabel(domain.getLabel());
        entity.setFieldType(mapToJpaFieldType(domain.getType()));
        entity.setRequired(domain.isRequired());
        entity.setValidationRegex(domain.getValidationPattern());
        entity.setAllowedValues(domain.getOptions());
        entity.setDescription(domain.getDescription());
        entity.setDefaultValue(domain.getDefaultValue());
        if (domain.getDisplayOrder() != null) {
            entity.setDisplayOrder(domain.getDisplayOrder());
        }
        entity.setActive(domain.isActive());
        if (domain.getCategoryId() != null) {
            categoryJpaRepository.findById(domain.getCategoryId()).ifPresent(entity::setCategory);
        } else {
            entity.setCategory(null);
        }
        entity.setDeletedAt(domain.getDeletedAt());
        if (domain.getDeletedBy() != null) {
            userJpaRepository.findById(domain.getDeletedBy()).ifPresent(entity::setDeletedBy);
        } else {
            entity.setDeletedBy(null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataDefinition> findById(UUID id) {
        return metadataDefinitionJpaRepository.findByIdAndDeletedAtIsNull(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MetadataDefinition> findByName(String name) {
        return metadataDefinitionJpaRepository.findByFieldNameAndDeletedAtIsNull(name).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MetadataDefinition> findAllActive(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("id")));
        Page<MetadataDefinitionJpaEntity> result = metadataDefinitionJpaRepository.findAllByDeletedAtIsNull(pageable);
        return toPageResponse(result, "displayOrder", "ASC");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MetadataDefinition> findAllDeleted(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt")));
        Page<MetadataDefinitionJpaEntity> result = metadataDefinitionJpaRepository.findAllByDeletedAtIsNotNull(pageable);
        return toPageResponse(result, "updatedAt", "DESC");
    }

    @Override
    public void softDelete(UUID id, UUID deletedByUserId) {
        MetadataDefinitionJpaEntity entity = metadataDefinitionJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Metadata definition with ID " + id + " was not found."));
        if (entity.getDeletedAt() != null) {
            return; // déjà supprimée, idempotent
        }
        entity.setDeletedAt(Instant.now());
        if (deletedByUserId != null) {
            userJpaRepository.findById(deletedByUserId).ifPresent(entity::setDeletedBy);
        }
        metadataDefinitionJpaRepository.save(entity);
    }

    @Override
    public MetadataDefinition restore(UUID id) {
        MetadataDefinitionJpaEntity entity = metadataDefinitionJpaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVALID_INPUT, "Metadata definition with ID " + id + " was not found."));
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        MetadataDefinitionJpaEntity saved = metadataDefinitionJpaRepository.save(entity);
        return mapToDomain(saved);
    }

    private PageResponse<MetadataDefinition> toPageResponse(Page<MetadataDefinitionJpaEntity> page, String sortBy, String sortDirection) {
        return PageResponse.<MetadataDefinition>builder()
                .content(page.getContent().stream().map(this::mapToDomain).toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
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
                .options(entity.getAllowedValues())
                .description(entity.getDescription())
                .defaultValue(entity.getDefaultValue())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy() != null ? entity.getDeletedBy().getId() : null)
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
                .allowedValues(domain.getOptions())
                .description(domain.getDescription())
                .defaultValue(domain.getDefaultValue())
                .displayOrder(domain.getDisplayOrder() != null ? domain.getDisplayOrder() : 0)
                .active(domain.isActive())
                .deletedAt(domain.getDeletedAt())
                .build();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        if (domain.getCategoryId() != null) {
            categoryJpaRepository.findById(domain.getCategoryId()).ifPresent(entity::setCategory);
        } else {
            entity.setCategory(null);
        }
        if (domain.getDeletedBy() != null) {
            userJpaRepository.findById(domain.getDeletedBy()).ifPresent(entity::setDeletedBy);
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
