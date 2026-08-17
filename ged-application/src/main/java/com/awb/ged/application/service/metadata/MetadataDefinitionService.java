package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.in.metadata.*;
import com.awb.ged.application.port.in.security.CurrentUserProvider;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.common.security.CurrentUser;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MetadataDefinitionService implements CreateMetadataDefinitionUseCase,
        GetMetadataDefinitionUseCase,
        ListMetadataDefinitionsUseCase,
        ListDeletedMetadataDefinitionsUseCase,
        UpdateMetadataDefinitionUseCase,
        DeleteMetadataDefinitionUseCase,
        RestoreMetadataDefinitionUseCase {

    private final MetadataDefinitionRepositoryPort repositoryPort;
    private final com.awb.ged.application.port.out.persistence.CategoryRepositoryPort categoryRepositoryPort;
    private final MetadataDefinitionMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    @Autowired
    public MetadataDefinitionService(MetadataDefinitionRepositoryPort repositoryPort,
                                     com.awb.ged.application.port.out.persistence.CategoryRepositoryPort categoryRepositoryPort,
                                     MetadataDefinitionMapper mapper,
                                     CurrentUserProvider currentUserProvider) {
        this.repositoryPort = repositoryPort;
        this.categoryRepositoryPort = categoryRepositoryPort;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public MetadataDefinitionResponseDto createMetadataDefinition(CreateMetadataDefinitionCommand command) {
        if (command.getName() == null || command.getName().trim().isEmpty() ||
            command.getLabel() == null || command.getLabel().trim().isEmpty() ||
            command.getType() == null) {
            throw new InvalidRequestException(
                    ErrorCode.INVALID_INPUT,
                    "Le nom technique, le libellé et le type sont obligatoires pour la création."
            );
        }

        if (command.getCategoryId() != null) {
            categoryRepositoryPort.findById(command.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.INVALID_INPUT,
                            "Category with ID " + command.getCategoryId() + " was not found."
                    ));
        }

        // Uniqueness validation
        Optional<MetadataDefinition> existing = repositoryPort.findByName(command.getName());
        if (existing.isPresent()) {
            throw new ConflictException(
                    ErrorCode.INVALID_INPUT,
                    "A metadata definition with the name '" + command.getName() + "' already exists."
            );
        }

        List<String> cleanedOptions = validateAndCleanOptions(command.getType(), command.getOptions());

        MetadataDefinition definition = MetadataDefinition.builder()
                .id(UUID.randomUUID())
                .name(command.getName().trim())
                .label(command.getLabel().trim())
                .type(command.getType())
                .required(command.getRequired() != null ? command.getRequired() : false)
                .validationPattern(command.getValidationPattern())
                .options(cleanedOptions)
                .description(command.getDescription())
                .defaultValue(command.getDefaultValue())
                .displayOrder(command.getDisplayOrder() != null ? command.getDisplayOrder() : 0)
                .active(command.getActive() != null ? command.getActive() : true)
                .categoryId(command.getCategoryId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        MetadataDefinition saved = repositoryPort.save(definition);
        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MetadataDefinitionResponseDto getMetadataDefinitionById(UUID id) {
        MetadataDefinition definition = repositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Metadata definition with ID " + id + " was not found."
                ));
        return mapper.toResponseDto(definition);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MetadataDefinitionResponseDto> listMetadataDefinitions(int page, int size) {
        return listMetadataDefinitions(null, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MetadataDefinitionResponseDto> listMetadataDefinitions(UUID categoryId, int page, int size) {
        PageResponse<MetadataDefinition> pagedResult = repositoryPort.findAllActive(page, size);
        List<MetadataDefinitionResponseDto> content = pagedResult.getContent().stream()
                .filter(def -> categoryId == null || def.getCategoryId() == null || categoryId.equals(def.getCategoryId()))
                .map(mapper::toResponseDto)
                .toList();
        return PageResponse.<MetadataDefinitionResponseDto>builder()
                .content(content)
                .pageNumber(pagedResult.getPageNumber())
                .pageSize(pagedResult.getPageSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .sortBy(pagedResult.getSortBy())
                .sortDirection(pagedResult.getSortDirection())
                .first(pagedResult.isFirst())
                .last(pagedResult.isLast())
                .empty(pagedResult.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MetadataDefinitionResponseDto> listDeletedMetadataDefinitions(int page, int size) {
        PageResponse<MetadataDefinition> pagedResult = repositoryPort.findAllDeleted(page, size);
        List<MetadataDefinitionResponseDto> content = pagedResult.getContent().stream()
                .map(mapper::toResponseDto)
                .toList();
        return PageResponse.<MetadataDefinitionResponseDto>builder()
                .content(content)
                .pageNumber(pagedResult.getPageNumber())
                .pageSize(pagedResult.getPageSize())
                .totalElements(pagedResult.getTotalElements())
                .totalPages(pagedResult.getTotalPages())
                .sortBy(pagedResult.getSortBy())
                .sortDirection(pagedResult.getSortDirection())
                .first(pagedResult.isFirst())
                .last(pagedResult.isLast())
                .empty(pagedResult.isEmpty())
                .build();
    }

    @Override
    public MetadataDefinitionResponseDto updateMetadataDefinition(UpdateMetadataDefinitionCommand command) {
        MetadataDefinition definition = repositoryPort.findById(command.getId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Metadata definition with ID " + command.getId() + " was not found."
                ));

        // Uniqueness validation on name change
        if (command.getName() != null && !definition.getName().equals(command.getName())) {
            Optional<MetadataDefinition> existing = repositoryPort.findByName(command.getName());
            if (existing.isPresent()) {
                throw new ConflictException(
                        ErrorCode.INVALID_INPUT,
                        "A metadata definition with the name '" + command.getName() + "' already exists."
                );
            }
        }

        MetadataType targetType = command.getType() != null ? command.getType() : definition.getType();
        boolean targetRequired = command.getRequired() != null ? command.getRequired() : definition.isRequired();
        boolean targetActive = command.getActive() != null ? command.getActive() : definition.isActive();
        int targetDisplayOrder = command.getDisplayOrder() != null ? command.getDisplayOrder() : (definition.getDisplayOrder() != null ? definition.getDisplayOrder() : 0);

        List<String> targetOptions;
        if (command.getOptions() != null) {
            targetOptions = validateAndCleanOptions(targetType, command.getOptions());
        } else if (command.getType() != null && command.getType() != definition.getType()) {
            // Type changed -> validate against existing or empty
            targetOptions = validateAndCleanOptions(targetType, definition.getOptions());
        } else {
            targetOptions = definition.getOptions();
        }

        UUID targetCategoryId = definition.getCategoryId();
        if (command.isCategoryIdExplicitlySet()) {
            if (command.getCategoryId() != null) {
                categoryRepositoryPort.findById(command.getCategoryId())
                        .orElseThrow(() -> new NotFoundException(
                                ErrorCode.INVALID_INPUT,
                                "Category with ID " + command.getCategoryId() + " was not found."
                        ));
                targetCategoryId = command.getCategoryId();
            } else {
                targetCategoryId = null;
            }
        }

        MetadataDefinition updated = definition.toBuilder()
                .name(command.getName() != null ? command.getName() : definition.getName())
                .label(command.getLabel() != null ? command.getLabel() : definition.getLabel())
                .type(targetType)
                .required(targetRequired)
                .validationPattern(command.getValidationPattern() != null ? command.getValidationPattern() : definition.getValidationPattern())
                .options(targetOptions)
                .description(command.getDescription() != null ? command.getDescription() : definition.getDescription())
                .defaultValue(command.getDefaultValue() != null ? command.getDefaultValue() : definition.getDefaultValue())
                .displayOrder(targetDisplayOrder)
                .active(targetActive)
                .categoryId(targetCategoryId)
                .updatedAt(Instant.now())
                .build();

        MetadataDefinition saved = repositoryPort.save(updated);
        return mapper.toResponseDto(saved);
    }

    @Override
    public void deleteMetadataDefinition(UUID id) {
        repositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.INVALID_INPUT,
                        "Metadata definition with ID " + id + " was not found."
                ));
        UUID currentUserId = currentUserProvider.getCurrentUser()
                .map(CurrentUser::getId)
                .orElse(null);
        repositoryPort.softDelete(id, currentUserId);
    }

    @Override
    public MetadataDefinitionResponseDto restoreMetadataDefinition(UUID id) {
        MetadataDefinition restored = repositoryPort.restore(id);
        return mapper.toResponseDto(restored);
    }

    private List<String> validateAndCleanOptions(MetadataType type, List<String> rawOptions) {
        if (type == MetadataType.SELECT || type == MetadataType.MULTI_SELECT) {
            if (rawOptions == null || rawOptions.isEmpty()) {
                throw new InvalidRequestException(
                        ErrorCode.INVALID_INPUT,
                        "Les types SELECT et MULTI_SELECT doivent contenir au moins une option."
                );
            }
            List<String> cleaned = rawOptions.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();
            if (cleaned.isEmpty()) {
                throw new InvalidRequestException(
                        ErrorCode.INVALID_INPUT,
                        "Les types SELECT et MULTI_SELECT doivent contenir au moins une option."
                );
            }
            return cleaned;
        }
        return rawOptions != null ? rawOptions : Collections.emptyList();
    }
}
