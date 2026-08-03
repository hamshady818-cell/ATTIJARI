package com.awb.ged.api.metadata;

import com.awb.ged.api.metadata.dto.MetadataDefinitionRequest;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.CreateMetadataDefinitionCommand;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.port.in.metadata.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metadata-definitions")
public class MetadataDefinitionController {

    private final CreateMetadataDefinitionUseCase createUseCase;
    private final GetMetadataDefinitionUseCase getUseCase;
    private final ListMetadataDefinitionsUseCase listUseCase;
    private final UpdateMetadataDefinitionUseCase updateUseCase;
    private final DeleteMetadataDefinitionUseCase deleteUseCase;

    @Autowired
    public MetadataDefinitionController(CreateMetadataDefinitionUseCase createUseCase,
                                        GetMetadataDefinitionUseCase getUseCase,
                                        ListMetadataDefinitionsUseCase listUseCase,
                                        UpdateMetadataDefinitionUseCase updateUseCase,
                                        DeleteMetadataDefinitionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MetadataDefinitionResponseDto> createMetadataDefinition(
            @Valid @RequestBody MetadataDefinitionRequest request) {
        CreateMetadataDefinitionCommand command = CreateMetadataDefinitionCommand.builder()
                .name(request.getName())
                .label(request.getLabel())
                .type(request.getType())
                .required(request.isRequired())
                .validationPattern(request.getValidationPattern())
                .build();
        MetadataDefinitionResponseDto created = createUseCase.createMetadataDefinition(command);
        URI location = URI.create("/api/v1/metadata-definitions/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<MetadataDefinitionResponseDto> getMetadataDefinitionById(@PathVariable("id") UUID id) {
        MetadataDefinitionResponseDto definition = getUseCase.getMetadataDefinitionById(id);
        return ResponseEntity.ok(definition);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<MetadataDefinitionResponseDto>> listMetadataDefinitions() {
        List<MetadataDefinitionResponseDto> list = listUseCase.listMetadataDefinitions();
        return ResponseEntity.ok(list);
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
                .required(request.isRequired())
                .validationPattern(request.getValidationPattern())
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
}
