package com.awb.ged.application.service.metadata;

import com.awb.ged.application.dto.document.DocumentMetadataValueDto;
import com.awb.ged.application.dto.document.DocumentResponseDto;
import com.awb.ged.application.dto.document.UploadDocumentCommand;
import com.awb.ged.application.dto.metadata.MetadataDefinitionResponseDto;
import com.awb.ged.application.dto.metadata.UpdateMetadataDefinitionCommand;
import com.awb.ged.application.mapper.DocumentMapper;
import com.awb.ged.application.mapper.MetadataDefinitionMapper;
import com.awb.ged.application.port.out.persistence.DocumentRepositoryPort;
import com.awb.ged.application.port.out.persistence.FolderRepositoryPort;
import com.awb.ged.application.port.out.persistence.MetadataDefinitionRepositoryPort;
import com.awb.ged.application.port.out.storage.StoragePort;
import com.awb.ged.application.port.out.persistence.UserRepositoryPort;
import com.awb.ged.application.service.document.UploadDocumentService;
import com.awb.ged.common.exception.InvalidRequestException;
import com.awb.ged.common.model.PageResponse;
import com.awb.ged.domain.document.model.Document;
import com.awb.ged.domain.document.model.FileReferenceId;
import com.awb.ged.domain.metadata.model.MetadataDefinition;
import com.awb.ged.domain.metadata.model.MetadataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Bug1And2FixesTest {

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;
    @Mock
    private FolderRepositoryPort folderRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private StoragePort storagePort;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private MetadataDefinitionRepositoryPort metadataDefinitionRepositoryPort;
    @Mock
    private MetadataDefinitionMapper metadataDefinitionMapper;
    @Mock
    private com.awb.ged.application.port.out.persistence.CategoryRepositoryPort categoryRepositoryPort;

    private UploadDocumentService uploadDocumentService;
    private MetadataDefinitionService metadataDefinitionService;

    private UUID userId;
    private MetadataDefinition confidentielDef;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        uploadDocumentService = new UploadDocumentService(
                documentRepositoryPort,
                folderRepositoryPort,
                userRepositoryPort,
                storagePort,
                documentMapper,
                null,
                null,
                metadataDefinitionRepositoryPort
        );

        metadataDefinitionService = new MetadataDefinitionService(
                metadataDefinitionRepositoryPort,
                categoryRepositoryPort,
                metadataDefinitionMapper,
                null
        );

        confidentielDef = MetadataDefinition.builder()
                .id(UUID.randomUUID())
                .name("confidentiel")
                .label("Confidentiel")
                .type(MetadataType.BOOLEAN)
                .required(true)
                .active(true)
                .build();

        when(userRepositoryPort.findById(any())).thenReturn(Optional.of(com.awb.ged.domain.user.model.User.builder().id(userId).username("testuser").build()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BUG 1 TESTS (A, B, C, D)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TEST A : BOOLEAN required=true & valeur absente => Upload REFUSÉ (HTTP 400)")
    void testA_booleanRequired_missingValue_refused() {
        PageResponse<MetadataDefinition> pageResponse = PageResponse.<MetadataDefinition>builder()
                .content(List.of(confidentielDef))
                .build();
        when(metadataDefinitionRepositoryPort.findAllActive(0, 1000)).thenReturn(pageResponse);

        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Test_Doc.pdf")
                .ownerId(userId)
                .fileContent("dummy content".getBytes())
                .metadata(Collections.emptyList()) // Value for 'confidentiel' is missing!
                .build();

        assertThatThrownBy(() -> uploadDocumentService.uploadDocument(command))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("La métadonnée 'Confidentiel' est obligatoire.");

        verify(storagePort, never()).store(any(), any(), any());
    }

    @Test
    @DisplayName("TEST B : BOOLEAN required=true & valeur='false' => Upload ACCEPTÉ")
    void testB_booleanRequired_valueFalse_accepted() {
        PageResponse<MetadataDefinition> pageResponse = PageResponse.<MetadataDefinition>builder()
                .content(List.of(confidentielDef))
                .build();
        when(metadataDefinitionRepositoryPort.findAllActive(0, 1000)).thenReturn(pageResponse);
        when(storagePort.store(any(), any(), any())).thenReturn(new FileReferenceId("storage-ref-123"));
        when(documentRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toResponseDto(any())).thenReturn(DocumentResponseDto.builder().id(UUID.randomUUID()).name("Test_Doc.pdf").build());

        DocumentMetadataValueDto metadataValueDto = DocumentMetadataValueDto.builder()
                .definitionId(confidentielDef.getId())
                .key("confidentiel")
                .value("false") // "false" is a valid boolean value!
                .build();

        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Test_Doc.pdf")
                .ownerId(userId)
                .fileContent("dummy content".getBytes())
                .metadata(List.of(metadataValueDto))
                .build();

        DocumentResponseDto result = uploadDocumentService.uploadDocument(command);
        assertThat(result).isNotNull();
        verify(storagePort, times(1)).store(any(), any(), any());
    }

    @Test
    @DisplayName("TEST C : BOOLEAN required=true & valeur='true' => Upload ACCEPTÉ")
    void testC_booleanRequired_valueTrue_accepted() {
        PageResponse<MetadataDefinition> pageResponse = PageResponse.<MetadataDefinition>builder()
                .content(List.of(confidentielDef))
                .build();
        when(metadataDefinitionRepositoryPort.findAllActive(0, 1000)).thenReturn(pageResponse);
        when(storagePort.store(any(), any(), any())).thenReturn(new FileReferenceId("storage-ref-123"));
        when(documentRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toResponseDto(any())).thenReturn(DocumentResponseDto.builder().id(UUID.randomUUID()).name("Test_Doc.pdf").build());

        DocumentMetadataValueDto metadataValueDto = DocumentMetadataValueDto.builder()
                .definitionId(confidentielDef.getId())
                .key("confidentiel")
                .value("true")
                .build();

        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Test_Doc.pdf")
                .ownerId(userId)
                .fileContent("dummy content".getBytes())
                .metadata(List.of(metadataValueDto))
                .build();

        DocumentResponseDto result = uploadDocumentService.uploadDocument(command);
        assertThat(result).isNotNull();
        verify(storagePort, times(1)).store(any(), any(), any());
    }

    @Test
    @DisplayName("TEST D : BOOLEAN required=false & valeur absente => Upload ACCEPTÉ")
    void testD_booleanNotRequired_missingValue_accepted() {
        MetadataDefinition optionalDef = confidentielDef.toBuilder()
                .required(false)
                .build();

        PageResponse<MetadataDefinition> pageResponse = PageResponse.<MetadataDefinition>builder()
                .content(List.of(optionalDef))
                .build();
        when(metadataDefinitionRepositoryPort.findAllActive(0, 1000)).thenReturn(pageResponse);
        when(storagePort.store(any(), any(), any())).thenReturn(new FileReferenceId("storage-ref-123"));
        when(documentRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentMapper.toResponseDto(any())).thenReturn(DocumentResponseDto.builder().id(UUID.randomUUID()).name("Test_Doc.pdf").build());

        UploadDocumentCommand command = UploadDocumentCommand.builder()
                .name("Test_Doc.pdf")
                .ownerId(userId)
                .fileContent("dummy content".getBytes())
                .metadata(Collections.emptyList())
                .build();

        DocumentResponseDto result = uploadDocumentService.uploadDocument(command);
        assertThat(result).isNotNull();
        verify(storagePort, times(1)).store(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BUG 2 TESTS (E, F, G)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TEST E : PATCH active=false => active est mis à false")
    void testE_patchActiveFalse() {
        UUID defId = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(defId)
                .name("statut")
                .label("Statut")
                .type(MetadataType.SELECT)
                .required(true)
                .active(true)
                .description("Statut du document")
                .options(List.of("Validé", "Rejeté"))
                .build();

        when(metadataDefinitionRepositoryPort.findById(defId)).thenReturn(Optional.of(existing));
        when(metadataDefinitionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metadataDefinitionMapper.toResponseDto(any())).thenAnswer(invocation -> {
            MetadataDefinition d = invocation.getArgument(0);
            return MetadataDefinitionResponseDto.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .label(d.getLabel())
                    .type(d.getType())
                    .required(d.isRequired())
                    .active(d.isActive())
                    .description(d.getDescription())
                    .options(d.getOptions())
                    .build();
        });

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(defId)
                .active(false) // Only active is updated in PATCH
                .build();

        MetadataDefinitionResponseDto response = metadataDefinitionService.updateMetadataDefinition(command);

        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("TEST F : PATCH active=true => active est mis à true")
    void testF_patchActiveTrue() {
        UUID defId = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(defId)
                .name("statut")
                .label("Statut")
                .type(MetadataType.SELECT)
                .required(true)
                .active(false) // Currently inactive
                .build();

        when(metadataDefinitionRepositoryPort.findById(defId)).thenReturn(Optional.of(existing));
        when(metadataDefinitionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metadataDefinitionMapper.toResponseDto(any())).thenAnswer(invocation -> {
            MetadataDefinition d = invocation.getArgument(0);
            return MetadataDefinitionResponseDto.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .label(d.getLabel())
                    .type(d.getType())
                    .required(d.isRequired())
                    .active(d.isActive())
                    .build();
        });

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(defId)
                .active(true)
                .build();

        MetadataDefinitionResponseDto response = metadataDefinitionService.updateMetadataDefinition(command);

        assertThat(response).isNotNull();
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("TEST G : PATCH uniquement active => les autres propriétés (required, label, description, options) ne changent pas")
    void testG_patchOnlyActive_preservesOtherProperties() {
        UUID defId = UUID.randomUUID();
        MetadataDefinition existing = MetadataDefinition.builder()
                .id(defId)
                .name("statut")
                .label("Statut du dossier")
                .type(MetadataType.SELECT)
                .required(true)
                .active(true)
                .description("Description importante")
                .defaultValue("Validé")
                .displayOrder(5)
                .options(List.of("En cours", "Validé"))
                .build();

        when(metadataDefinitionRepositoryPort.findById(defId)).thenReturn(Optional.of(existing));
        when(metadataDefinitionRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(metadataDefinitionMapper.toResponseDto(any())).thenAnswer(invocation -> {
            MetadataDefinition d = invocation.getArgument(0);
            return MetadataDefinitionResponseDto.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .label(d.getLabel())
                    .type(d.getType())
                    .required(d.isRequired())
                    .active(d.isActive())
                    .description(d.getDescription())
                    .defaultValue(d.getDefaultValue())
                    .displayOrder(d.getDisplayOrder())
                    .options(d.getOptions())
                    .build();
        });

        UpdateMetadataDefinitionCommand command = UpdateMetadataDefinitionCommand.builder()
                .id(defId)
                .active(false)
                .build();

        MetadataDefinitionResponseDto response = metadataDefinitionService.updateMetadataDefinition(command);

        assertThat(response).isNotNull();
        assertThat(response.isActive()).isFalse();
        assertThat(response.getName()).isEqualTo("statut");
        assertThat(response.getLabel()).isEqualTo("Statut du dossier");
        assertThat(response.getType()).isEqualTo(MetadataType.SELECT);
        assertThat(response.isRequired()).isTrue();
        assertThat(response.getDescription()).isEqualTo("Description importante");
        assertThat(response.getDefaultValue()).isEqualTo("Validé");
        assertThat(response.getDisplayOrder()).isEqualTo(5);
        assertThat(response.getOptions()).containsExactly("En cours", "Validé");
    }
}
