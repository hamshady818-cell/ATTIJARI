package com.awb.ged.api.metadata;

import com.awb.ged.api.metadata.dto.MetadataDefinitionRequest;
import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.port.in.metadata.*;
import com.awb.ged.common.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata-definitions")
public class MetadataDefinitionController {

    private final CreateMetadataDefinitionUseCase createUseCase;
    private final GetMetadataDefinitionUseCase getUseCase;
    private final ListMetadataDefinitionsUseCase listUseCase;
    private final ListDeletedMetadataDefinitionsUseCase listDeletedUseCase;
    private final UpdateMetadataDefinitionUseCase updateUseCase;
    private final DeleteMetadataDefinitionUseCase deleteUseCase;
    private final RestoreMetadataDefinitionUseCase restoreUseCase;

    @Autowired
    public MetadataDefinitionController(CreateMetadataDefinitionUseCase createUseCase,
                                        GetMetadataDefinitionUseCase getUseCase,
                                        ListMetadataDefinitionsUseCase listUseCase,
                                        ListDeletedMetadataDefinitionsUseCase listDeletedUseCase,
                                        UpdateMetadataDefinitionUseCase updateUseCase,
                                        DeleteMetadataDefinitionUseCase deleteUseCase,
                                        RestoreMetadataDefinitionUseCase restoreUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.listDeletedUseCase = listDeletedUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.restoreUseCase = restoreUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MetadataDefinitionResponseDto> createMetadataDefinition(
            @Valid @RequestBody MetadataDefinitionRequest request) {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name(request.getName())
                .label(request.getLabel())
                .type(request.getType())
                .required(Boolean.TRUE.equals(request.getRequired()))
                .validationPattern(request.getValidationPattern())
                .options(request.getOptions())
                .description(request.getDescription())
                .defaultValue(request.getDefaultValue())
                .displayOrder(request.getDisplayOrder())
                .active(request.getActive())
                .categoryId(request.getCategoryId())
                .build();
        MetadataDefinitionResponseDto created = createUseCase.createMetadataDefinition(command);
        URI location = URI.create("/api/v1/metadata-definitions/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<PageResponse<MetadataDefinitionResponseDto>> listDeletedMetadataDefinitions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listDeletedUseCase.listDeletedMetadataDefinitions(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<MetadataDefinitionResponseDto> getMetadataDefinitionById(@PathVariable("id") UUID id) {
        MetadataDefinitionResponseDto definition = getUseCase.getMetadataDefinitionById(id);
        return ResponseEntity.ok(definition);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('USER') or hasRole('VIEWER')")
    public ResponseEntity<PageResponse<MetadataDefinitionResponseDto>> listMetadataDefinitions(
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (categoryId != null) {
            return ResponseEntity.ok(listUseCase.listMetadataDefinitions(categoryId, page, size));
        }
        return ResponseEntity.ok(listUseCase.listMetadataDefinitions(page, size));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MetadataDefinitionResponseDto> updateMetadataDefinition(
            @PathVariable("id") UUID id,
            @Valid @RequestBody MetadataDefinitionRequest request) {
        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(id)
                .name(request.getName())
                .label(request.getLabel())
                .type(request.getType())
                .required(request.getRequired())
                .validationPattern(request.getValidationPattern())
                .options(request.getOptions())
                .description(request.getDescription())
                .defaultValue(request.getDefaultValue())
                .displayOrder(request.getDisplayOrder())
                .active(request.getActive())
                .categoryId(request.getCategoryId())
                .categoryIdExplicitlySet(request.isCategoryIdExplicitlySet())
                .build();
        MetadataDefinitionResponseDto updated = updateUseCase.updateMetadataDefinition(command);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteMetadataDefinition(@PathVariable("id") UUID id) {
        deleteUseCase.deleteMetadataDefinition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MetadataDefinitionResponseDto> restoreMetadataDefinition(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(restoreUseCase.restoreMetadataDefinition(id));
    }
}
