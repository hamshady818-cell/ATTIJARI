package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.in.metadata.*;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.common.exception.ConflictException;
import com.awb.ged.common.exception.ErrorCode;
import com.awb.ged.common.exception.NotFoundException;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MetadataDefinitionService implements CreateMetadataDefinitionUseCase, GetMetadataDefinitionUseCase, ListMetadataDefinitionsUseCase, UpdateMetadataDefinitionUseCase, DeleteMetadataDefinitionUseCase {

    private final MetadataDefinitionRepositoryPort repositoryPort;
    private final MetadataDefinitionMapper mapper;

    @Autowired
    public MetadataDefinitionService(MetadataDefinitionRepositoryPort repositoryPort, MetadataDefinitionMapper mapper) {
        this.repositoryPort = repositoryPort;
        this.mapper = mapper;
    }

    @Override
    public MetadataDefinitionResponseDto createMetadataDefinition(CreateMetadataDefinitionCommand command) {
        // Uniqueness validation
        Optional<MetadataDefinition> existing = repositoryPort.findByName(command.getName());
        if (existing.isPresent()) {
            throw new ConflictException(
                    ErrorCode.INVALID_INPUT,
                    "A metadata definition with the name '" + command.getName() + "' already exists."
            );
        }

        MetadataDefinition definition = MetadataDefinition.builder()
                .id(UUID.randomUUID())
                .name(command.getName())
                .label(command.getLabel())
                .type(command.getType())
                .required(command.isRequired())
                .validationPattern(command.getValidationPattern())
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
    public List<MetadataDefinitionResponseDto> listMetadataDefinitions() {
        List<MetadataDefinition> list = repositoryPort.findAll();
        return list.stream()
                .map(mapper::toResponseDto)
                .toList();
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

        boolean targetRequired = command.getRequired() != null ? command.getRequired() : definition.isRequired();

        MetadataDefinition updated = definition.toBuilder()
                .name(command.getName() != null ? command.getName() : definition.getName())
                .label(command.getLabel() != null ? command.getLabel() : definition.getLabel())
                .type(command.getType() != null ? command.getType() : definition.getType())
                .required(targetRequired)
                .validationPattern(command.getValidationPattern() != null ? command.getValidationPattern() : definition.getValidationPattern())
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
        repositoryPort.delete(id);
    }
}
